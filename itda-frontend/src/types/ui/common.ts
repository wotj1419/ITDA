export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info' | 'progress';
  title: string;
  message?: string;
  meta?: string;
  duration?: number;
  position?: 'top-right' | 'bottom-right';
  autoClose?: boolean;
}

export interface ModalConfig {
  id: string;
  isOpen: boolean;
  data?: unknown;
}
