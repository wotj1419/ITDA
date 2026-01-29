import type { CameraMotion } from '../types/ui/sceneNodes';

const STYLE_LABEL_TO_KEY: Record<string, string> = {
  실사: 'PHOTO_REAL',
  애니메이션: 'ANIME_2D',
  픽사: 'STYLIZED_3D',
  수채화: 'WATERCOLOR_ILLUSTRATION',
  유화: 'OIL_PAINT_ILLUSTRATION',
};

const TIME_OF_DAY_LABEL_TO_KEY: Record<string, string> = {
  아침: 'DAWN',
  낮: 'DAY',
  저녁: 'DUSK',
  밤: 'NIGHT',
};

const MOOD_LABEL_TO_KEY: Record<string, string> = {
  중립: 'NEUTRAL',
  편안: 'COZY',
  고독: 'LONELY',
  긴장: 'TENSE',
  행복: 'HOPEFUL',
  우울: 'DARK',
};

const SHOT_TYPE_LABEL_TO_KEY: Record<string, string> = {
  와이드샷: 'WIDE',
  미디엄샷: 'MEDIUM',
  클로즈업: 'CLOSE_UP',
  '익스트림 클로즈업': 'EXTREME_CLOSE_UP',
  오버숄더: 'OTS',
  POV: 'POV',
};

const EXPRESSION_LABEL_TO_KEY: Record<string, string> = {
  기본: 'NEUTRAL',
  미소: 'SMILE',
  슬픔: 'SAD',
  놀람: 'SURPRISED',
  분노: 'ANGRY',
  무표정: 'BLANK',
};

const CAMERA_MOTION_VALUE_TO_KEY: Record<CameraMotion, string> = {
  lowZoomIn: 'SLOW_ZOOM_IN',
  zoomOut: 'ZOOM_OUT',
  panLeftToRight: 'PAN_LR',
  tiltUp: 'TILT_UP',
  staticCamera: 'STATIC',
};

const CAMERA_MOTION_KEY_TO_VALUE: Record<string, CameraMotion> = {
  STATIC: 'staticCamera',
  SLOW_ZOOM_IN: 'lowZoomIn',
  ZOOM_OUT: 'zoomOut',
  PAN_LR: 'panLeftToRight',
  TILT_UP: 'tiltUp',
};

const CAMERA_MOTION_LEGACY_TO_VALUE: Record<string, CameraMotion> = {
  zoomIn: 'lowZoomIn',
  zoomOut: 'zoomOut',
  panLeft: 'panLeftToRight',
  panRight: 'panLeftToRight',
  tiltUp: 'tiltUp',
  tiltDown: 'tiltUp',
  static: 'staticCamera',
};

function invertMap(map: Record<string, string>): Record<string, string> {
  return Object.entries(map).reduce<Record<string, string>>((acc, [label, key]) => {
    acc[key] = label;
    return acc;
  }, {});
}

const STYLE_KEY_TO_LABEL = invertMap(STYLE_LABEL_TO_KEY);
const TIME_OF_DAY_KEY_TO_LABEL = invertMap(TIME_OF_DAY_LABEL_TO_KEY);
const MOOD_KEY_TO_LABEL = invertMap(MOOD_LABEL_TO_KEY);
const SHOT_TYPE_KEY_TO_LABEL = invertMap(SHOT_TYPE_LABEL_TO_KEY);
const EXPRESSION_KEY_TO_LABEL = invertMap(EXPRESSION_LABEL_TO_KEY);

function resolveKey(
  value: string | null | undefined,
  labelToKey: Record<string, string>,
  keyToLabel: Record<string, string>
): string | undefined {
  if (!value) return undefined;
  if (labelToKey[value]) return labelToKey[value];
  if (keyToLabel[value]) return value;
  return value;
}

function resolveLabel(
  value: string | null | undefined,
  keyToLabel: Record<string, string>
): string {
  if (!value) return '';
  return keyToLabel[value] ?? value;
}

export function resolveStyleKey(value?: string | null): string | undefined {
  return resolveKey(value, STYLE_LABEL_TO_KEY, STYLE_KEY_TO_LABEL);
}

export function resolveTimeOfDayKey(value?: string | null): string | undefined {
  return resolveKey(value, TIME_OF_DAY_LABEL_TO_KEY, TIME_OF_DAY_KEY_TO_LABEL);
}

export function resolveMoodKey(value?: string | null): string | undefined {
  return resolveKey(value, MOOD_LABEL_TO_KEY, MOOD_KEY_TO_LABEL);
}

export function resolveShotTypeKey(value?: string | null): string | undefined {
  return resolveKey(value, SHOT_TYPE_LABEL_TO_KEY, SHOT_TYPE_KEY_TO_LABEL);
}

export function resolveExpressionKey(value?: string | null): string | undefined {
  return resolveKey(value, EXPRESSION_LABEL_TO_KEY, EXPRESSION_KEY_TO_LABEL);
}

export function resolveStyleLabel(value?: string | null): string {
  return resolveLabel(value, STYLE_KEY_TO_LABEL);
}

export function resolveTimeOfDayLabel(value?: string | null): string {
  return resolveLabel(value, TIME_OF_DAY_KEY_TO_LABEL);
}

export function resolveMoodLabel(value?: string | null): string {
  return resolveLabel(value, MOOD_KEY_TO_LABEL);
}

export function resolveShotTypeLabel(value?: string | null): string {
  return resolveLabel(value, SHOT_TYPE_KEY_TO_LABEL);
}

export function resolveExpressionLabel(value?: string | null): string {
  return resolveLabel(value, EXPRESSION_KEY_TO_LABEL);
}

export function mapShotTypeLabelsToKeys(values?: string[] | null): string[] {
  if (!values) return [];
  return values
    .map((value) => resolveShotTypeKey(value))
    .filter((value): value is string => Boolean(value));
}

export function mapShotTypeKeysToLabels(values?: string[] | null): string[] {
  if (!values) return [];
  return values.map((value) => resolveShotTypeLabel(value)).filter(Boolean);
}

export function resolveCameraMotionKey(value?: string | null): string | undefined {
  if (!value) return undefined;
  if (CAMERA_MOTION_VALUE_TO_KEY[value as CameraMotion]) {
    return CAMERA_MOTION_VALUE_TO_KEY[value as CameraMotion];
  }
  if (CAMERA_MOTION_KEY_TO_VALUE[value]) return value;
  const legacyValue = CAMERA_MOTION_LEGACY_TO_VALUE[value];
  if (legacyValue) return CAMERA_MOTION_VALUE_TO_KEY[legacyValue];
  return value;
}

export function normalizeCameraMotionValue(value?: string | null): CameraMotion {
  if (!value) return 'staticCamera';
  if (CAMERA_MOTION_KEY_TO_VALUE[value]) return CAMERA_MOTION_KEY_TO_VALUE[value];
  if (CAMERA_MOTION_VALUE_TO_KEY[value as CameraMotion]) return value as CameraMotion;
  return CAMERA_MOTION_LEGACY_TO_VALUE[value] ?? 'staticCamera';
}
