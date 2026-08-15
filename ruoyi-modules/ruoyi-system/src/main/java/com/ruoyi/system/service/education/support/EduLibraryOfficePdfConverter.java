package com.ruoyi.system.service.education.support;



import java.io.InputStream;

import java.net.URL;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

import java.nio.file.StandardCopyOption;

import java.util.concurrent.TimeUnit;

import java.util.stream.Stream;

import org.apache.pdfbox.pdmodel.PDDocument;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.ruoyi.common.core.utils.StringUtils;

import com.ruoyi.system.config.EduLibraryProperties;



/**

 * Convert Office documents to PDF via bundled LibreOffice (Baidu Wenku style: unified PDF reading).

 */

@Component

public class EduLibraryOfficePdfConverter

{

    private static final Logger log = LoggerFactory.getLogger(EduLibraryOfficePdfConverter.class);



    @Autowired

    private EduLibraryProperties libraryProperties;



    public boolean needsConversion(String ext)

    {

        if (ext == null)

        {

            return false;

        }

        switch (ext.trim().toLowerCase())

        {

            case "doc":

            case "docx":

            case "ppt":

            case "pptx":

            case "xls":

            case "xlsx":

                return true;

            default:

                return false;

        }

    }



    public boolean isSourceReady(String fileUrl)

    {

        if (StringUtils.isEmpty(fileUrl))

        {

            return false;

        }

        try

        {

            String relative = extractRelativePath(fileUrl);

            if (StringUtils.isEmpty(relative))

            {

                return false;

            }

            String root = libraryProperties.getPreview().getLocalFileRoot();

            if (StringUtils.isEmpty(root))

            {

                return false;

            }

            return Files.isRegularFile(Paths.get(root, relative));

        }

        catch (Exception ex)

        {

            return false;

        }

    }



    /**

     * @return public preview URL when a preview PDF already exists on disk

     */

    public String resolveExistingPreviewUrl(String fileUrl)

    {

        try

        {

            Path preview = resolvePreviewTargetPath(fileUrl);

            if (preview != null && Files.isRegularFile(preview))

            {

                return buildPublicUrl(preview);

            }

        }

        catch (Exception ex)

        {

            log.debug("Resolve existing preview failed for {}", fileUrl, ex);

        }

        return null;

    }



    /**

     * @return public preview URL of generated PDF, or null on failure

     */

    public String convertToPreviewPdf(String fileUrl)

    {

        if (StringUtils.isEmpty(fileUrl))

        {

            return null;

        }

        Path source = null;

        Path tempDir = null;

        try

        {

            source = resolveSourceFile(fileUrl);

            if (source == null || !Files.isRegularFile(source))

            {

                log.warn("Library preview source not found for {}", fileUrl);

                return null;

            }

            String soffice = resolveSofficeExecutable();

            if (soffice == null)

            {

                log.warn("LibreOffice not configured for library preview conversion");

                return null;

            }

            tempDir = Files.createTempDirectory("edu-library-pdf-");

            ProcessBuilder pb = new ProcessBuilder(

                    soffice,

                    "--headless",

                    "--nologo",

                    "--nofirststartwizard",

                    "--convert-to", "pdf",

                    "--outdir", tempDir.toAbsolutePath().toString(),

                    source.toAbsolutePath().toString());

            pb.redirectErrorStream(true);

            Process process = pb.start();

            boolean finished = process.waitFor(3, TimeUnit.MINUTES);

            if (!finished)

            {

                process.destroyForcibly();

                log.warn("LibreOffice conversion timeout for {}", fileUrl);

                return null;

            }

            if (process.exitValue() != 0)

            {

                log.warn("LibreOffice conversion failed exit={} for {}", process.exitValue(), fileUrl);

                return null;

            }

            String baseName = stripExtension(source.getFileName().toString());

            Path generated = findGeneratedPdf(tempDir, baseName);

            if (generated == null)

            {

                log.warn("LibreOffice output missing for {}", fileUrl);

                return null;

            }

            Path target = resolvePreviewTargetPath(fileUrl);

            if (target == null)

            {

                log.warn("Unable to resolve preview target path for {}", fileUrl);

                return null;

            }

            Files.createDirectories(target.getParent());

            Files.copy(generated, target, StandardCopyOption.REPLACE_EXISTING);

            return buildPublicUrl(target);

        }

        catch (Exception ex)

        {

            log.warn("Library PDF conversion failed for {}", fileUrl, ex);

            return null;

        }

        finally

        {

            if (tempDir != null)

            {

                try

                {

                    Files.walk(tempDir)

                            .sorted((a, b) -> b.compareTo(a))

                            .forEach(p -> {

                                try

                                {

                                    Files.deleteIfExists(p);

                                }

                                catch (Exception ignored)

                                {

                                }

                            });

                }

                catch (Exception ignored)

                {

                }

            }

            if (source != null && source.toString().contains("edu-library-src-"))

            {

                try

                {

                    Files.deleteIfExists(source);

                    Files.deleteIfExists(source.getParent());

                }

                catch (Exception ignored)

                {

                }

            }

        }

    }



    /**

     * Convert an on-disk Office file to the given PDF path (used for archive inner files in kkFileView cache).

     */

    public boolean convertLocalFileToPdf(Path sourceFile, Path targetPdf)

    {

        if (sourceFile == null || targetPdf == null || !Files.isRegularFile(sourceFile))

        {

            return false;

        }

        if (Files.isRegularFile(targetPdf) && isPdfFile(targetPdf))

        {

            return true;

        }

        String soffice = resolveSofficeExecutable();

        if (soffice == null)

        {

            log.warn("LibreOffice not configured for archive inner conversion");

            return false;

        }

        Path tempDir = null;

        try

        {

            tempDir = Files.createTempDirectory("edu-library-pdf-");

            ProcessBuilder pb = new ProcessBuilder(

                    soffice,

                    "--headless",

                    "--nologo",

                    "--nofirststartwizard",

                    "--convert-to", "pdf",

                    "--outdir", tempDir.toAbsolutePath().toString(),

                    sourceFile.toAbsolutePath().toString());

            pb.redirectErrorStream(true);

            Process process = pb.start();

            boolean finished = process.waitFor(3, TimeUnit.MINUTES);

            if (!finished)

            {

                process.destroyForcibly();

                log.warn("LibreOffice archive inner conversion timeout for {}", sourceFile);

                return false;

            }

            if (process.exitValue() != 0)

            {

                log.warn("LibreOffice archive inner conversion failed exit={} for {}", process.exitValue(), sourceFile);

                return false;

            }

            String baseName = stripExtension(sourceFile.getFileName().toString());

            Path generated = findGeneratedPdf(tempDir, baseName);

            if (generated == null)

            {

                log.warn("LibreOffice archive inner output missing for {}", sourceFile);

                return false;

            }

            Files.createDirectories(targetPdf.getParent());

            Files.copy(generated, targetPdf, StandardCopyOption.REPLACE_EXISTING);

            return Files.isRegularFile(targetPdf) && isPdfFile(targetPdf);

        }

        catch (Exception ex)

        {

            log.warn("Archive inner PDF conversion failed for {}", sourceFile, ex);

            return false;

        }

        finally

        {

            if (tempDir != null)

            {

                try

                {

                    Files.walk(tempDir)

                            .sorted((a, b) -> b.compareTo(a))

                            .forEach(p -> {

                                try

                                {

                                    Files.deleteIfExists(p);

                                }

                                catch (Exception ignored)

                                {

                                }

                            });

                }

                catch (Exception ignored)

                {

                }

            }

        }

    }



    public Path resolveLocalPath(String fileUrl)

    {

        try

        {

            return resolveSourceFile(fileUrl);

        }

        catch (Exception ex)

        {

            log.warn("Resolve local path failed for {}", fileUrl, ex);

            return null;

        }

    }



    public String toPublicUrl(Path localFile)

    {

        return buildPublicUrl(localFile);

    }



    /**

     * Count pages in a PDF referenced by public or relative URL.

     */

    public Integer countPdfPages(String fileUrl)

    {

        if (StringUtils.isEmpty(fileUrl))

        {

            return null;

        }

        Path pdfPath = null;

        Path tempDir = null;

        try

        {

            pdfPath = resolveLocalPath(fileUrl);

            if (pdfPath == null || !Files.isRegularFile(pdfPath))

            {

                tempDir = Files.createTempDirectory("edu-library-pdf-count-");

                Path tempFile = tempDir.resolve("source.pdf");

                try (InputStream in = new URL(toFetchableUrl(fileUrl)).openStream())

                {

                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);

                }

                pdfPath = tempFile;

            }

            try (PDDocument document = PDDocument.load(pdfPath.toFile()))

            {

                int pages = document.getNumberOfPages();

                return pages > 0 ? pages : null;

            }

        }

        catch (Exception ex)

        {

            log.debug("Count PDF pages failed for {}", fileUrl, ex);

            return null;

        }

        finally

        {

            if (tempDir != null)

            {

                try

                {

                    Files.walk(tempDir)

                            .sorted((a, b) -> b.compareTo(a))

                            .forEach(p -> {

                                try

                                {

                                    Files.deleteIfExists(p);

                                }

                                catch (Exception ignored)

                                {

                                }

                            });

                }

                catch (Exception ignored)

                {

                }

            }

        }

    }



    /**

     * Rewrite file URLs to the configured public gateway base (e.g. http://host:8080/statics/...).

     */

    public String normalizePublicFileUrl(String fileUrl)

    {

        if (StringUtils.isEmpty(fileUrl))

        {

            return fileUrl;

        }

        String relative = extractRelativePath(fileUrl);

        String prefix = libraryProperties.getPreview().getLocalFilePrefix();

        if (StringUtils.isEmpty(prefix))

        {

            prefix = "/statics";

        }

        String base = libraryProperties.getPreview().getFilePublicBaseUrl();

        if (StringUtils.isEmpty(base))

        {

            base = "http://127.0.0.1:8080";

        }

        base = base.replaceAll("/$", "");

        if (StringUtils.isNotEmpty(relative))

        {

            return base + prefix + "/" + relative.replace('\\', '/');

        }

        if (fileUrl.startsWith("/"))

        {

            return base + (fileUrl.startsWith(prefix) ? fileUrl : prefix + fileUrl);

        }

        return fileUrl;

    }



    private Path resolveSourceFile(String fileUrl) throws Exception

    {

        String relative = extractRelativePath(fileUrl);

        if (StringUtils.isNotEmpty(relative))

        {

            String root = libraryProperties.getPreview().getLocalFileRoot();

            if (StringUtils.isNotEmpty(root))

            {

                Path local = Paths.get(root, relative);

                if (Files.isRegularFile(local))

                {

                    return local;

                }

            }

        }

        return downloadToTemp(fileUrl);

    }



    private Path resolvePreviewTargetPath(String fileUrl)

    {

        String relative = extractRelativePath(fileUrl);

        if (StringUtils.isEmpty(relative))

        {

            return null;

        }

        String root = libraryProperties.getPreview().getLocalFileRoot();

        if (StringUtils.isEmpty(root))

        {

            return null;

        }

        Path original = Paths.get(root, relative);

        String baseName = stripExtension(original.getFileName().toString());

        Path parent = original.getParent();

        if (parent == null)

        {

            return null;

        }

        return parent.resolve(baseName + ".preview.pdf");

    }



    private Path findGeneratedPdf(Path tempDir, String preferredBaseName) throws Exception

    {

        Path preferred = tempDir.resolve(preferredBaseName + ".pdf");

        if (Files.isRegularFile(preferred))

        {

            return preferred;

        }

        try (Stream<Path> stream = Files.list(tempDir))

        {

            return stream.filter(path -> Files.isRegularFile(path)

                    && path.getFileName().toString().toLowerCase().endsWith(".pdf"))

                    .findFirst()

                    .orElse(null);

        }

    }



    private Path downloadToTemp(String fileUrl) throws Exception

    {

        Path tempDir = Files.createTempDirectory("edu-library-src-");

        String name = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        if (name.contains("?"))

        {

            name = name.substring(0, name.indexOf('?'));

        }

        Path target = tempDir.resolve(name);

        try (InputStream in = new URL(toFetchableUrl(fileUrl)).openStream())

        {

            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

        }

        return target;

    }



    private String toFetchableUrl(String fileUrl)

    {

        return normalizePublicFileUrl(fileUrl);

    }



    private String buildPublicUrl(Path pdfFile)

    {

        String root = libraryProperties.getPreview().getLocalFileRoot();

        String prefix = libraryProperties.getPreview().getLocalFilePrefix();

        String base = libraryProperties.getPreview().getFilePublicBaseUrl();

        if (StringUtils.isEmpty(root) || StringUtils.isEmpty(prefix) || StringUtils.isEmpty(base))

        {

            return null;

        }

        try

        {

            Path normalizedRoot = Paths.get(root).toAbsolutePath().normalize();

            Path normalizedFile = pdfFile.toAbsolutePath().normalize();

            if (!normalizedFile.startsWith(normalizedRoot))

            {

                log.warn("Preview file is outside local root: {}", normalizedFile);

                return null;

            }

            String relative = normalizedRoot.relativize(normalizedFile).toString().replace('\\', '/');

            return base.replaceAll("/$", "") + prefix + "/" + relative;

        }

        catch (Exception ex)

        {

            log.warn("Build preview public URL failed for {}", pdfFile, ex);

            return null;

        }

    }



    private String extractRelativePath(String fileUrl)

    {

        String prefix = libraryProperties.getPreview().getLocalFilePrefix();

        if (StringUtils.isEmpty(prefix))

        {

            prefix = "/statics";

        }

        int idx = fileUrl.indexOf(prefix);

        if (idx >= 0)

        {

            String rel = fileUrl.substring(idx + prefix.length());

            if (rel.startsWith("/"))

            {

                rel = rel.substring(1);

            }

            return rel;

        }

        return null;

    }



    private String resolveSofficeExecutable()

    {

        String home = libraryProperties.getPreview().getLibreOfficeHome();

        if (StringUtils.isNotEmpty(home))

        {

            Path win = Paths.get(home, "program", "soffice.exe");

            if (Files.isRegularFile(win))

            {

                return win.toAbsolutePath().toString();

            }

            Path linux = Paths.get(home, "program", "soffice");

            if (Files.isRegularFile(linux))

            {

                return linux.toAbsolutePath().toString();

            }

        }

        Path bundled = Paths.get("tools", "kkfileview", "dist", "kkFileView-4.2.1", "libreoffice", "program", "soffice.exe");

        if (Files.isRegularFile(bundled))

        {

            return bundled.toAbsolutePath().toString();

        }

        return null;

    }



    private static String stripExtension(String name)

    {

        int dot = name.lastIndexOf('.');

        return dot > 0 ? name.substring(0, dot) : name;

    }



    private static boolean isPdfFile(Path file)

    {

        try

        {

            byte[] head = Files.readAllBytes(file);

            return head != null && head.length >= 4

                && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';

        }

        catch (Exception ex)

        {

            return false;

        }

    }

}


