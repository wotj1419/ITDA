export interface ApiResponse<T = unknown> {
  code: string;
  message?: string;
  data?: T;
  details?: Record<string, unknown>;
}
