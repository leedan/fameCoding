package cn.famecode.ai.domain.agent.service.completion;

import cn.famecode.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.famecode.ai.domain.agent.model.valobj.completion.InlineCompletionCommandVO;
import cn.famecode.ai.domain.agent.model.valobj.completion.InlineCompletionResultVO;
import cn.famecode.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import cn.famecode.ai.domain.agent.model.valobj.properties.InlineCompletionRouteProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行内代码极速预测补全领域服务实现（模式三 通道 A）
 *
 * <p>动态自适应解析机制：
 * <ul>
 *   <li>优先级 1：读取 application YAML 中显式配置的 ai.route.inline-completion</li>
 *   <li>优先级 2：若未单独指定，自动继承 intent-ai-api 中配置的快速轻量/内网模型（如 Qwen-Coder）</li>
 *   <li>优先级 3：自动继承当前已激活主 Agent（如 code-agent.yml）的模型与 API 地址</li>
 *   <li>严禁在 Java 代码中硬编码任何默认地址、模型或秘钥</li>
 * </ul>
 *
 */
@Slf4j
@Service
public class InlineCompletionDomainService implements IInlineCompletionDomainService {

    private static final String SYSTEM_PROMPT =
            "You are an ultra-fast, high-precision code completion AI.\n" +
            "Your sole objective is to complete code at the exact cursor position between PREFIX and SUFFIX.\n" +
            "Strict output guidelines:\n" +
            "1. Output ONLY the raw code to be inserted at the cursor.\n" +
            "2. Do NOT repeat or echo any part of the PREFIX or SUFFIX.\n" +
            "3. Do NOT wrap your output in markdown code blocks (NO ``` or ```language).\n" +
            "4. Do NOT provide any conversational responses, reasoning, or markdown explanations.\n" +
            "5. If no completion is clearly needed or possible, output nothing.";

    @Resource
    private InlineCompletionRouteProperties properties;

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Resource
    private ObjectMapper objectMapper;

    /** 可选注入 application-dev.yml 中的 intent-ai-api 作为低延迟快速模型的候选继承源 */
    @Value("${intent-ai-api.base-url:}")
    private String intentBaseUrl;

    @Value("${intent-ai-api.api-key:}")
    private String intentApiKey;

    @Value("${intent-ai-api.chat-model.model:}")
    private String intentModel;

    @Value("${intent-ai-api.completions-path:chat/completions}")
    private String intentCompletionsPath;

    /** 根据 baseUrl + timeoutMs 动态缓存 RestClient，配置更新时自动无缝复用 */
    private final Map<String, RestClient> restClientCache = new ConcurrentHashMap<>();

    @Data
    @Builder
    private static class ResolvedModelConfig {
        private boolean enabled;
        private String baseUrl;
        private String apiKey;
        private String model;
        private int maxTokens;
        private double temperature;
        private int timeoutMs;
        private String completionsPath;
        private String configSource;
    }

    @Override
    public InlineCompletionResultVO complete(InlineCompletionCommandVO commandVO) {
        long startTime = System.currentTimeMillis();

        // 1. 动态解析当前生效的模型配置（优先级：显式配置 > intent-ai-api > 激活的Agent）
        ResolvedModelConfig config = resolveEffectiveConfig();
        if (!config.isEnabled() || StringUtils.isBlank(config.getBaseUrl()) || StringUtils.isBlank(config.getModel())) {
            return buildEmptyResponse(0L, config != null ? config.getModel() : "");
        }

        String prefix = commandVO.getPrefix();
        String suffix = commandVO.getSuffix();
        if (StringUtils.isBlank(prefix) && StringUtils.isBlank(suffix)) {
            return buildEmptyResponse(0L, config.getModel());
        }

        String language = StringUtils.isNotBlank(commandVO.getLanguage()) ? commandVO.getLanguage() : "plaintext";
        String path = StringUtils.isNotBlank(commandVO.getPath()) ? commandVO.getPath() : "editor_file";

        // 截取合理长度的上下文，防止过大 payload 增加请求延迟
        String safePrefix = truncatePrefix(prefix, 1500);
        String safeSuffix = truncateSuffix(suffix, 800);

        // 组装 User Prompt
        String userPrompt = buildUserPrompt(language, path, safePrefix, safeSuffix);

        try {
            // 组装 OpenAI 兼容 Chat Completions 请求体
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", config.getModel());
            requestMap.put("messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestMap.put("max_tokens", config.getMaxTokens());
            requestMap.put("temperature", config.getTemperature());
            requestMap.put("stream", false);

            RestClient client = getOrCreateRestClient(config.getBaseUrl(), config.getTimeoutMs());

            String pathUri = config.getCompletionsPath();
            if (pathUri.startsWith("/")) {
                pathUri = pathUri.substring(1);
            }

            RestClient.RequestBodySpec requestSpec = client.post()
                    .uri(pathUri)
                    .contentType(MediaType.APPLICATION_JSON);

            if (StringUtils.isNotBlank(config.getApiKey()) && !"none".equalsIgnoreCase(config.getApiKey()) && !"EMPTY".equalsIgnoreCase(config.getApiKey())) {
                requestSpec.header("Authorization", "Bearer " + config.getApiKey());
            }

            String responseJson = requestSpec.body(requestMap)
                    .retrieve()
                    .body(String.class);

            long durationMs = System.currentTimeMillis() - startTime;

            if (StringUtils.isBlank(responseJson)) {
                return buildEmptyResponse(durationMs, config.getModel());
            }

            // 解析返回结果
            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode choices = rootNode.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).path("message");
                String rawContent = messageNode.path("content").asText("");
                String cleanedCompletion = cleanCompletionText(rawContent, safeSuffix);

                if (StringUtils.isNotBlank(cleanedCompletion)) {
                    log.info("[InlineCompletion] 预测补全成功 (耗时: {}ms, 字符数: {}, 命中模型: {}, 来源: {})",
                            durationMs, cleanedCompletion.length(), config.getModel(), config.getConfigSource());

                    return InlineCompletionResultVO.builder()
                            .completion(cleanedCompletion)
                            .model(config.getModel())
                            .durationMs(durationMs)
                            .hasCompletion(true)
                            .build();
                }
            }

            return buildEmptyResponse(durationMs, config.getModel());

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.warn("[InlineCompletion] 补全请求异常降级 (耗时: {}ms, 模型: {}, 来源: {}): {}",
                    durationMs, config.getModel(), config.getConfigSource(), e.getMessage());
            return buildEmptyResponse(durationMs, config.getModel());
        }
    }

    /**
     * 动态自适应解析当前生效的模型配置
     */
    private ResolvedModelConfig resolveEffectiveConfig() {
        boolean enabled = properties.getEnabled() == null || properties.getEnabled();
        if (!enabled) {
            return ResolvedModelConfig.builder().enabled(false).build();
        }

        String baseUrl = properties.getBaseUrl();
        String apiKey = properties.getApiKey();
        String model = properties.getModel();
        String completionsPath = properties.getCompletionsPath();
        String configSource = "ai.route.inline-completion";

        // 优先级 1: 检查显式配置
        boolean hasExplicit = StringUtils.isNotBlank(baseUrl) && StringUtils.isNotBlank(model);

        // 优先级 2: 若未显式配置，优先检查 intent-ai-api（用户常在此配置内网/轻量模型）
        if (!hasExplicit && StringUtils.isNotBlank(intentBaseUrl) && StringUtils.isNotBlank(intentModel)) {
            baseUrl = intentBaseUrl;
            apiKey = intentApiKey;
            model = intentModel;
            completionsPath = intentCompletionsPath;
            configSource = "inherited-from:intent-ai-api";
            hasExplicit = true;
        }

        // 优先级 3: 若依然为空，从当前激活的 Agent 配置中继承
        if (!hasExplicit) {
            AiAgentConfigTableVO agentVO = findPrimaryAgentConfig();
            if (agentVO != null && agentVO.getModule() != null) {
                AiAgentConfigTableVO.Module module = agentVO.getModule();

                if (module.getAiApi() != null) {
                    baseUrl = module.getAiApi().getBaseUrl();
                    apiKey = module.getAiApi().getApiKey();
                    completionsPath = module.getAiApi().getCompletionsPath();
                }
                if (module.getChatModel() != null) {
                    model = module.getChatModel().getModel();
                }

                configSource = "inherited-from-agent:" + (agentVO.getAppName() != null ? agentVO.getAppName() : "primary");
            }
        }

        cn.famecode.ai.domain.agent.service.util.OpenAiUrlNormalizer.NormalizedUrl normalized =
                cn.famecode.ai.domain.agent.service.util.OpenAiUrlNormalizer.normalize(baseUrl, completionsPath);
        baseUrl = normalized.getBaseUrl();
        completionsPath = normalized.getCompletionsPath();

        int maxTokens = properties.getMaxTokens() != null ? properties.getMaxTokens() : 128;
        double temperature = properties.getTemperature() != null ? properties.getTemperature() : 0.1;
        int timeoutMs = properties.getTimeoutMs() != null ? properties.getTimeoutMs() : 3500;

        return ResolvedModelConfig.builder()
                .enabled(StringUtils.isNotBlank(baseUrl) && StringUtils.isNotBlank(model))
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeoutMs(timeoutMs)
                .completionsPath(completionsPath)
                .configSource(configSource)
                .build();
    }

    private AiAgentConfigTableVO findPrimaryAgentConfig() {
        if (aiAgentAutoConfigProperties == null || aiAgentAutoConfigProperties.getTables() == null) {
            return null;
        }
        Map<String, AiAgentConfigTableVO> tables = aiAgentAutoConfigProperties.getTables();
        if (tables.isEmpty()) {
            return null;
        }
        if (tables.containsKey("unifiedAgent")) {
            return tables.get("unifiedAgent");
        }
        return tables.values().iterator().next();
    }

    private RestClient getOrCreateRestClient(String baseUrl, int timeoutMs) {
        String trimmed = baseUrl != null ? baseUrl.trim() : "";
        final String finalBaseUrl = trimmed.endsWith("/") ? trimmed : trimmed + "/";
        final int finalTimeoutMs = timeoutMs > 0 ? timeoutMs : 3500;

        String cacheKey = finalBaseUrl + ":" + finalTimeoutMs;
        return restClientCache.computeIfAbsent(cacheKey, k -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(finalTimeoutMs);
            requestFactory.setReadTimeout(finalTimeoutMs);

            return RestClient.builder()
                    .baseUrl(finalBaseUrl)
                    .requestFactory(requestFactory)
                    .build();
        });
    }

    private String buildUserPrompt(String language, String path, String prefix, String suffix) {
        return "Language: " + language + "\n" +
                "File: " + path + "\n\n" +
                "<<<PREFIX>>>\n" +
                prefix + "\n" +
                "<<<CURSOR>>>\n" +
                "<<<SUFFIX>>>\n" +
                suffix + "\n\n" +
                "Complete the exact code to be inserted at <<<CURSOR>>>. No markdown, no prefix/suffix repetition.";
    }

    private String cleanCompletionText(String raw, String suffix) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }

        String text = raw.trim();

        // 1. 去除开头的 markdown 标记
        if (text.startsWith("```")) {
            int newlineIndex = text.indexOf('\n');
            if (newlineIndex != -1) {
                text = text.substring(newlineIndex + 1);
            } else {
                text = text.substring(3);
            }
        }

        // 2. 去除末尾的 markdown 标记
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }

        // 3. 去除可能误输出的标记标签
        text = text.replace("<<<CURSOR>>>", "").replace("<<<SUFFIX>>>", "").replace("<<<PREFIX>>>", "");

        // 4. 去除可能重复 suffix 开头的首行代码
        if (StringUtils.isNotBlank(suffix)) {
            String firstSuffixLine = suffix.lines().findFirst().orElse("").trim();
            if (StringUtils.isNotBlank(firstSuffixLine) && text.endsWith(firstSuffixLine)) {
                text = text.substring(0, text.length() - firstSuffixLine.length());
            }
        }

        return text;
    }

    private String truncatePrefix(String str, int maxChars) {
        if (str == null || str.length() <= maxChars) {
            return str != null ? str : "";
        }
        return str.substring(str.length() - maxChars);
    }

    private String truncateSuffix(String str, int maxChars) {
        if (str == null || str.length() <= maxChars) {
            return str != null ? str : "";
        }
        return str.substring(0, maxChars);
    }

    private InlineCompletionResultVO buildEmptyResponse(long durationMs, String model) {
        return InlineCompletionResultVO.builder()
                .completion("")
                .model(model)
                .durationMs(durationMs)
                .hasCompletion(false)
                .build();
    }

}