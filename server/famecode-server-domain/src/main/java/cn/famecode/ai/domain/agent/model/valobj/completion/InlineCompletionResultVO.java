package cn.famecode.ai.domain.agent.model.valobj.completion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 行内代码补全结果值对象（Domain 层）
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InlineCompletionResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 建议代码 */
    private String completion;

    /** 命中模型 */
    private String model;

    /** 耗时毫秒 */
    private Long durationMs;

    /** 是否有结果 */
    private boolean hasCompletion;

}