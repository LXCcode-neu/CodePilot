/** 通用 API 响应包装类型 */
export interface Result<T> {
  /** 响应状态码 */
  code: number;
  /** 响应消息 */
  message: string;
  /** 响应数据 */
  data: T;
}

/** 任务事件消息，用于 SSE（Server-Sent Events）实时推送 */
export interface TaskEventMessage {
  /** 事件唯一标识 */
  id: string;
  /** 事件发生时间 */
  time: string;
  /** 任务状态 */
  status?: string;
  /** 任务阶段 */
  phase?: string;
  /** 步骤类型 */
  stepType?: string;
  /** 事件消息内容 */
  message: string;
  /** 事件附加数据 */
  payload?: Record<string, unknown>;
}
