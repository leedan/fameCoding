package cn.famecode.ai.domain.agent.service.armory.node;

import cn.famecode.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.famecode.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.famecode.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.famecode.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.famecode.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.annotation.Resource;
import java.time.Duration;

@Slf4j
@Service
public class AiApiNode extends AbstractArmorySupport {

    @Resource
    private ChatModelNode chatModelNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AiApiNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.AiApi aiApiConfig = aiAgentConfigTableVO.getModule().getAiApi();

        int connectTimeoutMs = aiApiConfig.getConnectTimeoutMs() != null ? aiApiConfig.getConnectTimeoutMs() : 10_000;
        int readTimeoutMs = aiApiConfig.getReadTimeoutMs() != null ? aiApiConfig.getReadTimeoutMs() : 120_000;

        log.info("Ai API 超时配置: connect={}ms, read={}ms", connectTimeoutMs, readTimeoutMs);

        // 构建带超时的 RestClient
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory);

        cn.famecode.ai.domain.agent.service.util.OpenAiUrlNormalizer.NormalizedUrl normalized =
                cn.famecode.ai.domain.agent.service.util.OpenAiUrlNormalizer.normalize(
                        aiApiConfig.getBaseUrl(),
                        StringUtils.isNotBlank(aiApiConfig.getCompletionsPath()) ? aiApiConfig.getCompletionsPath() : "v1/chat/completions"
                );

        log.info("OpenAiApi 节点装配: rawBaseUrl={}, normalizedBaseUrl={}, completionsPath={}",
                aiApiConfig.getBaseUrl(), normalized.getBaseUrl(), normalized.getCompletionsPath());

        // 适配内网免密接口：当 key 为 EMPTY/none 时传入空字符串，Spring AI 检测无 text 则不发送 Authorization 头，匹配原生 curl
        String rawApiKey = aiApiConfig.getApiKey();
        boolean isAnonymous = StringUtils.isBlank(rawApiKey) || "EMPTY".equalsIgnoreCase(rawApiKey) || "none".equalsIgnoreCase(rawApiKey);
        String effectiveApiKey = isAnonymous ? "" : rawApiKey;

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalized.getBaseUrl())
                .apiKey(effectiveApiKey)
                .completionsPath(normalized.getCompletionsPath())
                .embeddingsPath(StringUtils.isNotBlank(aiApiConfig.getEmbeddingsPath()) ? aiApiConfig.getEmbeddingsPath() : "v1/embeddings")
                .restClientBuilder(restClientBuilder)
                .build();

        dynamicContext.setOpenAiApi(openAiApi);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return chatModelNode;
    }

}