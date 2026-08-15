package com.ruoyi.system.controller.education;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.education.support.EduLibraryArchiveConvertSupport;

@RestController
@RequestMapping("/portal/banner")
public class PortalBannerController extends BaseController
{
    private static final String KEY_MODE = "portal.home.banner.mode";
    private static final String KEY_IMAGE = "portal.home.banner.imageUrl";
    private static final String KEY_VIDEO = "portal.home.banner.videoUrl";
    private static final String KEY_POSTER = "portal.home.banner.videoPoster";
    private static final String KEY_OVERLAY = "portal.home.banner.overlay";
    private static final String KEY_SLIDES = "portal.home.banner.slides";
    private static final String KEY_HERO_TITLE = "portal.home.banner.heroTitle";
    private static final String KEY_HERO_DESC = "portal.home.banner.heroDesc";
    private static final String KEY_HEADER_MODE = "portal.header.banner.mode";
    private static final String KEY_HEADER_IMAGE = "portal.header.banner.imageUrl";
    private static final String KEY_HEADER_OVERLAY = "portal.header.banner.overlay";
    private static final String[] LOCAL_MEDIA_MARKERS = { "/statics/", "/profile/upload/" };

    private static final Map<String, String[]> CONFIG_META = new HashMap<>();

    static
    {
        CONFIG_META.put(KEY_MODE, new String[] { "Portal home banner mode", "none, image, or video" });
        CONFIG_META.put(KEY_IMAGE, new String[] { "Portal home banner image", "Image URL" });
        CONFIG_META.put(KEY_VIDEO, new String[] { "Portal home banner video", "Video URL" });
        CONFIG_META.put(KEY_POSTER, new String[] { "Portal home banner poster", "Video poster URL" });
        CONFIG_META.put(KEY_OVERLAY, new String[] { "Portal home banner overlay", "0-1 opacity" });
        CONFIG_META.put(KEY_SLIDES, new String[] { "Portal home banner slides", "JSON carousel slides" });
        CONFIG_META.put(KEY_HERO_TITLE, new String[] { "Portal home hero title", "Title when image/video mode" });
        CONFIG_META.put(KEY_HERO_DESC, new String[] { "Portal home hero desc", "Description when image/video mode" });
        CONFIG_META.put(KEY_HEADER_MODE, new String[] { "Portal header banner mode", "none or image" });
        CONFIG_META.put(KEY_HEADER_IMAGE, new String[] { "Portal header banner image", "Top bar background image" });
        CONFIG_META.put(KEY_HEADER_OVERLAY, new String[] { "Portal header banner overlay", "0-1 dark overlay" });
    }

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private EduLibraryArchiveConvertSupport archiveConvertSupport;

    @Value("${file.path:}")
    private String localFilePath;

    @GetMapping
    public AjaxResult publicConfig()
    {
        return success(readBannerConfig());
    }

    @RequiresPermissions("education:portal:banner:query")
    @GetMapping("/admin")
    public AjaxResult adminConfig()
    {
        return success(readBannerConfig());
    }

    @RequiresPermissions("education:portal:banner:edit")
    @PutMapping("/admin")
    public AjaxResult saveAdmin(@RequestBody Map<String, Object> body)
    {
        if (body == null)
        {
            return error("empty body");
        }
        String mode = normalizeMode(stringVal(body.get("mode"), "none"));
        String imageUrl = normalizeMediaUrl(stringVal(body.get("imageUrl"), ""));
        String videoUrl = normalizeMediaUrl(stringVal(body.get("videoUrl"), ""));
        String videoPoster = normalizeMediaUrl(stringVal(body.get("videoPoster"), ""));
        String overlay = numberString(body.get("overlay"), "0.42");
        String heroTitle = stringVal(body.get("heroTitle"), "");
        String heroDesc = stringVal(body.get("heroDesc"), "");
        String slidesJson = normalizeSlidesJson(body.get("slides"));

        if ("image".equals(mode))
        {
            videoUrl = "";
            videoPoster = "";
        }
        else if ("video".equals(mode))
        {
            imageUrl = "";
        }
        else
        {
            imageUrl = "";
            videoUrl = "";
            videoPoster = "";
        }

        upsertConfigValue(KEY_MODE, mode);
        upsertConfigValue(KEY_IMAGE, imageUrl);
        upsertConfigValue(KEY_VIDEO, videoUrl);
        upsertConfigValue(KEY_POSTER, videoPoster);
        upsertConfigValue(KEY_OVERLAY, overlay);
        upsertConfigValue(KEY_HERO_TITLE, heroTitle);
        upsertConfigValue(KEY_HERO_DESC, heroDesc);
        upsertConfigValue(KEY_SLIDES, slidesJson);

        String headerMode = normalizeHeaderMode(stringVal(body.get("headerMode"), "none"));
        String headerImageUrl = normalizeMediaUrl(stringVal(body.get("headerImageUrl"), ""));
        String headerOverlay = numberString(body.get("headerOverlay"), "0.4");
        if (!"image".equals(headerMode))
        {
            headerImageUrl = "";
        }
        else if (StringUtils.isEmpty(headerImageUrl))
        {
            return error("header image url required when header mode is image");
        }
        upsertConfigValue(KEY_HEADER_MODE, headerMode);
        upsertConfigValue(KEY_HEADER_IMAGE, headerImageUrl);
        upsertConfigValue(KEY_HEADER_OVERLAY, headerOverlay);
        return success();
    }

    @GetMapping("/media")
    public ResponseEntity<byte[]> proxyMedia(@RequestParam("url") String url) throws IOException
    {
        if (StringUtils.isEmpty(url))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        byte[] localBytes = readLocalUpload(url);
        if (localBytes != null)
        {
            return ResponseEntity.ok()
                .contentType(guessMediaType(url))
                .body(localBytes);
        }

        if (!url.startsWith("http"))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        RestTemplate restTemplate = createRestTemplate();
        try
        {
            ResponseEntity<byte[]> upstream = restTemplate.getForEntity(URI.create(url), byte[].class);
            if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            HttpHeaders headers = new HttpHeaders();
            MediaType type = upstream.getHeaders().getContentType();
            if (type != null)
            {
                headers.setContentType(type);
            }
            return new ResponseEntity<>(upstream.getBody(), headers, upstream.getStatusCode());
        }
        catch (Exception ex)
        {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    /**
     * Trigger kkFileView onlinePreview server-side (Office to PDF) without browser CORS.
     */
    @GetMapping("/archive-warmup")
    public ResponseEntity<Void> archiveWarmup(@RequestParam("url") String url)
    {
        try
        {
            archiveConvertSupport.triggerWarmupUrl(url);
            return ResponseEntity.ok().build();
        }
        catch (IllegalArgumentException ex)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        catch (Exception ex)
        {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    /**
     * Load compress archive file tree for portal-native file list (no iframe postMessage).
     */
    @GetMapping("/archive-manifest")
    public AjaxResult archiveManifest(@RequestParam("previewUrl") String previewUrl)
    {
        if (StringUtils.isEmpty(previewUrl) || !archiveConvertSupport.isAllowedPreviewUrl(previewUrl))
        {
            return error("invalid preview url");
        }
        try
        {
            return success(archiveConvertSupport.buildManifestPayload(previewUrl));
        }
        catch (Exception ex)
        {
            return error("archive manifest failed");
        }
    }

    private RestTemplate createRestTemplate()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    private byte[] readLocalUpload(String url) throws IOException
    {
        if (StringUtils.isEmpty(localFilePath))
        {
            return null;
        }
        String relative = extractLocalRelativePath(url);
        if (StringUtils.isEmpty(relative))
        {
            return null;
        }
        Path file = Paths.get(localFilePath, relative).normalize();
        Path root = Paths.get(localFilePath).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file))
        {
            return null;
        }
        return Files.readAllBytes(file);
    }

    private String extractLocalRelativePath(String url)
    {
        String text = StringUtils.trim(url);
        for (String marker : LOCAL_MEDIA_MARKERS)
        {
            int idx = text.indexOf(marker);
            if (idx >= 0)
            {
                return text.substring(idx + marker.length());
            }
        }
        return null;
    }

    private MediaType guessMediaType(String url)
    {
        String lower = StringUtils.lowerCase(url);
        if (lower.endsWith(".png"))
        {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".gif"))
        {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp"))
        {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".mp4"))
        {
            return MediaType.parseMediaType("video/mp4");
        }
        if (lower.endsWith(".webm"))
        {
            return MediaType.parseMediaType("video/webm");
        }
        if (lower.endsWith(".pdf"))
        {
            return MediaType.APPLICATION_PDF;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private Map<String, Object> readBannerConfig()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("mode", configService.selectConfigByKey(KEY_MODE));
        map.put("imageUrl", configService.selectConfigByKey(KEY_IMAGE));
        map.put("videoUrl", configService.selectConfigByKey(KEY_VIDEO));
        map.put("videoPoster", configService.selectConfigByKey(KEY_POSTER));
        String overlay = configService.selectConfigByKey(KEY_OVERLAY);
        try
        {
            map.put("overlay", StringUtils.isNotEmpty(overlay) ? Double.parseDouble(overlay) : 0.42D);
        }
        catch (NumberFormatException ex)
        {
            map.put("overlay", 0.42D);
        }
        map.put("headerMode", normalizeHeaderMode(configService.selectConfigByKey(KEY_HEADER_MODE)));
        map.put("headerImageUrl", StringUtils.nvl(configService.selectConfigByKey(KEY_HEADER_IMAGE), ""));
        String headerOverlay = configService.selectConfigByKey(KEY_HEADER_OVERLAY);
        try
        {
            map.put("headerOverlay", StringUtils.isNotEmpty(headerOverlay) ? Double.parseDouble(headerOverlay) : 0.4D);
        }
        catch (NumberFormatException ex)
        {
            map.put("headerOverlay", 0.4D);
        }
        map.put("heroTitle", StringUtils.nvl(configService.selectConfigByKey(KEY_HERO_TITLE), ""));
        map.put("heroDesc", StringUtils.nvl(configService.selectConfigByKey(KEY_HERO_DESC), ""));
        map.put("slides", parseSlidesArray(configService.selectConfigByKey(KEY_SLIDES)));
        return map;
    }

    private JSONArray parseSlidesArray(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return new JSONArray();
        }
        try
        {
            JSONArray arr = JSON.parseArray(raw);
            return arr != null ? arr : new JSONArray();
        }
        catch (Exception ex)
        {
            return new JSONArray();
        }
    }

    private String normalizeSlidesJson(Object raw)
    {
        if (raw == null)
        {
            return "[]";
        }
        if (raw instanceof JSONArray)
        {
            return sanitizeSlides((JSONArray) raw).toJSONString();
        }
        if (raw instanceof java.util.List)
        {
            return sanitizeSlides(JSON.parseArray(JSON.toJSONString(raw))).toJSONString();
        }
        String text = String.valueOf(raw).trim();
        if (StringUtils.isEmpty(text))
        {
            return "[]";
        }
        try
        {
            return sanitizeSlides(JSON.parseArray(text)).toJSONString();
        }
        catch (Exception ex)
        {
            return "[]";
        }
    }

    private JSONArray sanitizeSlides(JSONArray source)
    {
        JSONArray result = new JSONArray();
        if (source == null)
        {
            return result;
        }
        int limit = Math.min(source.size(), 8);
        for (int i = 0; i < limit; i++)
        {
            Object item = source.get(i);
            if (!(item instanceof JSONObject))
            {
                continue;
            }
            JSONObject slide = (JSONObject) item;
            JSONObject clean = new JSONObject();
            clean.put("title", stringVal(slide.get("title"), ""));
            clean.put("desc", stringVal(slide.get("desc"), ""));
            clean.put("bg", stringVal(slide.get("bg"), ""));
            clean.put("imageUrl", normalizeMediaUrl(stringVal(slide.get("imageUrl"), "")));
            if (StringUtils.isEmpty(clean.getString("bg")) && StringUtils.isEmpty(clean.getString("imageUrl")))
            {
                continue;
            }
            result.add(clean);
        }
        return result;
    }

    private String normalizeHeaderMode(String mode)
    {
        if ("image".equals(mode))
        {
            return mode;
        }
        return "none";
    }

    private void upsertConfigValue(String key, String value)
    {
        String safeValue = value != null ? value : "";
        SysConfig query = new SysConfig();
        query.setConfigKey(key);
        SysConfig config = configMapper.selectConfig(query);
        if (config == null)
        {
            String[] meta = CONFIG_META.get(key);
            SysConfig created = new SysConfig();
            created.setConfigName(meta != null ? meta[0] : key);
            created.setConfigKey(key);
            created.setConfigValue(safeValue);
            created.setConfigType("Y");
            created.setRemark(meta != null ? meta[1] : "");
            configService.insertConfig(created);
            return;
        }
        configMapper.updateConfigValueById(config.getConfigId(), safeValue);
        redisService.setCacheObject(CacheConstants.SYS_CONFIG_KEY + key, safeValue);
    }

    private String normalizeMode(String mode)
    {
        if ("image".equals(mode) || "video".equals(mode) || "none".equals(mode))
        {
            return mode;
        }
        return "none";
    }

    private String normalizeMediaUrl(String url)
    {
        if (StringUtils.isEmpty(url))
        {
            return "";
        }
        String text = url.trim();
        if (text.startsWith("http://") || text.startsWith("https://"))
        {
            try
            {
                java.net.URL parsed = new java.net.URL(text);
                String path = parsed.getPath();
                if (StringUtils.isNotEmpty(path) && (path.contains("/statics/") || path.contains("/profile/upload/")))
                {
                    int staticsIdx = path.indexOf("/statics/");
                    if (staticsIdx >= 0)
                    {
                        return path.substring(staticsIdx);
                    }
                    int profileIdx = path.indexOf("/profile/upload/");
                    if (profileIdx >= 0)
                    {
                        return path.substring(profileIdx);
                    }
                }
            }
            catch (Exception ex)
            {
                // keep original url
            }
            return text;
        }
        return text.startsWith("/") ? text : "/" + text;
    }

    private String stringVal(Object raw, String fallback)
    {
        if (raw == null)
        {
            return fallback;
        }
        String text = String.valueOf(raw).trim();
        return StringUtils.isEmpty(text) ? fallback : text;
    }

    private String numberString(Object raw, String fallback)
    {
        if (raw == null)
        {
            return fallback;
        }
        if (raw instanceof Number)
        {
            return String.valueOf(raw);
        }
        return stringVal(raw, fallback);
    }
}
