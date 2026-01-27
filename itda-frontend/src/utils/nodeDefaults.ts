import type { AspectRatio, CameraMotion, GridLayout } from '../types/ui/sceneNodes';

export const DEFAULT_MASTER_STYLE = '실사';
export const DEFAULT_MASTER_TIME_OF_DAY = '낮';
export const DEFAULT_MASTER_MOOD = '중립';

export const DEFAULT_GRID_LAYOUT: GridLayout = '2x2';
export const DEFAULT_GRID_SHOT_TYPES = ['와이드샷', '미디엄샷', '클로즈업', '오버숄더'];

export const DEFAULT_VIDEO_DURATION = 4;
export const DEFAULT_VIDEO_CAMERA_MOTION: CameraMotion = 'staticCamera';
export const DEFAULT_VIDEO_ASPECT_RATIO: AspectRatio = '16:9';
export const VIDEO_ASPECT_RATIO_OPTIONS: AspectRatio[] = ['16:9', '9:16'];

export function normalizeAspectRatio(value?: string | null): AspectRatio {
  if (value === '9:16') return '9:16';
  return '16:9';
}
