import type { AuthService } from '../auth.interface';
import type {
    LoginRequest,
    SignupRequest,
    PasswordResetRequest,
    PasswordResetConfirmRequest,
    UpdateProfileRequest,
    User,
} from '../../types/api/auth';

let mockUser: User = {
    id: 1,
    email: 'minjun@example.com',
    name: 'Minjun Kim',
    profileImageUrl: 'https://i.pravatar.cc/150?u=user123',
    role: 'USER',
};

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
        return { ...mockUser };
    },

    async updateProfile(data: UpdateProfileRequest) {
        await new Promise((resolve) => setTimeout(resolve, 300));
        mockUser = {
            ...mockUser,
            ...(data.name !== undefined ? { name: data.name } : {}),
            ...(data.profileImageUrl !== undefined ? { profileImageUrl: data.profileImageUrl } : {}),
        };
        return { ...mockUser };
    },

    async uploadProfileImage(file: File) {
        await new Promise((resolve) => setTimeout(resolve, 300));
        const dataUrl = await readFileAsDataUrl(file);
        mockUser = {
            ...mockUser,
            profileImageUrl: dataUrl,
        };
        return { ...mockUser };
    },

    async removeProfileImage() {
        await new Promise((resolve) => setTimeout(resolve, 200));
        mockUser = {
            ...mockUser,
            profileImageUrl: null,
        };
        return { ...mockUser };
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

function readFileAsDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : '');
        reader.onerror = () => reject(reader.error);
        reader.readAsDataURL(file);
    });
}
