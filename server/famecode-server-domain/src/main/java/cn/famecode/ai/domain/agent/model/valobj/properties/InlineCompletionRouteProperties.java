package cn.famecode.ai.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模式三：行内代码极速补全模型路由配置
 *
 * <p>所有模型配置项完全由配置文件或当前已启用的 Agent 动态注入，禁止在类中硬编码任何默认地址或秘钥。
 *
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.route.inline-completion", ignoreInvalidFields = true)
public class InlineCompletionRouteProperties {

    /** 是否启用行内代码预测补全（未配置时默认继承主 Agent 或启用） */
    private Boolean enabled;

    /**
     * API 服务地址（可选覆盖项。若为空，自动继承当前启用的 Agent 配置）
     */
    private String baseUrl;

    /** API Key（可选覆盖项。若为空，自动继承当前启用的 Agent 配置） */
    private String apiKey;

    /** 补全专用模型名称（可选覆盖项。若为空，自动继承当前启用的 Agent 配置） */
    private String model;

    /** 补全最大输出 Token 数，代码补全建议单次在 64 ~ 256 之间 */
    private Integer maxTokens;

    /** 采样温度，代码补全需要高精确度，建议 0.0 ~ 0.2 */
    private Double temperature;

    /** 请求超时时间（毫秒），建议在 2000 ~ 5000ms */
    private Integer timeoutMs;

    /** completions 路径（可选覆盖项） */
    private String completionsPath;

}