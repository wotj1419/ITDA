import type { LoginRequest, SignupRequest, User, LoginResponse } from '../types';

export interface AuthService {
    login(credentials: LoginRequest): Promise<LoginResponse>;
    signup(data: SignupRequest): Promise<void>;
    fetchMe(): Promise<User>;
    logout(): void;
}
