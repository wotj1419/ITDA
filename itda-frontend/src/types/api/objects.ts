export type ObjectType = 'CHARACTER' | 'PROP' | 'ETC';
export type ObjectStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export interface ObjectSheet {
  objectId: number;
  name: string;
  type: ObjectType;
  description: string;
  style?: string;
  sheetImageUrl?: string;
  status?: ObjectStatus;
}

export interface CreateObjectRequest {
  name: string;
  type: ObjectType;
  description: string;
  style?: string;
}

export interface UpdateObjectRequest {
  name?: string;
  type?: ObjectType;
  description?: string;
  style?: string;
}
