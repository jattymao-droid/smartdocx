package com.ruoyi.system.service.education.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbCatalogChapter;
import com.ruoyi.system.domain.education.EduQbChapterMatchResult;
import com.ruoyi.system.domain.education.EduQbImportBlock;
import com.ruoyi.system.mapper.education.EduQbTextbookMapper;

@Service
public class EduQbChapterMatchService
{
    private static final double MATCH_THRESHOLD = 0.55D;

    @Autowired
    private EduQbTextbookMapper textbookMapper;

    public EduQbChapterMatchResult matchChapter(Long textbookId, String hint)
    {
        EduQbChapterMatchResult result = new EduQbChapterMatchResult();
        result.setHint(hint);
        result.setMatched(false);
        result.setScore(0D);
        if (textbookId == null || StringUtils.isEmpty(hint))
        {
            return result;
        }
        List<ChapterCandidate> candidates = loadCandidates(textbookId);
        if (candidates.isEmpty())
        {
            return result;
        }
        ChapterCandidate best = pickBest(candidates, hint);
        if (best == null || best.score < MATCH_THRESHOLD)
        {
            return result;
        }
        result.setChapterId(best.chapterId);
        result.setChapterName(best.chapterName);
        result.setChapterText(best.chapterText);
        result.setScore(Math.round(best.score * 1000D) / 1000D);
        result.setMatched(true);
        return result;
    }

    public List<EduQbChapterMatchResult> matchChapters(Long textbookId, List<String> hints)
    {
        List<EduQbChapterMatchResult> results = new ArrayList<>();
        if (hints == null)
        {
            return results;
        }
        for (String hint : hints)
        {
            if (StringUtils.isEmpty(hint))
            {
                continue;
            }
            results.add(matchChapter(textbookId, hint));
        }
        return results;
    }

    public List<String> extractHeadingHints(List<EduQbImportBlock> blocks)
    {
        List<String> hints = new ArrayList<>();
        if (blocks == null)
        {
            return hints;
        }
        for (EduQbImportBlock block : blocks)
        {
            if (block == null || StringUtils.isEmpty(block.getText()))
            {
                continue;
            }
            if ("heading".equals(block.getBlockKind()) || EduQbImportHeadingSupport.isChapterHeading(block.getText()))
            {
                String hint = EduQbImportHeadingSupport.cleanHeadingHint(block.getText());
                if (StringUtils.isEmpty(hint))
                {
                    hint = block.getText().trim();
                }
                if (!hints.contains(hint))
                {
                    hints.add(hint);
                }
            }
        }
        return hints;
    }

    public EduQbChapterMatchResult matchBestFromBlocks(Long textbookId, List<EduQbImportBlock> blocks)
    {
        List<String> hints = extractHeadingHints(blocks);
        EduQbChapterMatchResult best = new EduQbChapterMatchResult();
        best.setMatched(false);
        best.setScore(0D);
        for (int i = hints.size() - 1; i >= 0; i--)
        {
            EduQbChapterMatchResult hit = matchChapter(textbookId, hints.get(i));
            if (Boolean.TRUE.equals(hit.getMatched()) && (best.getScore() == null || hit.getScore() > best.getScore()))
            {
                best = hit;
            }
        }
        if (!Boolean.TRUE.equals(best.getMatched()) && !hints.isEmpty())
        {
            return matchChapter(textbookId, hints.get(hints.size() - 1));
        }
        return best;
    }

    private List<ChapterCandidate> loadCandidates(Long textbookId)
    {
        List<EduQbCatalogChapter> chapters = textbookMapper.selectChapterListByTextbookId(textbookId);
        if (chapters == null || chapters.isEmpty())
        {
            return List.of();
        }
        Map<Long, EduQbCatalogChapter> byId = new HashMap<>();
        for (EduQbCatalogChapter chapter : chapters)
        {
            byId.put(chapter.getChapterId(), chapter);
        }
        List<ChapterCandidate> candidates = new ArrayList<>();
        for (EduQbCatalogChapter chapter : chapters)
        {
            String path = buildChapterPath(byId, chapter.getChapterId());
            ChapterCandidate candidate = new ChapterCandidate();
            candidate.chapterId = chapter.getChapterId();
            candidate.chapterName = chapter.getChapterName();
            candidate.chapterText = path;
            candidates.add(candidate);
        }
        return candidates;
    }

    private String buildChapterPath(Map<Long, EduQbCatalogChapter> byId, Long chapterId)
    {
        LinkedHashMap<Long, String> segments = new LinkedHashMap<>();
        Long current = chapterId;
        int guard = 0;
        while (current != null && guard++ < 20)
        {
            EduQbCatalogChapter chapter = byId.get(current);
            if (chapter == null)
            {
                break;
            }
            segments.put(current, chapter.getChapterName());
            Long parentId = chapter.getParentId();
            if (parentId == null || parentId <= 0L)
            {
                break;
            }
            current = parentId;
        }
        List<String> names = new ArrayList<>(segments.values());
        java.util.Collections.reverse(names);
        return String.join(" > ", names);
    }

    private ChapterCandidate pickBest(List<ChapterCandidate> candidates, String hint)
    {
        String normalizedHint = normalize(hint);
        String cleanedHint = normalize(EduQbImportHeadingSupport.cleanHeadingHint(hint));
        ChapterCandidate best = null;
        for (ChapterCandidate candidate : candidates)
        {
            double score = scoreCandidate(candidate, normalizedHint, cleanedHint, hint);
            if (best == null || score > best.score)
            {
                best = candidate;
                best.score = score;
            }
        }
        return best;
    }

    private double scoreCandidate(ChapterCandidate candidate, String normalizedHint, String cleanedHint, String rawHint)
    {
        String name = normalize(candidate.chapterName);
        String path = normalize(candidate.chapterText);
        if (StringUtils.isEmpty(name))
        {
            return 0D;
        }
        if (name.equals(normalizedHint) || name.equals(cleanedHint))
        {
            return 1D;
        }
        if (path.equals(normalize(rawHint)))
        {
            return 0.98D;
        }
        if (EduQbChapterTextMatchSupport.matchesSegment(candidate.chapterText, rawHint)
                || EduQbChapterTextMatchSupport.matchesSegment(candidate.chapterText, cleanedHint))
        {
            return 0.95D;
        }
        if (EduQbChapterTextMatchSupport.matchesSegment(rawHint, candidate.chapterName))
        {
            return 0.92D;
        }
        if (normalizedHint.contains(name) || name.contains(cleanedHint) || cleanedHint.contains(name))
        {
            return 0.82D;
        }
        if (path.contains(cleanedHint) || cleanedHint.contains(path))
        {
            return 0.75D;
        }
        return overlapRatio(name, cleanedHint) * 0.7D;
    }

    private double overlapRatio(String a, String b)
    {
        if (StringUtils.isEmpty(a) || StringUtils.isEmpty(b))
        {
            return 0D;
        }
        int min = Math.min(a.length(), b.length());
        int max = Math.max(a.length(), b.length());
        if (max == 0)
        {
            return 0D;
        }
        int common = 0;
        for (int i = 0; i < min; i++)
        {
            if (a.charAt(i) == b.charAt(i))
            {
                common++;
            }
        }
        return (double) common / max;
    }

    private String normalize(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.trim()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", "")
                .replaceAll("[\\uFF1A:\\uFF0C,\\u3002\\uFF0E.\\u3001;\\uFF1B\\uFF08\\uFF09()\\[\\]\\u3010\\u3011]", "")
                .toLowerCase();
    }

    private static class ChapterCandidate
    {
        private Long chapterId;
        private String chapterName;
        private String chapterText;
        private double score;
    }
}
