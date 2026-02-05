import type {
    LoginRequest,
    SignupRequest,
    User,
    LoginResponse,
    PasswordResetRequest,
    PasswordResetConfirmRequest,
} from '../types/api/auth';

export interface AuthService {
    login(credentials: LoginRequest): Promise<LoginResponse>;
    signup(data: SignupRequest): Promise<void>;
    fetchMe(): Promise<User>;
    requestPasswordReset(data: PasswordResetRequest): Promise<void>;
    confirmPasswordReset(data: PasswordResetConfirmRequest): Promise<void>;
    logout(): void;
}
