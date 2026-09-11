/**
 * Monaco Editor 行内幽灵代码预测补全 Provider (Ghost Text)
 *
 * 对应模式三 通道 A：低延迟代码补全
 */
import { fetchInlineCompletion } from '../api/completion'

let providerDisposable: { dispose: () => void } | null = null
let debounceTimeout: any = null

/**
 * 注册 Monaco 全局行内代码补全提供者
 */
export function registerMonacoInlineCompletion(monaco: any): void {
  if (!monaco || providerDisposable) {
    return
  }

  try {
    providerDisposable = monaco.languages.registerInlineCompletionsProvider(
      { pattern: '**' },
      {
        provideInlineCompletions: async (
          model: any,
          position: any,
          _context: any,
          token: any
        ) => {
          // 如果当前处于只读模式或已被取消，直接返回
          if (token.isCancellationRequested) {
            return { items: [] }
          }

          // 防抖 180ms，避免连续输入时产生请求风暴
          if (debounceTimeout) {
            clearTimeout(debounceTimeout)
          }

          await new Promise<void>((resolve) => {
            debounceTimeout = setTimeout(() => resolve(), 180)
          })

          if (token.isCancellationRequested) {
            return { items: [] }
          }

          try {
            const fullContent = model.getValue()
            if (!fullContent || fullContent.trim().length === 0) {
              return { items: [] }
            }

            const currentLine = position.lineNumber
            const currentColumn = position.column
            const totalLines = model.getLineCount()

            // 截取光标前 60 行作为 Prefix
            const startLine = Math.max(1, currentLine - 60)
            const prefix = model.getValueInRange({
              startLineNumber: startLine,
              startColumn: 1,
              endLineNumber: currentLine,
              endColumn: currentColumn,
            })

            // 截取光标后 30 行作为 Suffix
            const endLine = Math.min(totalLines, currentLine + 30)
            const suffix = model.getValueInRange({
              startLineNumber: currentLine,
              startColumn: currentColumn,
              endLineNumber: endLine,
              endColumn: model.getLineMaxColumn(endLine),
            })

            // 如果当前行光标前全是空，且无有效上下文，不滥发请求
            if (!prefix.trim()) {
              return { items: [] }
            }

            const language = model.getLanguageId() || 'plaintext'
            const uri = model.uri ? model.uri.toString() : 'file'
            const path = uri.replace(/^inmemory:\/\/model\//, '')

            const result = await fetchInlineCompletion({
              path,
              language,
              prefix,
              suffix,
              line: currentLine,
              column: currentColumn,
            })

            if (!result || !result.hasCompletion || !result.completion) {
              return { items: [] }
            }

            if (token.isCancellationRequested) {
              return { items: [] }
            }

            return {
              items: [
                {
                  insertText: result.completion,
                  range: new monaco.Range(
                    currentLine,
                    currentColumn,
                    currentLine,
                    currentColumn
                  ),
                },
              ],
            }
          } catch (err) {
            return { items: [] }
          }
        },
        freeInlineCompletions: () => {},
      }
    )
  } catch (err) {
    console.warn('[InlineCompletion] 注册 Monaco Provider 失败', err)
  }
}

/**
 * 销毁补全 Provider
 */
export function disposeMonacoInlineCompletion(): void {
  if (providerDisposable) {
    providerDisposable.dispose()
    providerDisposable = null
  }
  if (debounceTimeout) {
    clearTimeout(debounceTimeout)
    debounceTimeout = null
  }
}
