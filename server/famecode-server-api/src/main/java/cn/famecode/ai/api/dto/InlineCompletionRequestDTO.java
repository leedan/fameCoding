package cn.famecode.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 行内代码补全请求 DTO
 *
 * <p>专用于 IDE 编辑器中实时键入时的低延迟代码预测补全（Ghost Text）。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InlineCompletionRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件相对或绝对路径，如 "src/main/java/cn/bugstack/UserService.java" */
    private String path;

    /** 编程语言标识，如 "java", "typescript", "python", "go" */
    private String language;

    /** 光标前的代码上下文（截取前 50~100 行） */
    private String prefix;

    /** 光标后的代码上下文（截取后 30~50 行） */
    private String suffix;

    /** 当前光标所在行号（1-based） */
    private Integer line;

    /** 当前光标所在列号（1-based） */
    private Integer column;

    /** 工程上下文（可选） */
    private ProjectContextDTO projectContext;

}