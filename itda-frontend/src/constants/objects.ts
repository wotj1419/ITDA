import type { ObjectType } from '../types/api/objects'

export const OBJECT_STYLE_OPTIONS = ['실사', '만화', '애니메이션', '사이버펑크']

export const OBJECT_TYPE_OPTIONS: Array<{ label: string; value: ObjectType }> = [
  { label: '캐릭터', value: 'CHARACTER' },
  { label: '소품', value: 'PROP' },
  { label: '기타', value: 'ETC' },
]

export const OBJECT_TYPE_LABELS: Record<ObjectType, string> = {
  CHARACTER: '캐릭터',
  PROP: '소품',
  ETC: '기타',
}
