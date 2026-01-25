export interface User {
  id: number;
  email: string;
  name: string;
  profileImageUrl?: string | null;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
}
