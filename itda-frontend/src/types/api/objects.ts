export type ObjectType = 'CHARACTER' | 'PROP' | 'ETC';

export interface ObjectSheet {
  objectId: number;
  name: string;
  type: ObjectType;
  description: string;
  style?: string;
  sheetImageUrl?: string;
}

export interface CreateObjectRequest {
  name: string;
  type: ObjectType;
  description: string;
  style?: string;
}
