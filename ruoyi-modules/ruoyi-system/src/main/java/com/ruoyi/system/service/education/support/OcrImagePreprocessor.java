package com.ruoyi.system.service.education.support;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

/**
 * Preprocess exam photos / screenshots for Tesseract.
 */
public final class OcrImagePreprocessor
{
    public enum Mode
    {
        /** Grayscale + contrast, keep anti-aliasing (best for UI screenshots) */
        SOFT,
        /** Otsu binarization (best for scanned paper) */
        BINARY,
        /** Scale only */
        RAW
    }

    private OcrImagePreprocessor()
    {
    }

    public static BufferedImage prepare(BufferedImage source, int minWidth, Mode mode)
    {
        if (source == null)
        {
            return null;
        }
        BufferedImage cropped = cropToContent(source, 12);
        BufferedImage scaled = scaleToTargetWidth(cropped, Math.max(minWidth, 1600));
        if (mode == Mode.RAW)
        {
            return toRgb(scaled);
        }
        BufferedImage gray = toGrayscale(scaled);
        BufferedImage contrast = stretchContrast(gray, 1.25f, -18f);
        if (mode == Mode.SOFT)
        {
            return contrast;
        }
        return binarizeOtsu(contrast);
    }

    /** Trim uniform margins; keep central content block. */
    public static BufferedImage cropToContent(BufferedImage src, int padding)
    {
        int w = src.getWidth();
        int h = src.getHeight();
        int minX = w;
        int minY = h;
        int maxX = 0;
        int maxY = 0;
        boolean found = false;
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                if (isInkPixel(src.getRGB(x, y)))
                {
                    found = true;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (!found)
        {
            return src;
        }
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(w - 1, maxX + padding);
        maxY = Math.min(h - 1, maxY + padding);
        int cw = maxX - minX + 1;
        int ch = maxY - minY + 1;
        if (cw < w * 0.25 || ch < h * 0.25)
        {
            return src;
        }
        return src.getSubimage(minX, minY, cw, ch);
    }

    private static boolean isInkPixel(int rgb)
    {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int lum = (r * 299 + g * 587 + b * 114) / 1000;
        return lum < 175;
    }

    private static BufferedImage scaleToTargetWidth(BufferedImage src, int targetWidth)
    {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w == targetWidth)
        {
            return copyImage(src);
        }
        double ratio = (double) targetWidth / w;
        int nw = targetWidth;
        int nh = Math.max(1, (int) Math.round(h * ratio));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static BufferedImage toRgb(BufferedImage src)
    {
        if (src.getType() == BufferedImage.TYPE_INT_RGB)
        {
            return src;
        }
        return copyImage(src);
    }

    private static BufferedImage copyImage(BufferedImage src)
    {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage toGrayscale(BufferedImage src)
    {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    private static BufferedImage stretchContrast(BufferedImage src, float scale, float offset)
    {
        RescaleOp op = new RescaleOp(scale, offset, null);
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        op.filter(src, out);
        return out;
    }

    private static BufferedImage binarizeOtsu(BufferedImage gray)
    {
        int w = gray.getWidth();
        int h = gray.getHeight();
        int[] hist = new int[256];
        int total = w * h;
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                hist[gray.getRaster().getSample(x, y, 0)]++;
            }
        }
        int threshold = otsuThreshold(hist, total);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                int v = gray.getRaster().getSample(x, y, 0);
                out.setRGB(x, y, v >= threshold ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }
        return out;
    }

    private static int otsuThreshold(int[] hist, int total)
    {
        float sum = 0f;
        for (int i = 0; i < 256; i++)
        {
            sum += i * hist[i];
        }
        float sumB = 0f;
        int wB = 0;
        float max = 0f;
        int threshold = 127;
        for (int i = 0; i < 256; i++)
        {
            wB += hist[i];
            if (wB == 0)
            {
                continue;
            }
            int wF = total - wB;
            if (wF == 0)
            {
                break;
            }
            sumB += i * hist[i];
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float between = wB * wF * (mB - mF) * (mB - mF);
            if (between > max)
            {
                max = between;
                threshold = i;
            }
        }
        return threshold;
    }
}
