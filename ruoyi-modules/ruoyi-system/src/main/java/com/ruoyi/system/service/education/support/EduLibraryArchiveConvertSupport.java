package com.ruoyi.system.service.education.support;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.config.EduLibraryProperties;

/**
 * Pre-convert Office files inside uploaded archives via kkFileView (upload-time warmup).
 */
@Component
public class EduLibraryArchiveConvertSupport
{
    private static final Logger log = LoggerFactory.getLogger(EduLibraryArchiveConvertSupport.class);

    private static final int MAX_INNER_OFFICE_FILES = 30;
    private static final int PDF_PROBE_ATTEMPTS = 20;
    private static final long PDF_PROBE_DELAY_MS = 800L;

    @Autowired
    private EduLibraryPreviewSupport previewSupport;

    @Autowired
    private EduLibraryOfficePdfConverter officePdfConverter;

    @Autowired
    private EduLibraryProperties libraryProperties;

    public static final class ArchiveManifest
    {
        public String fileKey;
        public String fileTree;
        public String kkBase;
        public String previewUrl;
        public Object nodes;
    }

    public static final class ArchiveInnerFile
    {
        public String id;
        public String name;
        public String ext;
    }

    public static final class ConvertResult
    {
        public int totalOffice;
        public int converted;
        public int failed;

        public boolean hasPartialFailure()
        {
            return failed > 0 && converted > 0;
        }

        public boolean isFullySuccessful()
        {
            return totalOffice == 0 || failed == 0;
        }
    }

    public ConvertResult warmupArchiveInnerFiles(String fileUrl)
    {
        ConvertResult result = new ConvertResult();
        String previewUrl = previewSupport.buildKkfileviewUrl(fileUrl);
        if (StringUtils.isEmpty(previewUrl))
        {
            result.failed = 1;
            return result;
        }
        try
        {
            ArchiveManifest manifest = loadManifest(previewUrl);
            List<ArchiveInnerFile> officeFiles = filterOfficeFiles(flattenNodes(manifest.nodes));
            result.totalOffice = officeFiles.size();
            if (officeFiles.isEmpty())
            {
                return result;
            }
            int processed = 0;
            for (ArchiveInnerFile file : officeFiles)
            {
                if (processed >= MAX_INNER_OFFICE_FILES)
                {
                    break;
                }
                processed++;
                if (warmupInnerFile(manifest, file))
                {
                    result.converted++;
                }
                else
                {
                    result.failed++;
                }
            }
        }
        catch (Exception ex)
        {
            log.warn("Archive inner warmup failed for fileUrl={}", fileUrl, ex);
            result.failed = Math.max(result.failed, 1);
        }
        return result;
    }

    public Map<String, Object> buildManifestPayload(String previewUrl) throws Exception
    {
        ArchiveManifest manifest = loadManifest(previewUrl);
        Map<String, Object> data = new HashMap<>();
        data.put("fileKey", manifest.fileKey);
        data.put("fileTree", manifest.fileTree);
        data.put("kkBase", manifest.kkBase);
        data.put("archiveUrl", manifest.previewUrl);
        data.put("nodes", normalizeArchiveNodes(manifest.nodes));
        return data;
    }

    public void triggerWarmupUrl(String warmupUrl)
    {
        if (StringUtils.isEmpty(warmupUrl) || !isAllowedWarmupUrl(warmupUrl))
        {
            throw new IllegalArgumentException("invalid warmup url");
        }
        String innerPath = extractInnerPathFromWarmupUrl(warmupUrl);
        if (StringUtils.isEmpty(innerPath))
        {
            throw new IllegalArgumentException("invalid archive inner warmup url");
        }
        String ext = guessExt(innerPath);
        if (!officePdfConverter.needsConversion(ext))
        {
            return;
        }
        if (!convertInnerFileToPdf(innerPath, ext))
        {
            throw new IllegalStateException("archive inner pdf conversion failed");
        }
    }

    public boolean isAllowedPreviewUrl(String previewUrl)
    {
        if (StringUtils.isEmpty(previewUrl))
        {
            return false;
        }
        String lower = previewUrl.toLowerCase();
        return (lower.startsWith("http://127.0.0.1") || lower.startsWith("http://localhost"))
            && lower.contains("/onlinepreview");
    }

    private boolean isAllowedWarmupUrl(String url)
    {
        if (StringUtils.isEmpty(url) || !url.startsWith("http") || !url.contains("/onlinePreview"))
        {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.startsWith("http://127.0.0.1") || lower.startsWith("http://localhost");
    }

    private boolean warmupInnerFile(ArchiveManifest manifest, ArchiveInnerFile file)
    {
        try
        {
            return convertInnerFileToPdf(file.id, file.ext);
        }
        catch (Exception ex)
        {
            log.warn("Archive inner file warmup failed: {}", file.id, ex);
            return false;
        }
    }

    private boolean convertInnerFileToPdf(String innerPath, String ext)
    {
        Path source = resolveKkfileviewCachePath(innerPath);
        if (source == null || !Files.isRegularFile(source))
        {
            log.warn("Archive inner source missing: {}", innerPath);
            return false;
        }
        String key = StringUtils.isEmpty(ext) ? guessExt(innerPath) : ext.trim().toLowerCase();
        String pdfRelative = buildArchivePdfServePath(innerPath, key);
        Path target = resolveKkfileviewCachePath(pdfRelative);
        if (target == null)
        {
            return false;
        }
        return officePdfConverter.convertLocalFileToPdf(source, target);
    }

    private Path resolveKkfileviewCachePath(String relativePath)
    {
        String root = resolveKkfileviewFileRoot();
        if (StringUtils.isEmpty(root) || StringUtils.isEmpty(relativePath))
        {
            return null;
        }
        String normalized = StringUtils.trim(relativePath).replace('\\', '/').replaceAll("^/+", "");
        return Paths.get(root, normalized.split("/")).normalize();
    }

    private String resolveKkfileviewFileRoot()
    {
        String configured = libraryProperties.getPreview().getKkfileviewFileRoot();
        if (StringUtils.isNotEmpty(configured))
        {
            return configured.trim();
        }
        Path bundled = Paths.get("tools", "kkfileview", "dist", "kkFileView-4.2.1", "file");
        if (Files.isDirectory(bundled))
        {
            return bundled.toAbsolutePath().toString();
        }
        return "";
    }

    private String extractInnerPathFromWarmupUrl(String warmupUrl)
    {
        String encoded = extractQueryParam(warmupUrl, "url");
        if (StringUtils.isEmpty(encoded))
        {
            return "";
        }
        try
        {
            String decodedParam = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
            byte[] raw = Base64.getDecoder().decode(decodedParam);
            String innerUrl = new String(raw, StandardCharsets.UTF_8);
            int schemeEnd = innerUrl.indexOf("://");
            if (schemeEnd < 0)
            {
                return "";
            }
            int pathStart = innerUrl.indexOf('/', schemeEnd + 3);
            if (pathStart < 0)
            {
                return "";
            }
            String pathPart = innerUrl.substring(pathStart + 1);
            int queryIdx = pathPart.indexOf('?');
            if (queryIdx >= 0)
            {
                pathPart = pathPart.substring(0, queryIdx);
            }
            return pathPart.replace('\\', '/');
        }
        catch (Exception ex)
        {
            log.warn("Failed to parse archive warmup url: {}", warmupUrl, ex);
            return "";
        }
    }

    private String extractQueryParam(String url, String key)
    {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(key))
        {
            return "";
        }
        int queryStart = url.indexOf('?');
        if (queryStart < 0)
        {
            return "";
        }
        String query = url.substring(queryStart + 1);
        for (String pair : query.split("&"))
        {
            int eq = pair.indexOf('=');
            if (eq <= 0)
            {
                continue;
            }
            if (key.equals(pair.substring(0, eq)))
            {
                return pair.substring(eq + 1);
            }
        }
        return "";
    }

    private boolean waitForPdf(String kkBase, String innerPath, String ext)
    {
        String pdfUrl = buildPdfServeUrl(kkBase, innerPath, ext);
        RestTemplate restTemplate = createRestTemplate(10000);
        for (int attempt = 0; attempt < PDF_PROBE_ATTEMPTS; attempt++)
        {
            try
            {
                byte[] body = restTemplate.getForObject(URI.create(pdfUrl), byte[].class);
                if (isPdf(body))
                {
                    return true;
                }
            }
            catch (Exception ignored)
            {
                // keep polling
            }
            try
            {
                Thread.sleep(PDF_PROBE_DELAY_MS);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private ArchiveManifest loadManifest(String previewUrl) throws Exception
    {
        if (!isAllowedPreviewUrl(previewUrl))
        {
            throw new IllegalArgumentException("invalid preview url");
        }
        RestTemplate restTemplate = createRestTemplate(120000);
        String html = restTemplate.getForObject(URI.create(previewUrl), String.class);
        String fileKey = extractArchiveDataAttr(html, "file-key");
        String fileTree = extractArchiveDataAttr(html, "file-tree");
        String kkBase = extractArchiveDataAttr(html, "kk-base");
        if (StringUtils.isEmpty(fileTree) || StringUtils.isEmpty(kkBase))
        {
            fileTree = extractArchiveScriptValue(html, "Base64.encode\\('([^']+)'\\)");
            kkBase = extractArchiveScriptValue(html, "kkBase:\\s*'([^']+)'");
            fileKey = extractArchiveScriptValue(html, "fileKey:\\s*'([^']+)'");
        }
        if (StringUtils.isEmpty(fileTree) || StringUtils.isEmpty(kkBase))
        {
            throw new IllegalStateException("archive meta not found");
        }
        String base = kkBase.endsWith("/") ? kkBase : kkBase + "/";
        String treeToken = Base64.getEncoder().encodeToString(fileTree.getBytes(StandardCharsets.UTF_8));
        String dirUrl = base + "directory?urls=" + URLEncoder.encode(treeToken, StandardCharsets.UTF_8.name());
        Object nodes = restTemplate.getForObject(URI.create(dirUrl), Object.class);

        ArchiveManifest manifest = new ArchiveManifest();
        manifest.fileKey = fileKey;
        manifest.fileTree = fileTree;
        manifest.kkBase = base;
        manifest.previewUrl = previewUrl;
        manifest.nodes = nodes;
        return manifest;
    }

    private List<ArchiveInnerFile> filterOfficeFiles(List<ArchiveInnerFile> files)
    {
        List<ArchiveInnerFile> officeFiles = new ArrayList<>();
        if (files == null)
        {
            return officeFiles;
        }
        for (ArchiveInnerFile file : files)
        {
            if (file == null || StringUtils.isEmpty(file.id))
            {
                continue;
            }
            String ext = StringUtils.isEmpty(file.ext) ? guessExt(file.name) : file.ext.trim().toLowerCase();
            if (officePdfConverter.needsConversion(ext))
            {
                file.ext = ext;
                officeFiles.add(file);
            }
        }
        return officeFiles;
    }

    private List<ArchiveInnerFile> flattenNodes(Object nodes)
    {
        List<ArchiveInnerFile> bucket = new ArrayList<>();
        flattenNodesInternal(normalizeArchiveNodes(nodes), bucket);
        return bucket;
    }

    private void flattenNodesInternal(JSONArray nodes, List<ArchiveInnerFile> bucket)
    {
        if (nodes == null)
        {
            return;
        }
        for (int i = 0; i < nodes.size(); i++)
        {
            JSONObject node = nodes.getJSONObject(i);
            if (node == null)
            {
                continue;
            }
            boolean folder = node.getBooleanValue("folder");
            JSONArray children = node.getJSONArray("children");
            if (folder && children != null && !children.isEmpty())
            {
                flattenNodesInternal(children, bucket);
                continue;
            }
            if (!folder)
            {
                ArchiveInnerFile file = new ArchiveInnerFile();
                file.id = node.getString("id");
                file.name = node.getString("name");
                file.ext = node.getString("ext");
                bucket.add(file);
            }
        }
    }

    private JSONArray normalizeArchiveNodes(Object nodes)
    {
        JSONArray array = new JSONArray();
        if (nodes == null)
        {
            return array;
        }
        if (nodes instanceof JSONArray)
        {
            return (JSONArray) nodes;
        }
        if (nodes instanceof List)
        {
            array.addAll((List<?>) nodes);
            return array;
        }
        array.add(nodes);
        return array;
    }

    private String buildWarmupUrl(String kkBase, String innerPath, String fileKey)
    {
        String base = StringUtils.trim(kkBase).replaceAll("/$", "");
        String rawPath = StringUtils.trim(innerPath).replace('\\', '/').replaceAll("^/+", "");
        String innerUrl = base + "/" + rawPath + "?fileKey=" + URLEncoder.encode(StringUtils.defaultString(fileKey), StandardCharsets.UTF_8);
        String encodedInner = URLEncoder.encode(
                Base64.getEncoder().encodeToString(innerUrl.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        return base + "/onlinePreview?url=" + encodedInner + "&officePreviewType=pdf";
    }

    private String buildPdfServeUrl(String kkBase, String innerPath, String ext)
    {
        String base = StringUtils.trim(kkBase).replaceAll("/$", "");
        String servePath = buildArchivePdfServePath(innerPath, ext);
        return base + "/" + encodePathSegments(servePath);
    }

    static String buildArchivePdfServePath(String innerPath, String ext)
    {
        String path = StringUtils.trim(innerPath).replace('\\', '/');
        String key = StringUtils.isEmpty(ext) ? "" : ext.trim().toLowerCase();
        if ("pdf".equals(key))
        {
            return path;
        }
        int dot = path.lastIndexOf('.');
        if (dot <= 0)
        {
            return path;
        }
        return path.substring(0, dot) + ".pdf";
    }

    private String encodePathSegments(String innerPath)
    {
        String normalized = StringUtils.trim(innerPath).replace('\\', '/').replaceAll("^/+", "");
        if (StringUtils.isEmpty(normalized))
        {
            return "";
        }
        String[] segments = normalized.split("/");
        StringBuilder builder = new StringBuilder();
        for (String segment : segments)
        {
            if (StringUtils.isEmpty(segment))
            {
                continue;
            }
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append(URLEncoder.encode(segment, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private boolean isPdf(byte[] body)
    {
        return body != null && body.length >= 4
            && body[0] == '%' && body[1] == 'P' && body[2] == 'D' && body[3] == 'F';
    }

    private String guessExt(String name)
    {
        if (StringUtils.isEmpty(name))
        {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot >= name.length() - 1)
        {
            return "";
        }
        return name.substring(dot + 1).trim().toLowerCase();
    }

    private RestTemplate createRestTemplate(int readTimeoutMs)
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    private String extractArchiveDataAttr(String html, String attr)
    {
        if (StringUtils.isEmpty(html))
        {
            return "";
        }
        Pattern pattern = Pattern.compile("data-" + attr + "=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find())
        {
            return "";
        }
        return HtmlUtils.htmlUnescape(matcher.group(1));
    }

    private String extractArchiveScriptValue(String html, String patternText)
    {
        if (StringUtils.isEmpty(html))
        {
            return "";
        }
        Pattern pattern = Pattern.compile(patternText);
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find())
        {
            return "";
        }
        return HtmlUtils.htmlUnescape(matcher.group(1));
    }
}
