/**
 * 行内代码预测补全 API
 */
import { post } from './request'

export interface InlineCompletionRequest {
  path: string
  language: string
  prefix: string
  suffix: string
  line: number
  column: number
}

export interface InlineCompletionResponse {
  completion: string
  model: string
  durationMs: number
  hasCompletion: boolean
}

/**
 * 获取行内代码预测补全（Ghost Text）
 */
export async function fetchInlineCompletion(
  req: InlineCompletionRequest
): Promise<InlineCompletionResponse | null> {
  try {
    const res = await post<InlineCompletionResponse>('/api/v1/completion/inline', req)
    if (res.code === '0000' && res.data && res.data.hasCompletion) {
      return res.data
    }
    return null
  } catch (err) {
    // 补全静默失败，避免打扰用户编码
    return null
  }
}
