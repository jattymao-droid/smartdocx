package com.ruoyi.system.service.education.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import com.ruoyi.system.service.education.support.EduQbLocalFileSupport;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.service.education.support.EduQbFileUploadUtils;
import com.ruoyi.common.core.utils.uuid.Seq;
import com.ruoyi.system.domain.education.EduQbDocxParseResult;
import com.ruoyi.system.domain.education.EduQbImportBlock;

public final class EduQbDocxParseService
{
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private static final String IMAGE_ONLY_LABEL = "\u63d2\u56fe";

    private EduQbDocxParseService()
    {
    }

    public static List<EduQbImportBlock> parseFile(File file)
    {
        return parseFileWithPreview(file).getBlocks();
    }

    public static EduQbDocxParseResult parseFileWithPreview(File file)
    {
        if (file == null || !file.exists())
        {
            throw new ServiceException("DOCX \u6587\u4ef6\u4e0d\u5b58\u5728");
        }
        if (file.length() > MAX_FILE_SIZE)
        {
            throw new ServiceException("DOCX \u6587\u4ef6\u4e0d\u80fd\u8d85\u8fc7 20MB");
        }
        String name = file.getName().toLowerCase();
        if (!name.endsWith(".docx"))
        {
            throw new ServiceException("\u4ec5\u652f\u6301 .docx \u683c\u5f0f");
        }
        try (InputStream in = new FileInputStream(file); XWPFDocument document = new XWPFDocument(in))
        {
            ImageCache imageCache = new ImageCache();
            List<EduQbImportBlock> blocks = parseDocument(document, imageCache);
            EduQbDocxParseResult result = new EduQbDocxParseResult();
            result.setBlocks(blocks);
            result.setPreviewHtml(buildPreviewHtml(blocks));
            return result;
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("DOCX \u89e3\u6790\u5931\u8d25\uff1a" + ex.getMessage());
        }
    }

    public static String buildPreviewHtml(List<EduQbImportBlock> blocks)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"qb-docx-preview\">");
        for (EduQbImportBlock block : blocks)
        {
            sb.append("<div class=\"qb-docx-block\" data-block-id=\"")
                    .append(block.getBlockId())
                    .append("\"><span class=\"qb-block-no\">")
                    .append(block.getOrderNum())
                    .append("</span><div class=\"qb-block-body\">");
            if (StringUtils.isNotEmpty(block.getText()))
            {
                sb.append("<div class=\"qb-block-text\">")
                        .append(escapeHtml(block.getText()))
                        .append("</div>");
            }
            appendImageHtml(sb, block.getImageUrls());
            sb.append("</div></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static void appendImageHtml(StringBuilder sb, List<String> imageUrls)
    {
        if (imageUrls == null || imageUrls.isEmpty())
        {
            return;
        }
        sb.append("<div class=\"qb-block-images\">");
        for (String url : imageUrls)
        {
            if (StringUtils.isEmpty(url))
            {
                continue;
            }
            sb.append("<img class=\"qb-block-image\" src=\"")
                    .append(escapeHtml(url))
                    .append("\" alt=\"figure\" />");
        }
        sb.append("</div>");
    }

    private static String escapeHtml(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static List<EduQbImportBlock> parseDocument(XWPFDocument document, ImageCache imageCache) throws IOException
    {
        List<EduQbImportBlock> blocks = new ArrayList<>();
        int blockId = 0;
        for (IBodyElement element : document.getBodyElements())
        {
            if (element instanceof XWPFParagraph)
            {
                ParagraphContent content = extractParagraphContent((XWPFParagraph) element, document, imageCache);
                if (content.hasContent())
                {
                    blocks.add(buildBlock(blockId++, content, imageCache));
                }
            }
            else if (element instanceof XWPFTable)
            {
                blockId = appendTableParagraphs((XWPFTable) element, document, imageCache, blocks, blockId);
            }
        }
        List<String> orphanUrls = EduQbDocxImageExtractor.appendOrphanPictures(document, imageCache);
        for (String url : orphanUrls)
        {
            ParagraphContent orphan = new ParagraphContent("", List.of(url));
            blocks.add(buildBlock(blockId++, orphan, imageCache));
        }
        if (blocks.isEmpty())
        {
            throw new ServiceException("DOCX \u672a\u89e3\u6790\u5230\u6709\u6548\u6bb5\u843d");
        }
        return blocks;
    }

    private static int appendTableParagraphs(XWPFTable table, XWPFDocument document, ImageCache imageCache,
            List<EduQbImportBlock> blocks, int blockId) throws IOException
    {
        for (XWPFTableRow row : table.getRows())
        {
            for (XWPFTableCell cell : row.getTableCells())
            {
                for (XWPFParagraph paragraph : cell.getParagraphs())
                {
                    ParagraphContent content = extractParagraphContent(paragraph, document, imageCache);
                    if (content.hasContent())
                    {
                        blocks.add(buildBlock(blockId++, content, imageCache));
                    }
                }
            }
        }
        return blockId;
    }

    private static ParagraphContent extractParagraphContent(XWPFParagraph paragraph, XWPFDocument document,
            ImageCache imageCache) throws IOException
    {
        String text = normalizeText(paragraph.getText());
        List<String> imageUrls = EduQbDocxImageExtractor.extractParagraphImages(paragraph, document, imageCache);
        return new ParagraphContent(text, imageUrls);
    }

    private static EduQbImportBlock buildBlock(int blockId, ParagraphContent content, ImageCache imageCache)
    {
        for (String url : content.imageUrls)
        {
            imageCache.markReferencedByUrl(url);
        }
        EduQbImportBlock block = new EduQbImportBlock();
        block.setBlockId(blockId);
        block.setOrderNum(blockId + 1);
        String text = content.text;
        if (StringUtils.isEmpty(text) && !content.imageUrls.isEmpty())
        {
            text = IMAGE_ONLY_LABEL;
        }
        block.setText(text);
        block.setImageUrls(content.imageUrls);
        if (EduQbImportHeadingSupport.isChapterHeading(text))
        {
            block.setBlockKind("heading");
        }
        else
        {
            block.setBlockKind("content");
        }
        return block;
    }

    private static String normalizeText(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static final class ParagraphContent
    {
        private final String text;
        private final List<String> imageUrls;

        private ParagraphContent(String text, List<String> imageUrls)
        {
            this.text = text;
            this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        }

        private boolean hasContent()
        {
            return StringUtils.isNotEmpty(text) || !imageUrls.isEmpty();
        }
    }

    static final class ImageCache
    {
        private final Map<String, String> urlByPart = new HashMap<>();
        private final Set<String> referencedParts = new HashSet<>();

        String savePicture(XWPFPictureData picData) throws IOException
        {
            if (picData == null)
            {
                return null;
            }
            String partKey = picData.getPackagePart().getPartName().getName();
            if (urlByPart.containsKey(partKey))
            {
                return urlByPart.get(partKey);
            }
            String ext = picData.suggestFileExtension();
            if (StringUtils.isEmpty(ext))
            {
                ext = "png";
            }
            String pathName = DateUtils.datePath() + "/" + Seq.getId(Seq.uploadSeqType) + "A001." + ext;
            File desc = EduQbFileUploadUtils.getAbsoluteFile(EduQbLocalFileSupport.getUploadPath(), pathName);
            Files.write(desc.toPath(), picData.getData());
            String url = EduQbFileUploadUtils.getPathFileName(EduQbLocalFileSupport.getUploadPath(), pathName);
            urlByPart.put(partKey, url);
            return url;
        }

        boolean isReferenced(String partKey)
        {
            return referencedParts.contains(partKey);
        }

        void markReferenced(String partKey)
        {
            if (StringUtils.isNotEmpty(partKey))
            {
                referencedParts.add(partKey);
            }
        }

        void markReferencedByUrl(String url)
        {
            if (StringUtils.isEmpty(url))
            {
                return;
            }
            for (Map.Entry<String, String> entry : urlByPart.entrySet())
            {
                if (url.equals(entry.getValue()))
                {
                    referencedParts.add(entry.getKey());
                }
            }
        }
    }
}
