package com.ruoyi.system.service.education.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.config.EduQbAiTutorProperties;
import com.ruoyi.system.domain.education.EduQbAiTutorConfigVO;
import com.ruoyi.system.domain.education.EduQbAiTutorMessage;
import com.ruoyi.system.domain.education.EduQbAiTutorRequest;
import com.ruoyi.system.domain.education.EduQbAiTutorParsedReply;
import com.ruoyi.system.domain.education.EduQbAiTutorResponse;
import com.ruoyi.system.domain.education.EduQbConstants;
import com.ruoyi.system.domain.education.EduQbQuestion;
import com.ruoyi.system.service.education.IEduQbAiTutorConfigService;
import com.ruoyi.system.service.education.IEduQbAiTutorService;
import com.ruoyi.system.service.education.IEduQbQuestionService;
import com.ruoyi.system.service.education.support.EduQbAiTutorReplySupport;
import com.ruoyi.system.service.education.support.EduQbPracticeAnswerSupport;

@Service
public class EduQbAiTutorServiceImpl implements IEduQbAiTutorService
{
    private static final Logger log = LoggerFactory.getLogger(EduQbAiTutorServiceImpl.class);

    private static final String MODE_EXPLAIN = "explain";
    private static final String MODE_HINT = "hint";
    private static final String MODE_CHAT = "chat";

    private static final int MAX_HISTORY = 10;

    @Autowired
    private IEduQbAiTutorConfigService aiTutorConfigService;

    @Autowired
    private IEduQbQuestionService questionService;

    @Override
    public EduQbAiTutorConfigVO selectConfig()
    {
        EduQbAiTutorProperties runtime = aiTutorConfigService.resolveRuntimeConfig();
        EduQbAiTutorConfigVO vo = new EduQbAiTutorConfigVO();
        vo.setEnabled(runtime.isEnabled());
        vo.setAiPowered(runtime.isRemoteConfigured());
        vo.setModel(runtime.getModel());
        return vo;
    }

    @Override
    public EduQbAiTutorResponse chat(EduQbAiTutorRequest request)
    {
        EduQbAiTutorProperties runtime = aiTutorConfigService.resolveRuntimeConfig();
        if (!runtime.isEnabled())
        {
            throw new ServiceException("AI\u8bb2\u9898\u529f\u80fd\u672a\u542f\u7528");
        }
        if (request == null || request.getQuestionId() == null)
        {
            throw new ServiceException("\u8bf7\u9009\u62e9\u9898\u76ee");
        }
        String mode = normalizeMode(request.getMode());
        EduQbQuestion question = requireApprovedQuestion(request.getQuestionId());
        String userMessage = resolveUserMessage(mode, request.getMessage());

        String reply;
        boolean aiPowered = false;
        if (runtime.isRemoteConfigured())
        {
            try
            {
                reply = callRemoteAi(runtime, mode, question, userMessage, request.getHistory());
                aiPowered = true;
            }
            catch (Exception ex)
            {
                log.warn("AI tutor remote call failed, fallback to local: {}", ex.getMessage());
                reply = buildLocalReply(mode, question, userMessage);
                reply = EduQbAiTutorReplySupport.appendDefaultChoicesBlock(reply);
            }
        }
        else
        {
            reply = buildLocalReply(mode, question, userMessage);
            reply = EduQbAiTutorReplySupport.appendDefaultChoicesBlock(reply);
        }

        EduQbAiTutorParsedReply parsed = EduQbAiTutorReplySupport.parse(reply);
        if (parsed.getChoices().isEmpty())
        {
            parsed = EduQbAiTutorReplySupport.parse(
                    EduQbAiTutorReplySupport.appendDefaultChoicesBlock(parsed.getReply()));
        }
        EduQbAiTutorResponse response = new EduQbAiTutorResponse();
        response.setReply(parsed.getReply());
        response.setChoiceQuestion(parsed.getChoiceQuestion());
        response.setChoices(parsed.getChoices());
        response.setMode(mode);
        response.setQuestionId(question.getQuestionId());
        response.setAiPowered(aiPowered);
        return response;
    }

    private String callRemoteAi(EduQbAiTutorProperties runtime, String mode, EduQbQuestion question, String userMessage,
            List<EduQbAiTutorMessage> history)
    {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", buildSystemPrompt(mode, question));
        messages.add(system);

        if (history != null)
        {
            int start = Math.max(0, history.size() - MAX_HISTORY);
            for (int i = start; i < history.size(); i++)
            {
                EduQbAiTutorMessage item = history.get(i);
                if (item == null || StringUtils.isEmpty(item.getContent()))
                {
                    continue;
                }
                String role = "assistant".equalsIgnoreCase(item.getRole()) ? "assistant" : "user";
                Map<String, String> msg = new HashMap<>();
                msg.put("role", role);
                msg.put("content", item.getContent().trim());
                messages.add(msg);
            }
        }

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        Map<String, Object> body = new HashMap<>();
        body.put("model", runtime.getModel());
        body.put("messages", messages);
        body.put("temperature", runtime.getTemperature());

        String responseText = postJson(runtime, runtime.resolveChatUrl(), JSON.toJSONString(body));
        JSONObject root = JSON.parseObject(responseText);
        if (root == null)
        {
            throw new ServiceException("AI\u54cd\u5e94\u89e3\u6790\u5931\u8d25");
        }
        if (root.containsKey("error"))
        {
            JSONObject error = root.getJSONObject("error");
            String msg = error != null ? error.getString("message") : root.getString("error");
            throw new ServiceException("AI\u8c03\u7528\u5931\u8d25\uff1a" + (msg != null ? msg : "unknown"));
        }
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty())
        {
            throw new ServiceException("AI\u672a\u8fd4\u56de\u6709\u6548\u5185\u5bb9");
        }
        JSONObject first = choices.getJSONObject(0);
        JSONObject message = first != null ? first.getJSONObject("message") : null;
        String content = message != null ? message.getString("content") : null;
        if (StringUtils.isEmpty(content))
        {
            throw new ServiceException("AI\u672a\u8fd4\u56de\u6709\u6548\u5185\u5bb9");
        }
        return content.trim();
    }

    private String buildLocalReply(String mode, EduQbQuestion question, String userMessage)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\u3010\u672c\u5730\u8bb2\u9898\u6a21\u5f0f\u3011\u5f53\u524d\u672a\u914d\u7f6e AI \u63a5\u53e3\uff0c\u4ee5\u9898\u5e93\u89e3\u6790\u4e3a\u4f60\u8bb2\u89e3\u3002\n\n");
        if (MODE_HINT.equals(mode))
        {
            sb.append("\uD83D\uDCA1 \u601d\u8def\u63d0\u793a\uff1a\n");
            sb.append(buildHintFromQuestion(question));
            sb.append("\n\n\u5982\u9700\u5b8c\u6574\u6b65\u9aa4\uff0c\u53ef\u5207\u6362\u5230\u300c\u8be6\u7ec6\u8bb2\u89e3\u300d\u6216\u914d\u7f6e AI \u63a5\u53e3\u540e\u7ee7\u7eed\u8ffd\u95ee\u3002");
            return sb.toString();
        }
        if (MODE_CHAT.equals(mode))
        {
            sb.append("\u4f60\u95ee\uff1a").append(userMessage).append("\n\n");
            sb.append(buildExplainFromQuestion(question));
            return sb.toString();
        }
        sb.append("\uD83D\uDCD6 \u8be6\u7ec6\u8bb2\u89e3\uff1a\n");
        sb.append(buildExplainFromQuestion(question));
        return sb.toString();
    }

    private String buildHintFromQuestion(EduQbQuestion question)
    {
        if (StringUtils.isNotEmpty(question.getKnowledgePoints()))
        {
            return "\u5148\u56de\u987e\u77e5\u8bc6\u70b9\uff1a" + stripHtml(question.getKnowledgePoints())
                    + "\u3002\u518d\u7ed3\u5408\u9898\u5e72\u627e\u51fa\u5df2\u77e5\u91cf\u4e0e\u6240\u6c42\u91cf\u7684\u5173\u7cfb\u3002";
        }
        if (StringUtils.isNotEmpty(question.getAnalysis()))
        {
            String analysis = stripHtml(question.getAnalysis());
            if (analysis.length() > 120)
            {
                return analysis.substring(0, 120) + "\u2026";
            }
            return analysis;
        }
        return "\u4ed4\u7ec6\u9605\u8bfb\u9898\u5e72\uff0c\u5212\u5206\u5df2\u77e5\u6761\u4ef6\u4e0e\u672a\u77e5\u91cf\uff0c\u9009\u62e9\u5408\u9002\u516c\u5f0f\u6216\u65b9\u6cd5\u9010\u6b65\u63a8\u5bfc\u3002";
    }

    private String buildExplainFromQuestion(EduQbQuestion question)
    {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(question.getKnowledgePoints()))
        {
            sb.append("\u77e5\u8bc6\u70b9\uff1a").append(stripHtml(question.getKnowledgePoints())).append("\n\n");
        }
        if (StringUtils.isNotEmpty(question.getAnalysis()))
        {
            sb.append(stripHtml(question.getAnalysis()));
        }
        else
        {
            sb.append("\u8be5\u9898\u6682\u65e0\u5b98\u65b9\u89e3\u6790\u3002\u5efa\u8bae\u5148\u5c1d\u8bd5\u72ec\u7acb\u601d\u8003\uff0c\u6216\u5728\u7ec3\u4e60\u4e2d\u5bf9\u7167\u7b54\u6848\u3002");
        }
        String answer = EduQbPracticeAnswerSupport.formatCorrectAnswerDisplay(
                question.getQuestionType(), question.getCorrectAnswer());
        if (StringUtils.isNotEmpty(answer))
        {
            sb.append("\n\n\u53c2\u8003\u7b54\u6848\uff1a").append(stripHtml(answer));
        }
        return sb.toString().trim();
    }

    private String buildSystemPrompt(String mode, EduQbQuestion question)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\u4f60\u662f\u4e00\u4f4d\u5584\u4e8e\u542f\u53d1\u5b66\u751f\u601d\u8003\u7684 K12 \u5b66\u79d1\u8f85\u5bfc\u8001\u5e08\uff0c\u7528\u53e3\u8bed\u5316\u3001\u9f13\u52b1\u5f0f\u7684\u4e2d\u6587\u4e0e\u5b66\u751f\u5bf9\u8bdd\u3002\n");
        sb.append("\u6559\u5b66\u539f\u5219\uff1a\n");
        sb.append("1. \u91c7\u7528\u5f15\u5bfc\u5f0f\u6559\u5b66\uff1a\u5148\u5e2e\u5b66\u751f\u7406\u6e05\u9898\u610f\u3001\u5df2\u77e5\u6761\u4ef6\u4e0e\u5f85\u6c42\u91cf\uff0c\u518d\u9009\u62e9\u9002\u7528\u6a21\u578b\u6216\u65b9\u6cd5\uff1b\n");
        sb.append("2. \u6bcf\u6b21\u53ea\u63a8\u8fdb\u4e00\u5c0f\u6b65\uff0c\u4e0d\u8981\u4e00\u6b21\u6027\u7ed9\u51fa\u5b8c\u6574\u89e3\u9898\u8fc7\u7a0b\u548c\u6700\u7ec8\u6570\u503c\u7b54\u6848\uff0c\u9664\u975e\u5b66\u751f\u660e\u786e\u8981\u6c42\uff1b\n");
        sb.append("3. \u6bcf\u6b21\u56de\u590d\u672b\u5c3e\u5fc5\u987b\u9644\u52a0\u5b66\u751f\u53ef\u70b9\u9009\u7684\u9009\u9879\u5757\uff0c\u683c\u5f0f\u4e25\u683c\u5982\u4e0b\uff08\u6b63\u6587\u91cc\u4e0d\u8981\u91cd\u590d\u5217\u51fa\u8fd9\u4e9b\u9009\u9879\uff09\uff1a\n");
        sb.append("[CHOICES]\n");
        sb.append("\u95ee\uff1a\uff08\u4f60\u7684\u542f\u53d1\u6027\u8bbe\u95ee\uff09\n");
        sb.append("A. \uff08\u7b2c\u4e00\u4e2a\u53ef\u9009\u7b54\u6848\uff09\n");
        sb.append("B. \uff08\u7b2c\u4e8c\u4e2a\u53ef\u9009\u7b54\u6848\uff09\n");
        sb.append("C. \uff08\u7b2c\u4e09\u4e2a\u53ef\u9009\u7b54\u6848\uff09\n");
        sb.append("D. \u6211\u8fd8\u4e0d\u592a\u786e\u5b9a\uff0c\u8bf7\u518d\u63d0\u793a\u4e00\u4e0b\n");
        sb.append("[/CHOICES]\n");
        sb.append("\u9009\u9879\u8981\u4e0e\u5f53\u524d\u8bb2\u89e3\u6b65\u9aa4\u76f8\u5173\uff0c\u63d0\u4f9b 3-4 \u4e2a\u6709\u533a\u522b\u7684\u601d\u8003\u65b9\u5411\uff0c\u907f\u514d\u76f4\u63a5\u7ed9\u51fa\u6700\u7ec8\u6570\u503c\u7b54\u6848\uff1b\n");
        sb.append("4. \u6570\u5b66\u7269\u7406\u5316\u5b66\u516c\u5f0f\u7528 $...$ \u8868\u793a\u884c\u5185\u516c\u5f0f\uff0c\u72ec\u7acb\u4e00\u884c\u7528 $$...$$\uff1b\u5355\u4f4d\u7528 \\\\text{m/s}\u3001\\\\text{m/s}^2\uff0c\u4e0b\u6807\u53ef\u5199 $t_{\\\\text{\u4e0a}}$\uff1b\n");
        sb.append("5. \u53ef\u7528\u77ed\u6807\u9898\u548c\u5217\u8868\u7ec4\u7ec7\u5185\u5bb9\uff0c\u4f46\u907f\u514d\u50cf\u6807\u51c6\u7b54\u6848\u89e3\u6790\u90a3\u6837\u5806\u780c\u6b65\u9aa4\u3002\n\n");
        sb.append("\u9898\u578b\uff1a").append(nullToEmpty(question.getQuestionType())).append("\n");
        if (StringUtils.isNotEmpty(question.getSubjectName()))
        {
            sb.append("\u5b66\u79d1\uff1a").append(question.getSubjectName()).append("\n");
        }
        if (StringUtils.isNotEmpty(question.getChapterText()))
        {
            sb.append("\u7ae0\u8282\uff1a").append(stripHtml(question.getChapterText())).append("\n");
        }
        sb.append("\u9898\u5e72\uff1a\n").append(stripHtml(question.getContent())).append("\n");
        if (StringUtils.isNotEmpty(question.getOptions()))
        {
            sb.append("\u9009\u9879\uff1a").append(stripHtml(question.getOptions())).append("\n");
        }
        if (StringUtils.isNotEmpty(question.getKnowledgePoints()))
        {
            sb.append("\u77e5\u8bc6\u70b9\uff1a").append(stripHtml(question.getKnowledgePoints())).append("\n");
        }
        if (MODE_HINT.equals(mode))
        {
            sb.append("\u5f53\u524d\u6a21\u5f0f\uff1a\u601d\u8def\u63d0\u793a\u3002\u53ea\u7ed9\u65b9\u5411\u6027\u63d0\u793a\u548c\u5173\u952e\u601d\u8003\u70b9\uff0c\u4e0d\u7ed9\u516c\u5f0f\u63a8\u5bfc\u7ed3\u679c\u548c\u6570\u503c\u7b54\u6848\uff0c\u7528\u542f\u53d1\u6027\u95ee\u9898\u5f15\u5bfc\u3002");
        }
        else if (MODE_CHAT.equals(mode))
        {
            sb.append("\u5f53\u524d\u6a21\u5f0f\uff1a\u81ea\u7531\u63d0\u95ee\u3002\u9488\u5bf9\u5b66\u751f\u8ffd\u95ee\u56de\u7b54\uff0c\u7ee7\u7eed\u7528\u5f15\u5bfc\u5f0f\u8bb2\u89e3\uff0c\u4e0d\u8981\u4e3b\u52a8\u5c55\u5f00\u5b8c\u6574\u89e3\u9898\u8fc7\u7a0b\u3002");
        }
        else
        {
            sb.append("\u5f53\u524d\u6a21\u5f0f\uff1a\u8be6\u7ec6\u8bb2\u89e3\u3002\u5148\u5e26\u5b66\u751f\u8bfb\u9898\u5206\u6790\u9898\u610f\u4e0e\u5df2\u77e5\u6761\u4ef6\uff0c\u53ea\u7ed9\u51fa\u7b2c\u4e00\u6b65\u601d\u8003\u6846\u67b6\uff0c\u4ee5 1-2 \u4e2a\u542f\u53d1\u6027\u95ee\u9898\u7ed3\u5c3e\uff0c\u7b49\u5f85\u5b66\u751f\u56de\u5e94\u540e\u518d\u7ee7\u7eed\u3002");
        }
        return sb.toString();
    }

    private String resolveUserMessage(String mode, String message)
    {
        if (MODE_CHAT.equals(mode))
        {
            if (StringUtils.isEmpty(message))
            {
                throw new ServiceException("\u8bf7\u8f93\u5165\u4f60\u7684\u95ee\u9898");
            }
            return message.trim();
        }
        if (StringUtils.isNotEmpty(message))
        {
            return message.trim();
        }
        if (MODE_HINT.equals(mode))
        {
            return "\u8bf7\u7ed9\u6211\u4e00\u4e9b\u89e3\u9898\u601d\u8def\u63d0\u793a\uff0c\u7528\u542f\u53d1\u6027\u95ee\u9898\u5f15\u5bfc\u6211\u601d\u8003\uff0c\u4e0d\u8981\u76f4\u63a5\u544a\u8bc9\u6211\u7b54\u6848\u3002";
        }
        return "\u8bf7\u7528\u5f15\u5bfc\u5f0f\u5e2e\u6211\u7406\u89e3\u8fd9\u9053\u9898\uff1a\u5148\u5e2e\u6211\u7406\u6e05\u9898\u610f\u548c\u5df2\u77e5\u6761\u4ef6\uff0c\u7ed9\u51fa\u7b2c\u4e00\u6b65\u601d\u8003\u65b9\u5411\uff0c\u4e0d\u8981\u76f4\u63a5\u7ed9\u51fa\u5b8c\u6574\u7b54\u6848\u3002";
    }

    private String normalizeMode(String mode)
    {
        if (MODE_HINT.equalsIgnoreCase(mode))
        {
            return MODE_HINT;
        }
        if (MODE_CHAT.equalsIgnoreCase(mode))
        {
            return MODE_CHAT;
        }
        return MODE_EXPLAIN;
    }

    private EduQbQuestion requireApprovedQuestion(Long questionId)
    {
        EduQbQuestion question = questionService.selectEduQbQuestionById(questionId);
        if (question == null || !"0".equals(question.getDelFlag()))
        {
            throw new ServiceException("\u8bd5\u9898\u4e0d\u5b58\u5728");
        }
        if (!EduQbConstants.STATUS_APPROVED.equals(question.getStatus()))
        {
            throw new ServiceException("\u8bd5\u9898\u672a\u901a\u8fc7\u5ba1\u6838");
        }
        return question;
    }

    private String stripHtml(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String nullToEmpty(String text)
    {
        return text == null ? "" : text;
    }

    private String postJson(EduQbAiTutorProperties runtime, String url, String json)
    {
        HttpURLConnection conn = null;
        try
        {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(runtime.getConnectTimeoutMs());
            conn.setReadTimeout(runtime.getReadTimeoutMs());
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + runtime.getApiKey());
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream())
            {
                os.write(bytes);
            }
            String response = readStream(conn);
            int code = conn.getResponseCode();
            if (code >= 400)
            {
                throw new ServiceException("AI HTTP " + code + ": " + response);
            }
            return response;
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("AI\u8bf7\u6c42\u5931\u8d25: " + ex.getMessage());
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
    }

    private String readStream(HttpURLConnection conn) throws Exception
    {
        int code = conn.getResponseCode();
        java.io.InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
