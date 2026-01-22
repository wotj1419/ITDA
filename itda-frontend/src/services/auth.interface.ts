import type { LoginRequest, SignupRequest, User } from '../types';

export interface AuthService {
    login(credentials: LoginRequest): Promise<{ user: User; token: string }>;
    signup(data: SignupRequest): Promise<void>;
    fetchMe(): Promise<User>;
    logout(): void;
}
