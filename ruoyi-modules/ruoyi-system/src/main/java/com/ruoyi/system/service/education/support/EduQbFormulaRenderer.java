package com.ruoyi.system.service.education.support;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import com.ruoyi.common.core.utils.StringUtils;

/**
 * Render question LaTeX / mixed Chinese+math for paper export (PDF HTML &amp; DOCX).
 */
public final class EduQbFormulaRenderer
{
    private static final float FONT_SIZE = 12f;
    private static final Pattern DELIMITER_PATTERN = Pattern.compile("\\$\\$[\\s\\S]+?\\$\\$|\\$[^$\\n]+?\\$");
    private static final Pattern LATEX_CMD_PATTERN = Pattern.compile(
            "\\\\(?:frac|sqrt|sin|cos|tan|cot|log|ln|theta|alpha|beta|gamma|Delta|Phi|mathrm|mathbf|text|ce|left|right|cdot|times|div|ge|le|ne|infty|sum|int|vec|overrightarrow|pm|mp|not|begin|end|operatorname)"
                    + "(?:\\*?(?:\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\})*(?:_\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}|\\^\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}|_[A-Za-z0-9]|\\^[A-Za-z0-9])*)*");
    private static final Pattern SUBSCRIPT_PATTERN = Pattern.compile(
            "[A-Za-z](?:_\\{[^{}]+\\}|_\\w|\\^\\{[^{}]+\\}|\\^\\w)+(?:\\s*[=+\\-]\\s*[A-Za-z0-9\\\\^_{}+\\-*/().\\[\\]|\\\\,\\s]+)?");

    private EduQbFormulaRenderer()
    {
    }

    public static String renderToHtml(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return "";
        }
        if (EduQbQuestionContentSupport.isHtmlContent(text))
        {
            return EduQbHtmlDocxRenderer.resolveHtmlForExport(text);
        }
        String normalized = normalizeOcrLatex(text);
        if (shouldRenderWholeLineAsLatex(normalized))
        {
            String whole = tryRenderLatexHtml(normalized);
            if (whole != null)
            {
                return whole;
            }
        }
        List<Part> parts = splitParts(normalized);
        if (parts.isEmpty())
        {
            return escapeHtml(normalized);
        }
        StringBuilder sb = new StringBuilder();
        for (Part part : parts)
        {
            if (part.math)
            {
                String html = tryRenderLatexHtml(part.content);
                sb.append(html != null ? html : escapeHtml(part.content));
            }
            else
            {
                sb.append(escapeHtml(part.content));
            }
        }
        return sb.toString();
    }

    public static void appendRichText(XWPFParagraph paragraph, String text, int fontSize) throws Exception
    {
        if (StringUtils.isEmpty(text))
        {
            return;
        }
        if (EduQbQuestionContentSupport.isHtmlContent(text))
        {
            EduQbHtmlDocxRenderer.appendHtmlInline(paragraph, text, fontSize);
            return;
        }
        String normalized = normalizeOcrLatex(text);
        if (shouldRenderWholeLineAsLatex(normalized))
        {
            byte[] png = tryRenderLatexPng(normalized);
            if (png != null)
            {
                appendInlineImage(paragraph, png, fontSize);
                return;
            }
        }
        List<Part> parts = splitParts(normalized);
        if (parts.isEmpty())
        {
            appendPlainRun(paragraph, normalized, fontSize, false);
            return;
        }
        for (Part part : parts)
        {
            if (part.math)
            {
                byte[] png = tryRenderLatexPng(part.content);
                if (png != null)
                {
                    appendInlineImage(paragraph, png, fontSize);
                }
                else
                {
                    appendPlainRun(paragraph, part.content, fontSize, false);
                }
            }
            else
            {
                appendPlainRun(paragraph, part.content, fontSize, false);
            }
        }
    }

    public static String normalizeOcrLatex(String text)
    {
        if (text == null || text.isEmpty())
        {
            return "";
        }
        String s = text;
        s = s.replace("\\!", "");
        s = s.replace("\\m_", "m_");
        s = s.replace("\uFF04", "$");
        s = s.replace("= =", "=");
        s = s.replaceAll("(_\\{[^{}]+\\}|_[A-Za-z0-9])\\s+([a-zA-Z])", "$1$2");
        s = s.replaceAll("\\s{2,}", " ");
        return s.trim();
    }

    private static boolean shouldRenderWholeLineAsLatex(String text)
    {
        if (!text.contains("\\") && !text.contains("_") && !text.contains("^") && !text.contains("$"))
        {
            return false;
        }
        if (containsChinese(text) && text.contains("$"))
        {
            return false;
        }
        if (containsChinese(text))
        {
            return text.contains("\\text{") || text.contains("\\mathrm{");
        }
        return text.contains("\\") || SUBSCRIPT_PATTERN.matcher(text).find();
    }

    private static List<Part> splitParts(String input)
    {
        List<Part> parts = new ArrayList<>();
        if (containsChinese(input) && DELIMITER_PATTERN.matcher(input).find())
        {
            appendDelimitedParts(parts, input);
            return parts;
        }

        int index = 0;
        while (index < input.length())
        {
            if (input.startsWith("\\text{", index))
            {
                int openBrace = index + 5;
                int close = openBrace < input.length() && input.charAt(openBrace) == '{' ? findClosingBrace(input, openBrace) : -1;
                if (close > openBrace)
                {
                    parts.add(Part.text(input.substring(openBrace + 1, close)));
                    index = close + 1;
                    continue;
                }
            }

            Matcher delim = DELIMITER_PATTERN.matcher(input);
            delim.region(index, input.length());
            if (delim.lookingAt())
            {
                String raw = delim.group();
                String latex = raw.startsWith("$$") ? raw.substring(2, raw.length() - 2).trim()
                        : raw.substring(1, raw.length() - 1).trim();
                parts.add(Part.math(latex));
                index = delim.end();
                continue;
            }

            Matcher cmd = LATEX_CMD_PATTERN.matcher(input);
            cmd.region(index, input.length());
            if (cmd.lookingAt())
            {
                parts.add(Part.math(cmd.group()));
                index = cmd.end();
                continue;
            }

            Matcher sub = SUBSCRIPT_PATTERN.matcher(input);
            sub.region(index, input.length());
            if (sub.lookingAt() && !containsChinese(sub.group()))
            {
                parts.add(Part.math(sub.group()));
                index = sub.end();
                continue;
            }

            int nextSpecial = findNextSpecial(input, index);
            if (nextSpecial > index)
            {
                parts.add(Part.text(input.substring(index, nextSpecial)));
                index = nextSpecial;
            }
            else
            {
                parts.add(Part.text(String.valueOf(input.charAt(index))));
                index++;
            }
        }
        return mergeAdjacentText(parts);
    }

    private static void appendDelimitedParts(List<Part> parts, String input)
    {
        Matcher matcher = DELIMITER_PATTERN.matcher(input);
        int last = 0;
        while (matcher.find())
        {
            if (matcher.start() > last)
            {
                parts.add(Part.text(input.substring(last, matcher.start())));
            }
            String raw = matcher.group();
            String latex = raw.startsWith("$$") ? raw.substring(2, raw.length() - 2).trim()
                    : raw.substring(1, raw.length() - 1).trim();
            parts.add(Part.math(latex));
            last = matcher.end();
        }
        if (last < input.length())
        {
            parts.add(Part.text(input.substring(last)));
        }
    }

    private static int findNextSpecial(String input, int start)
    {
        int next = input.length();
        if (input.indexOf("\\text{", start) >= 0)
        {
            next = Math.min(next, input.indexOf("\\text{", start));
        }
        Matcher delim = DELIMITER_PATTERN.matcher(input);
        delim.region(start, input.length());
        if (delim.find())
        {
            next = Math.min(next, delim.start());
        }
        Matcher cmd = LATEX_CMD_PATTERN.matcher(input);
        cmd.region(start, input.length());
        if (cmd.find())
        {
            next = Math.min(next, cmd.start());
        }
        Matcher sub = SUBSCRIPT_PATTERN.matcher(input);
        sub.region(start, input.length());
        if (sub.find())
        {
            next = Math.min(next, sub.start());
        }
        return next;
    }

    private static List<Part> mergeAdjacentText(List<Part> parts)
    {
        if (parts.size() <= 1)
        {
            return parts;
        }
        List<Part> merged = new ArrayList<>();
        StringBuilder textBuf = new StringBuilder();
        for (Part part : parts)
        {
            if (part.math)
            {
                if (textBuf.length() > 0)
                {
                    merged.add(Part.text(textBuf.toString()));
                    textBuf.setLength(0);
                }
                merged.add(part);
            }
            else
            {
                textBuf.append(part.content);
            }
        }
        if (textBuf.length() > 0)
        {
            merged.add(Part.text(textBuf.toString()));
        }
        return merged;
    }

    private static int findClosingBrace(String input, int openBraceIndex)
    {
        if (openBraceIndex < 0 || openBraceIndex >= input.length() || input.charAt(openBraceIndex) != '{')
        {
            return -1;
        }
        int depth = 0;
        for (int i = openBraceIndex; i < input.length(); i++)
        {
            char c = input.charAt(i);
            if (c == '{')
            {
                depth++;
            }
            else if (c == '}')
            {
                depth--;
                if (depth == 0)
                {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String tryRenderLatexHtml(String latex)
    {
        byte[] png = tryRenderLatexPng(latex);
        if (png == null)
        {
            return null;
        }
        try
        {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            int heightPt = Math.max(10, Math.round(image.getHeight() * 0.75f));
            String b64 = Base64.getEncoder().encodeToString(png);
            return "<img class=\"formula-img\" src=\"data:image/png;base64," + b64
                    + "\" style=\"vertical-align:middle;height:" + heightPt + "pt;\" alt=\"\"/>";
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static byte[] tryRenderLatexPng(String latex)
    {
        if (StringUtils.isEmpty(latex))
        {
            return null;
        }
        try
        {
            TeXFormula formula = new TeXFormula(latex.trim());
            TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_TEXT, FONT_SIZE);
            icon.setInsets(new Insets(1, 2, 1, 2));
            BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setColor(new Color(0, 0, 0, 0));
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2.setColor(Color.BLACK);
            icon.paintIcon(null, g2, 0, 0);
            g2.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static void appendPlainRun(XWPFParagraph paragraph, String text, int fontSize, boolean colorRed)
    {
        if (StringUtils.isEmpty(text))
        {
            return;
        }
        XWPFRun run = paragraph.createRun();
        run.setFontSize(fontSize);
        run.setFontFamily("SimSun");
        if (colorRed)
        {
            run.setColor("CC0000");
        }
        run.setText(text);
    }

    private static void appendInlineImage(XWPFParagraph paragraph, byte[] png, int fontSize) throws Exception
    {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        double scale = fontSize / FONT_SIZE;
        int widthPx = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int heightPx = Math.max(1, (int) Math.round(image.getHeight() * scale));
        XWPFRun run = paragraph.createRun();
        try (ByteArrayInputStream in = new ByteArrayInputStream(png))
        {
            run.addPicture(in, Document.PICTURE_TYPE_PNG, "formula", Units.toEMU(widthPx), Units.toEMU(heightPx));
        }
    }

    private static boolean containsChinese(String text)
    {
        for (int i = 0; i < text.length(); i++)
        {
            if (isChinese(text.charAt(i)))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isChinese(char ch)
    {
        return ch >= '\u4e00' && ch <= '\u9fff';
    }

    private static String escapeHtml(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final class Part
    {
        private final boolean math;
        private final String content;

        private Part(boolean math, String content)
        {
            this.math = math;
            this.content = content;
        }

        private static Part text(String content)
        {
            return new Part(false, content);
        }

        private static Part math(String content)
        {
            return new Part(true, content);
        }
    }
}
