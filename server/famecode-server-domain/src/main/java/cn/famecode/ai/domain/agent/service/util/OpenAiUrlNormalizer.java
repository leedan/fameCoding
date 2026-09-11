package cn.famecode.ai.domain.agent.service.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * OpenAI 兼容接口 URL 与 Path 智能规范化器
 * 统一处理 baseUrl 与 completionsPath，防止 Spring RestClient / WebClient 发生 404 (如 RFC 3986 相对路径替换导致丢弃 /v1)
 */
public class OpenAiUrlNormalizer {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NormalizedUrl {
        private String baseUrl;
        private String completionsPath;
    }

    /**
     * 规整 OpenAI 兼容接口的 baseUrl 与 completionsPath
     *
     * 典型场景：
     * 1. baseUrl="http://10.0.0.104:8080/v1", completionsPath="chat/completions"
     *    -> baseUrl="http://10.0.0.104:8080", completionsPath="v1/chat/completions"
     * 2. baseUrl="http://10.0.0.104:8080/v1/", completionsPath="chat/completions"
     *    -> baseUrl="http://10.0.0.104:8080", completionsPath="v1/chat/completions"
     * 3. baseUrl="http://10.0.0.104:8080/v1", completionsPath="v1/chat/completions"
     *    -> baseUrl="http://10.0.0.104:8080", completionsPath="v1/chat/completions"
     * 4. baseUrl="http://10.0.0.104:8080", completionsPath="v1/chat/completions"
     *    -> baseUrl="http://10.0.0.104:8080", completionsPath="v1/chat/completions"
     * 5. baseUrl="https://apis.itedus.cn/", completionsPath="v1/chat/completions"
     *    -> baseUrl="https://apis.itedus.cn", completionsPath="v1/chat/completions"
     */
    public static NormalizedUrl normalize(String rawBaseUrl, String rawCompletionsPath) {
        if (StringUtils.isBlank(rawBaseUrl)) {
            return new NormalizedUrl(rawBaseUrl, rawCompletionsPath);
        }

        String baseUrl = rawBaseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String completionsPath = StringUtils.isNotBlank(rawCompletionsPath) ? rawCompletionsPath.trim() : "v1/chat/completions";
        while (completionsPath.startsWith("/")) {
            completionsPath = completionsPath.substring(1);
        }

        // 识别并规整 /v1 或 /v2 版本号段
        if (baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
            if (!completionsPath.startsWith("v1/")) {
                completionsPath = "v1/" + completionsPath;
            }
        } else if (baseUrl.endsWith("/v2")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
            if (!completionsPath.startsWith("v2/")) {
                completionsPath = "v2/" + completionsPath;
            }
        } else if (!completionsPath.startsWith("v1/") && !completionsPath.startsWith("v2/")) {
            // 如果 baseUrl 既没有 /v1，completionsPath 也没有版本号，且以 chat/ 开头，补全 v1/
            if (completionsPath.startsWith("chat/")) {
                completionsPath = "v1/" + completionsPath;
            }
        }

        return new NormalizedUrl(baseUrl, completionsPath);
    }
}