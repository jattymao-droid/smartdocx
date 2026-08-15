package com.ruoyi.system.service.education.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.education.EduQbImportCommitItem;

public final class EduQbImportContentSupport
{
  private static final Pattern OPTION_LINE = Pattern.compile(
      "^[A-Ha-h][\\.\\uFF0E\\u3001\\u3002\\)\\uFF09:\\uFF1A]\\s*|^[A-Ha-h]\\s+\\S");

  private static final Pattern ANSWER_LINE = Pattern.compile(
      "^[\\s\\u3000]*(?:\u3010\u7b54\u6848\u3011|\u3010\u7b54\u3011|\u7b54\u6848|\u53c2\u8003\u7b54\u6848)[:\\s\\uFF1A]+(.+)$");

  private static final Pattern ANALYSIS_LINE = Pattern.compile(
      "^[\\s\\u3000]*(?:\u3010\u89e3\u6790\u3011|\u3010\u8be6\u89e3\u3011|\u89e3\u6790|\u8bd5\u9898\u89e3\u6790)[:\\s\\uFF1A]?(.*)$");

  private EduQbImportContentSupport()
  {
  }

  public static ParsedImportContent parseContent(String raw)
  {
    ParsedImportContent result = new ParsedImportContent();
    if (StringUtils.isEmpty(raw))
    {
      return result;
    }
    List<String> stemLines = new ArrayList<>();
    List<String> optionLines = new ArrayList<>();
    StringBuilder analysisBuf = new StringBuilder();
    boolean inAnalysis = false;
    for (String line : raw.split("\\r?\\n"))
    {
      for (String piece : expandLinePieces(line))
      {
        String text = piece == null ? "" : piece.trim();
        if (text.isEmpty())
        {
          continue;
        }
        Matcher answerMatcher = ANSWER_LINE.matcher(text);
        if (answerMatcher.find())
        {
          inAnalysis = false;
          result.setCorrectAnswer(EduQbExamPaperAnswerSupport.formatAnswerJson(
              answerMatcher.group(1).trim(), null));
          continue;
        }
        Matcher analysisMatcher = ANALYSIS_LINE.matcher(text);
        if (analysisMatcher.find())
        {
          inAnalysis = true;
          String tail = analysisMatcher.group(1);
          if (StringUtils.isNotEmpty(tail))
          {
            analysisBuf.append(tail.trim());
          }
          continue;
        }
        if (inAnalysis)
        {
          if (analysisBuf.length() > 0)
          {
            analysisBuf.append('\n');
          }
          analysisBuf.append(text);
          continue;
        }
        if (OPTION_LINE.matcher(text).find())
        {
          optionLines.add(text);
        }
        else
        {
          stemLines.add(text);
        }
      }
    }
    if (analysisBuf.length() > 0)
    {
      result.setAnalysis(analysisBuf.toString().trim());
    }
    result.setStem(EduQbQuestionContentSupport.stripLeadingQuestionNo(String.join("\n", stemLines).trim()));
    result.setOptionsJson(optionLines.isEmpty() ? null : JSON.toJSONString(optionLines));
    return result;
  }

  public static String optionsTextToJson(String optionsText)
  {
    if (StringUtils.isEmpty(optionsText))
    {
      return null;
    }
    List<String> lines = new ArrayList<>();
    for (String line : optionsText.split("\\r?\\n"))
    {
      String text = line == null ? "" : line.trim();
      if (!text.isEmpty())
      {
        lines.add(text);
      }
    }
    return lines.isEmpty() ? null : JSON.toJSONString(lines);
  }

  public static String resolveOptionsJson(EduQbImportCommitItem item, String stemContent)
  {
    if (item == null)
    {
      return null;
    }
    if (StringUtils.isNotEmpty(item.getOptions()))
    {
      return item.getOptions().trim();
    }
    if (StringUtils.isNotEmpty(item.getOptionsText()))
    {
      String json = optionsTextToJson(item.getOptionsText());
      if (StringUtils.isNotEmpty(json))
      {
        return json;
      }
    }
    String combined = stemContent;
    if (StringUtils.isNotEmpty(item.getOptionsText()))
    {
      combined = stemContent + "\n" + item.getOptionsText().trim();
    }
    return parseContent(combined).getOptionsJson();
  }

  private static List<String> expandLinePieces(String line)
  {
    List<String> result = new ArrayList<>();
    if (line == null)
    {
      return result;
    }
    String text = line.replace('\uFEFF', ' ').replaceAll("\\s+", " ").trim();
    if (text.isEmpty())
    {
      return result;
    }
    String[] parts = text.split("(?=(?:^|\\s)[A-Ha-d][\\.\\uFF0E\\u3001\\u3002\\)\\uFF09:：\\s])");
    int optionCount = 0;
    for (String part : parts)
    {
      String piece = part == null ? "" : part.trim();
      if (piece.isEmpty())
      {
        continue;
      }
      if (OPTION_LINE.matcher(piece).find())
      {
        optionCount++;
        result.add(piece);
      }
    }
    if (optionCount >= 2)
    {
      return result;
    }
    result.clear();
    result.add(text);
    return result;
  }

  public static class ParsedImportContent
  {
    private String stem = "";
    private String optionsJson;
    private String correctAnswer;
    private String analysis;

    public String getStem()
    {
      return stem;
    }

    public void setStem(String stem)
    {
      this.stem = stem == null ? "" : stem;
    }

    public String getOptionsJson()
    {
      return optionsJson;
    }

    public void setOptionsJson(String optionsJson)
    {
      this.optionsJson = optionsJson;
    }

    public String getCorrectAnswer()
    {
      return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer)
    {
      this.correctAnswer = correctAnswer;
    }

    public String getAnalysis()
    {
      return analysis;
    }

    public void setAnalysis(String analysis)
    {
      this.analysis = analysis;
    }
  }
}
