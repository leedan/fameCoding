package cn.famecode.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 行内代码补全响应 DTO
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InlineCompletionResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 补全生成的代码文本（插入在光标处）
     */
    private String completion;

    /**
     * 生效的模型名称（如 qwen2.5-coder-7b）
     */
    private String model;

    /**
     * 模型推理及处理总耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 是否存在有效补全建议
     */
    private boolean hasCompletion;

}