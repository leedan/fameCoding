package cn.famecode.ai.test.api.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Spring AI Test
 * 文档：<a href="https://docs.spring.io/spring-ai/reference/1.0/api/advisors.html">spring ai</a>
 */
import org.junit.jupiter.api.Test;

@Slf4j
public class SpringAiApiTest {

    @Test
    public void testOpenAiApiStream() {
        try {
            log.info("====== [测试: 引入 Netty 后的 Spring AI Stream] ======");

            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl("http://10.0.0.104:8080")
                    .apiKey("")
                    .build();

            OpenAiChatModel model = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder().model("Qwen3.8-27B").build())
                    .build();

            log.info("开始 Spring AI 流式调用...");
            model.stream("hi").doOnNext(chunk -> {
                log.info("收到 chunk: {}", chunk);
            }).blockLast();
            log.info("Spring AI 流式调用成功完成！");

        } catch (Exception e) {
            log.error("流式调用异常", e);
            org.junit.jupiter.api.Assertions.fail(e.getMessage());
        }
    }
}

