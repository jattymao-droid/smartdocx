package com.ruoyi.system.service.education.support;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.file.ImageUtils;

/**
 * Render limited question HTML (tables, images, blanks) into DOCX paragraphs.
 */
public final class EduQbHtmlDocxRenderer
{
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([a-zA-Z0-9]+)(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SRC_PATTERN = Pattern.compile("src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern MIN_WIDTH_PATTERN = Pattern.compile("min-width\\s*:\\s*([0-9.]+)em", Pattern.CASE_INSENSITIVE);

    private EduQbHtmlDocxRenderer()
    {
    }

    public static void appendHtmlStem(XWPFDocument document, XWPFParagraph firstParagraph, String html, int fontSize)
            throws Exception
    {
        if (StringUtils.isEmpty(html))
        {
            return;
        }
        HtmlWriter writer = new HtmlWriter(document, firstParagraph, fontSize, true);
        writer.write(html);
    }

    public static void appendHtmlInline(XWPFParagraph paragraph, String html, int fontSize) throws Exception
    {
        if (StringUtils.isEmpty(html))
        {
            return;
        }
        HtmlWriter writer = new HtmlWriter(paragraph.getDocument(), paragraph, fontSize, false);
        writer.write(html);
    }

    public static String resolveHtmlForExport(String html)
    {
        if (StringUtils.isEmpty(html))
        {
            return "";
        }
        Matcher matcher = SRC_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
        {
            String src = matcher.group(1);
            String resolved = resolveImageSrcForExport(src);
            matcher.appendReplacement(sb, Matcher.quoteReplacement("src=\"" + resolved + "\""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String resolveImageSrcForExport(String src)
    {
        if (StringUtils.isEmpty(src))
        {
            return "";
        }
        String trimmed = src.trim();
        if (trimmed.startsWith("data:") || trimmed.startsWith("http://") || trimmed.startsWith("https://"))
        {
            return trimmed;
        }
        byte[] bytes = ImageUtils.readFile(trimmed);
        if (bytes == null || bytes.length == 0)
        {
            return trimmed;
        }
        String mime = guessImageMime(trimmed);
        return "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
    }

    private static String guessImageMime(String path)
    {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png"))
        {
            return "image/png";
        }
        if (lower.endsWith(".gif"))
        {
            return "image/gif";
        }
        if (lower.endsWith(".webp"))
        {
            return "image/webp";
        }
        if (lower.endsWith(".bmp"))
        {
            return "image/bmp";
        }
        return "image/jpeg";
    }

    private static final class HtmlWriter
    {
        private final XWPFDocument document;
        private XWPFParagraph paragraph;
        private final int fontSize;
        private final boolean blockMode;
        private boolean firstParagraph;
        private boolean bold;
        private boolean italic;
        private boolean subscript;
        private boolean superscript;
        private boolean underline;

        private HtmlWriter(XWPFDocument document, XWPFParagraph paragraph, int fontSize, boolean blockMode)
        {
            this.document = document;
            this.paragraph = paragraph;
            this.fontSize = fontSize;
            this.blockMode = blockMode;
            this.firstParagraph = true;
        }

        private void write(String html) throws Exception
        {
            int index = 0;
            while (index < html.length())
            {
                if (html.charAt(index) == '<')
                {
                    int close = html.indexOf('>', index);
                    if (close < 0)
                    {
                        appendText(html.substring(index));
                        break;
                    }
                    String tagChunk = html.substring(index, close + 1);
                    if (tagChunk.toLowerCase().startsWith("<table"))
                    {
                        int end = findClosingTag(html, close + 1, "table");
                        renderTable(html.substring(index, end));
                        index = end;
                        continue;
                    }
                    handleTag(tagChunk);
                    index = close + 1;
                }
                else
                {
                    int next = html.indexOf('<', index);
                    if (next < 0)
                    {
                        appendText(html.substring(index));
                        break;
                    }
                    appendText(html.substring(index, next));
                    index = next;
                }
            }
        }

        private void handleTag(String tagChunk) throws Exception
        {
            Matcher matcher = TAG_PATTERN.matcher(tagChunk);
            if (!matcher.matches())
            {
                return;
            }
            boolean closing = "/".equals(matcher.group(1));
            String tag = matcher.group(2).toLowerCase();
            String attrs = matcher.group(3) != null ? matcher.group(3) : "";

            if ("br".equals(tag))
            {
                appendBreak();
                return;
            }
            if ("img".equals(tag))
            {
                appendImage(extractAttr(attrs, SRC_PATTERN));
                return;
            }
            if ("p".equals(tag) || "div".equals(tag))
            {
                if (closing && blockMode)
                {
                    startNewParagraph();
                }
                return;
            }
            if ("span".equals(tag) || "bk".equals(tag))
            {
                if (!closing && isBlankSpan(tag, attrs))
                {
                    appendBlank(attrs);
                }
                return;
            }
            if ("b".equals(tag) || "strong".equals(tag))
            {
                bold = !closing;
                return;
            }
            if ("i".equals(tag) || "em".equals(tag))
            {
                italic = !closing;
                return;
            }
            if ("sub".equals(tag))
            {
                subscript = !closing;
                return;
            }
            if ("sup".equals(tag))
            {
                superscript = !closing;
                return;
            }
            if ("u".equals(tag))
            {
                underline = !closing;
            }
        }

        private void renderTable(String tableHtml) throws Exception
        {
            List<List<String>> rows = parseTableRows(tableHtml);
            if (rows.isEmpty())
            {
                return;
            }
            if (blockMode && !firstParagraph)
            {
                startNewParagraph();
            }
            int cols = 0;
            for (List<String> row : rows)
            {
                cols = Math.max(cols, row.size());
            }
            XWPFTable table = document.createTable(rows.size(), cols);
            for (int r = 0; r < rows.size(); r++)
            {
                XWPFTableRow row = table.getRow(r);
                List<String> cells = rows.get(r);
                for (int c = 0; c < cols; c++)
                {
                    XWPFTableCell cell = row.getCell(c);
                    XWPFParagraph cellPara = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
                    for (int runIndex = cellPara.getRuns().size() - 1; runIndex >= 0; runIndex--)
                    {
                        cellPara.removeRun(runIndex);
                    }
                    String cellHtml = c < cells.size() ? cells.get(c) : "";
                    HtmlWriter cellWriter = new HtmlWriter(document, cellPara, fontSize, false);
                    cellWriter.write(cellHtml);
                }
            }
            paragraph = document.createParagraph();
            firstParagraph = false;
        }

        private List<List<String>> parseTableRows(String tableHtml)
        {
            List<List<String>> rows = new ArrayList<>();
            Matcher rowMatcher = Pattern.compile("<tr\\b[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                    .matcher(tableHtml);
            while (rowMatcher.find())
            {
                String rowHtml = rowMatcher.group(1);
                List<String> cells = new ArrayList<>();
                Matcher cellMatcher = Pattern.compile("<t[dh]\\b[^>]*>(.*?)</t[dh]>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                        .matcher(rowHtml);
                while (cellMatcher.find())
                {
                    cells.add(stripOuterTags(cellMatcher.group(1)));
                }
                if (!cells.isEmpty())
                {
                    rows.add(cells);
                }
            }
            return rows;
        }

        private String stripOuterTags(String html)
        {
            String text = html;
            text = text.replaceAll("(?is)</?(tbody|thead|tr|td|th|p|div)[^>]*>", "");
            return text.trim();
        }

        private int findClosingTag(String html, int start, String tag)
        {
            Pattern open = Pattern.compile("<" + tag + "\\b", Pattern.CASE_INSENSITIVE);
            Pattern close = Pattern.compile("</" + tag + ">", Pattern.CASE_INSENSITIVE);
            int depth = 1;
            int index = start;
            while (index < html.length() && depth > 0)
            {
                Matcher nextOpen = open.matcher(html);
                Matcher nextClose = close.matcher(html);
                int openAt = nextOpen.find(index) ? nextOpen.start() : -1;
                int closeAt = nextClose.find(index) ? nextClose.start() : -1;
                if (closeAt < 0)
                {
                    return html.length();
                }
                if (openAt >= 0 && openAt < closeAt)
                {
                    depth++;
                    index = nextOpen.end();
                }
                else
                {
                    depth--;
                    index = nextClose.end();
                    if (depth == 0)
                    {
                        return index;
                    }
                }
            }
            return html.length();
        }

        private void appendText(String raw)
        {
            String text = decodeEntities(raw).replace('\u00a0', ' ');
            if (StringUtils.isEmpty(text))
            {
                return;
            }
            XWPFRun run = paragraph.createRun();
            applyRunStyle(run);
            run.setText(text);
        }

        private void appendBreak()
        {
            XWPFRun run = paragraph.createRun();
            applyRunStyle(run);
            run.addBreak();
        }

        private void appendBlank(String attrs)
        {
            int width = 6;
            Matcher widthMatcher = MIN_WIDTH_PATTERN.matcher(attrs);
            if (widthMatcher.find())
            {
                try
                {
                    width = Math.max(3, Math.round(Float.parseFloat(widthMatcher.group(1))));
                }
                catch (NumberFormatException ignored)
                {
                    width = 6;
                }
            }
            XWPFRun run = paragraph.createRun();
            applyRunStyle(run);
            run.setUnderline(UnderlinePatterns.SINGLE);
            StringBuilder blanks = new StringBuilder();
            for (int i = 0; i < width; i++)
            {
                blanks.append(' ');
            }
            run.setText(blanks.toString());
        }

        private void appendImage(String src) throws Exception
        {
            if (StringUtils.isEmpty(src))
            {
                return;
            }
            byte[] bytes = ImageUtils.readFile(src.trim());
            if (bytes == null || bytes.length == 0)
            {
                return;
            }
            XWPFRun run = paragraph.createRun();
            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes))
            {
                run.addPicture(in, pictureType(src), "figure", Units.toEMU(280), Units.toEMU(200));
            }
        }

        private void startNewParagraph()
        {
            if (firstParagraph)
            {
                firstParagraph = false;
                return;
            }
            paragraph = document.createParagraph();
        }

        private void applyRunStyle(XWPFRun run)
        {
            run.setFontFamily("SimSun");
            int size = fontSize;
            if (subscript || superscript)
            {
                size = Math.max(8, fontSize - 2);
            }
            run.setFontSize(size);
            run.setBold(bold);
            run.setItalic(italic);
            if (underline)
            {
                run.setUnderline(UnderlinePatterns.SINGLE);
            }
            if (subscript)
            {
                run.setSubscript(org.apache.poi.xwpf.usermodel.VerticalAlign.SUBSCRIPT);
            }
            if (superscript)
            {
                run.setSubscript(org.apache.poi.xwpf.usermodel.VerticalAlign.SUPERSCRIPT);
            }
        }

        private static boolean isBlankSpan(String tag, String attrs)
        {
            if ("bk".equals(tag))
            {
                return true;
            }
            Matcher classMatcher = CLASS_PATTERN.matcher(attrs);
            return classMatcher.find() && classMatcher.group(1).contains("qb-blank");
        }

        private static String extractAttr(String attrs, Pattern pattern)
        {
            Matcher matcher = pattern.matcher(attrs);
            return matcher.find() ? matcher.group(1) : "";
        }

        private static String decodeEntities(String text)
        {
            if (text == null)
            {
                return "";
            }
            return text.replace("&nbsp;", " ")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"");
        }

        private static int pictureType(String path)
        {
            String lower = path.toLowerCase();
            if (lower.endsWith(".png"))
            {
                return Document.PICTURE_TYPE_PNG;
            }
            if (lower.endsWith(".gif"))
            {
                return Document.PICTURE_TYPE_GIF;
            }
            if (lower.endsWith(".bmp"))
            {
                return Document.PICTURE_TYPE_BMP;
            }
            return Document.PICTURE_TYPE_JPEG;
        }
    }
}
