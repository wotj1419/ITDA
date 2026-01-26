export type ShotTypeKey = 'WIDE' | 'MEDIUM' | 'CLOSE_UP' | 'OTS';

type ShotTypeOption = {
  key: ShotTypeKey;
  label: string;
  description: string;
};

export const SHOT_TYPE_OPTIONS: ShotTypeOption[] = [
  {
    key: 'WIDE',
    label: '와이드샷',
    description: '넓은 배경과 인물 전체를 함께 담아 분위기를 보여줍니다.',
  },
  {
    key: 'MEDIUM',
    label: '미디엄샷',
    description: '인물의 상반신 위주로 감정과 제스처를 전달합니다.',
  },
  {
    key: 'CLOSE_UP',
    label: '클로즈업',
    description: '얼굴이나 중요한 디테일을 강조해 감정을 집중시킵니다.',
  },
  {
    key: 'OTS',
    label: '오버숄더',
    description: '상대와의 대화를 어깨 너머 시점으로 보여줍니다.',
  },
];

const SHOT_TYPE_KEYS = new Set<ShotTypeKey>(['WIDE', 'MEDIUM', 'CLOSE_UP', 'OTS']);

const LABEL_TO_KEY = new Map<string, ShotTypeKey>([
  ['와이드샷', 'WIDE'],
  ['미디엄샷', 'MEDIUM'],
  ['클로즈업', 'CLOSE_UP'],
  ['익스트림 클로즈업', 'CLOSE_UP'],
  ['오버숄더', 'OTS'],
  ['OTS', 'OTS'],
  ['POV', 'WIDE'],
  ['투샷', 'MEDIUM'],
]);

const KEY_TO_LABEL = new Map<ShotTypeKey, string>(
  SHOT_TYPE_OPTIONS.map((option) => [option.key, option.label])
);

export const DEFAULT_SHOT_TYPES: ShotTypeKey[] = ['WIDE', 'MEDIUM', 'CLOSE_UP', 'OTS'];

export function normalizeShotTypeKey(value?: string | null): ShotTypeKey | null {
  if (!value) return null;
  const trimmed = value.trim();
  if (!trimmed) return null;
  const upper = trimmed.toUpperCase();
  if (SHOT_TYPE_KEYS.has(upper as ShotTypeKey)) {
    return upper as ShotTypeKey;
  }
  const mapped = LABEL_TO_KEY.get(trimmed) || LABEL_TO_KEY.get(upper);
  return mapped ?? null;
}

export function normalizeShotTypeKeys(values: string[]): ShotTypeKey[] {
  const normalized = values
    .map((value) => normalizeShotTypeKey(value))
    .filter(Boolean) as ShotTypeKey[];
  return Array.from(new Set(normalized));
}

export function shotTypeKeyToLabel(value: string): string {
  const key = normalizeShotTypeKey(value);
  if (key && KEY_TO_LABEL.has(key)) return KEY_TO_LABEL.get(key) as string;
  return value;
}
