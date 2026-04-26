export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface TaskEventMessage {
  id: string;
  time: string;
  status?: string;
  message: string;
  payload?: unknown;
}
