import apiClient from './client';
import type { AuthService } from '../auth.interface';
import type { ApiResponse } from '../../types/api/common';
import type {
    LoginRequest,
    SignupRequest,
    User,
    LoginResponse,
    PasswordResetRequest,
    PasswordResetConfirmRequest,
    UpdateProfileRequest,
} from '../../types/api/auth';

export const apiAuthService: AuthService = {
    async login(credentials: LoginRequest) {
        const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', credentials);
        if (!response.data.data) {
            throw new Error('Failed to login');
        }
        return response.data.data;
    },

    async signup(data: SignupRequest) {
        await apiClient.post('/auth/signup', data);
    },

    async fetchMe() {
        const response = await apiClient.get<ApiResponse<User>>('/auth/me');
        return response.data.data!;
    },

    async updateProfile(data: UpdateProfileRequest) {
        const response = await apiClient.put<ApiResponse<User>>('/auth/me', data);
        return response.data.data!;
    },

    async uploadProfileImage(file: File) {
        const formData = new FormData();
        formData.append('file', file);
        const response = await apiClient.patch<ApiResponse<User>>('/auth/me/profile-image', formData);
        return response.data.data!;
    },

    async requestPasswordReset(data: PasswordResetRequest) {
        await apiClient.post('/auth/password/reset/request', data);
    },

    async confirmPasswordReset(data: PasswordResetConfirmRequest) {
        await apiClient.post('/auth/password/reset/confirm', data);
    },

    logout() {
        // 선택 사항: 서버 로그아웃이 필요한 경우 호출
        // await apiClient.post('/auth/logout');
    }
};
