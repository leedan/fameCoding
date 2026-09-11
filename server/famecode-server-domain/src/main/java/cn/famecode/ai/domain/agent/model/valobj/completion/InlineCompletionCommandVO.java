package cn.famecode.ai.domain.agent.model.valobj.completion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 行内代码补全命令值对象（Domain 层）
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InlineCompletionCommandVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件路径 */
    private String path;

    /** 编程语言 */
    private String language;

    /** 光标前代码上下文 */
    private String prefix;

    /** 光标后代码上下文 */
    private String suffix;

    /** 光标行 */
    private Integer line;

    /** 光标列 */
    private Integer column;

}