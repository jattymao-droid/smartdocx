package com.ruoyi.system.service.education.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import com.ruoyi.common.core.exception.ServiceException;

/**
 * Normalizes question stems and computes content_hash / similarity scores.
 */
public final class EduQbContentHashSupport
{
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}\u3000-\u303f\uff00-\uffef]+");

    private EduQbContentHashSupport()
    {
    }

    public static String normalize(String content)
    {
        if (content == null)
        {
            return "";
        }
        String text = content.trim();
        if (text.isEmpty())
        {
            return "";
        }
        text = HTML_TAG.matcher(text).replaceAll("");
        text = text.toLowerCase();
        text = PUNCTUATION.matcher(text).replaceAll("");
        text = WHITESPACE.matcher(text).replaceAll("");
        return text;
    }

    public static String computeHash(String content)
    {
        String normalized = normalize(content);
        if (normalized.isEmpty())
        {
            return null;
        }
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash)
            {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u8ba1\u7b97\u9898\u5e72\u6458\u8981\u5931\u8d25");
        }
    }

    /**
     * Character bigram Jaccard similarity on normalized stems.
     */
    public static double similarity(String left, String right)
    {
        String a = normalize(left);
        String b = normalize(right);
        if (a.isEmpty() || b.isEmpty())
        {
            return 0D;
        }
        if (a.equals(b))
        {
            return 1D;
        }
        Set<String> gramsA = bigrams(a);
        Set<String> gramsB = bigrams(b);
        if (gramsA.isEmpty() || gramsB.isEmpty())
        {
            return a.equals(b) ? 1D : 0D;
        }
        int intersection = 0;
        for (String gram : gramsA)
        {
            if (gramsB.contains(gram))
            {
                intersection++;
            }
        }
        int union = gramsA.size() + gramsB.size() - intersection;
        return union == 0 ? 0D : (double) intersection / union;
    }

    private static Set<String> bigrams(String text)
    {
        Set<String> grams = new HashSet<>();
        if (text.length() == 1)
        {
            grams.add(text);
            return grams;
        }
        for (int i = 0; i < text.length() - 1; i++)
        {
            grams.add(text.substring(i, i + 2));
        }
        return grams;
    }
}
