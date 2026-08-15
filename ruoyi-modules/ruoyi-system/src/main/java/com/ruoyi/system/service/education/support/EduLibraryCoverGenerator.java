package com.ruoyi.system.service.education.support;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.utils.StringUtils;

@Component
public class EduLibraryCoverGenerator
{
    private static final Logger log = LoggerFactory.getLogger(EduLibraryCoverGenerator.class);
    private static final int MAX_COVER_WIDTH = 720;
    private static final int RENDER_DPI = 120;

    @Autowired
    private EduLibraryOfficePdfConverter officePdfConverter;

    /**
     * @return public URL of generated cover image, or null when unsupported / failed
     */
    public String generateCover(String fileUrl, String fileExt)
    {
        if (StringUtils.isEmpty(fileUrl))
        {
            return null;
        }
        String ext = fileExt == null ? "" : fileExt.trim().toLowerCase();
        if ("txt".equals(ext) || EduLibraryPreviewSupport.isArchiveExt(ext))
        {
            return null;
        }
        try
        {
            Path source = officePdfConverter.resolveLocalPath(fileUrl);
            if (source == null || !Files.isRegularFile(source))
            {
                log.warn("Cover source not found for {}", fileUrl);
                return null;
            }
            Path pdfPath = source;
            if (officePdfConverter.needsConversion(ext))
            {
                String previewPdfUrl = officePdfConverter.convertToPreviewPdf(fileUrl);
                if (StringUtils.isEmpty(previewPdfUrl))
                {
                    return null;
                }
                pdfPath = officePdfConverter.resolveLocalPath(previewPdfUrl);
                if (pdfPath == null || !Files.isRegularFile(pdfPath))
                {
                    return null;
                }
            }
            else if (!"pdf".equals(ext))
            {
                return null;
            }
            Path coverPath = source.getParent().resolve(stripExtension(source.getFileName().toString()) + ".cover.jpg");
            if (!renderFirstPage(pdfPath, coverPath))
            {
                return null;
            }
            return officePdfConverter.toPublicUrl(coverPath);
        }
        catch (Exception ex)
        {
            log.warn("Library cover generation failed for {}", fileUrl, ex);
            return null;
        }
    }

    private boolean renderFirstPage(Path pdfPath, Path coverPath) throws Exception
    {
        try (PDDocument document = PDDocument.load(pdfPath.toFile()))
        {
            if (document.getNumberOfPages() <= 0)
            {
                return false;
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, RENDER_DPI, ImageType.RGB);
            BufferedImage scaled = scaleToMaxWidth(image, MAX_COVER_WIDTH);
            ImageIO.write(scaled, "jpg", coverPath.toFile());
            return Files.isRegularFile(coverPath);
        }
    }

    private BufferedImage scaleToMaxWidth(BufferedImage source, int maxWidth)
    {
        if (source == null || source.getWidth() <= maxWidth)
        {
            return source;
        }
        int targetW = maxWidth;
        int targetH = Math.max(1, Math.round(source.getHeight() * (targetW / (float) source.getWidth())));
        BufferedImage target = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, targetW, targetH, null);
        g.dispose();
        return target;
    }

    private static String stripExtension(String name)
    {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
