import type { LoginRequest, SignupRequest, User, LoginResponse } from '../types/api/auth';

export interface AuthService {
    login(credentials: LoginRequest): Promise<LoginResponse>;
    signup(data: SignupRequest): Promise<void>;
    fetchMe(): Promise<User>;
    logout(): void;
}
