package com.ruoyi.system.service.education.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public final class EduQbPaperPdfExporter
{
    private static final Logger log = LoggerFactory.getLogger(EduQbPaperPdfExporter.class);

    private EduQbPaperPdfExporter()
    {
    }

    public static void renderToFile(String html, File outputFile, String templateCode) throws Exception
    {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists())
        {
            Files.createDirectories(parent.toPath());
        }
        try (OutputStream os = new FileOutputStream(outputFile))
        {
            renderToStream(html, os, templateCode);
        }
    }

    public static void renderToFile(String html, File outputFile) throws Exception
    {
        renderToFile(html, outputFile, null);
    }

    public static void renderToStream(String html, OutputStream os, String templateCode) throws Exception
    {
        int[] pageSize = EduQbPaperLayoutHelper.pageSizeMm(templateCode);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useDefaultPageSize(pageSize[0], pageSize[1], PdfRendererBuilder.PageSizeUnits.MM);
        registerChineseFont(builder);
        builder.withHtmlContent(normalizeHtmlEntities(html), null);
        builder.toStream(os);
        builder.run();
    }

    public static void renderToStream(String html, OutputStream os) throws Exception
    {
        renderToStream(html, os, null);
    }

    private static String normalizeHtmlEntities(String html)
    {
        if (html == null)
        {
            return "";
        }
        return html.replace("&nbsp;", "&#160;")
                .replace("&ensp;", "&#8194;")
                .replace("&emsp;", "&#8195;")
                .replace("&thinsp;", "&#8201;")
                .replace("&mdash;", "&#8212;")
                .replace("&ndash;", "&#8211;")
                .replace("&hellip;", "&#8230;")
                .replace("&copy;", "&#169;")
                .replace("&reg;", "&#174;")
                .replace("&trade;", "&#8482;");
    }

    private static void registerChineseFont(PdfRendererBuilder builder)
    {
        File fontFile = resolveChineseFontFile();
        if (fontFile == null)
        {
            log.warn("Chinese font not found, PDF may not render CJK characters correctly");
            return;
        }
        try
        {
            builder.useFont(fontFile, "SimSun");
        }
        catch (Exception ex)
        {
            log.warn("Failed to register font {}: {}", fontFile.getAbsolutePath(), ex.getMessage());
        }
    }

    private static File resolveChineseFontFile()
    {
        String[] paths = {
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/simsun.ttf",
                "C:/Windows/Fonts/msyh.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
        };
        for (String path : paths)
        {
            File file = new File(path);
            if (file.exists() && file.canRead())
            {
                return file;
            }
        }
        try (InputStream in = EduQbPaperPdfExporter.class.getResourceAsStream("/fonts/simsun.ttf"))
        {
            if (in != null)
            {
                File temp = File.createTempFile("edu_qb_simsun", ".ttf");
                temp.deleteOnExit();
                Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return temp;
            }
        }
        catch (Exception ex)
        {
            log.debug("Classpath font not available: {}", ex.getMessage());
        }
        return null;
    }
}
