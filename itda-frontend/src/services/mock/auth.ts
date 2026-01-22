 import type { AuthService } from '../auth.interface';
import type { LoginRequest, SignupRequest, User } from '../../types';

export const mockAuthService: AuthService = {
    async login(credentials: LoginRequest) {
        // Simulate network delay
        await new Promise((resolve) => setTimeout(resolve, 500));

        // Mock response
        const mockUser: User = {
            userId: 1,
            email: credentials.email,
            name: 'Minjun Kim',
            profileImage: 'https://i.pravatar.cc/150?u=user123',
        };

        const mockToken = 'mock_access_token_' + Date.now();
        return { user: mockUser, token: mockToken };
    },

    async signup(data: SignupRequest) {
        await new Promise((resolve) => setTimeout(resolve, 500));
        // Auto login logic is usually handled by the store calling login() after this
        void data;
    },

    async fetchMe() {
        await new Promise((resolve) => setTimeout(resolve, 300));
        return {
            userId: 1,
            email: 'minjun@example.com',
            name: 'Minjun Kim',
            profileImage: 'https://i.pravatar.cc/150?u=user123',
        };
    },

    logout() {
        // No-op for mock
    }
};
