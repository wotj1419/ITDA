import type { AuthService } from '../auth.interface';
import type {
    LoginRequest,
    SignupRequest,
    PasswordResetRequest,
    PasswordResetConfirmRequest,
} from '../../types/api/auth';

export const mockAuthService: AuthService = {
    async login(_credentials: LoginRequest) {
        // Simulate network delay
        await new Promise((resolve) => setTimeout(resolve, 500));

        // Mock response
        const mockToken = 'mock_access_token_' + Date.now();
        const mockRefreshToken = 'mock_refresh_token_' + Date.now();
        return {
            accessToken: mockToken,
            refreshToken: mockRefreshToken,
            expiresIn: 3600,
        };
    },

    async signup(_data: SignupRequest) {
        await new Promise((resolve) => setTimeout(resolve, 500));
        // Auto login logic is usually handled by the store calling login() after this
        void _data;
    },

    async fetchMe() {
        await new Promise((resolve) => setTimeout(resolve, 300));
        return {
            id: 1,
            email: 'minjun@example.com',
            name: 'Minjun Kim',
            profileImageUrl: 'https://i.pravatar.cc/150?u=user123',
            role: 'USER',
        };
    },

    async requestPasswordReset(_data: PasswordResetRequest) {
        await new Promise((resolve) => setTimeout(resolve, 400));
        void _data;
    },

    async confirmPasswordReset(_data: PasswordResetConfirmRequest) {
        await new Promise((resolve) => setTimeout(resolve, 400));
        void _data;
    },

    logout() {
        // No-op for mock
    }
};
