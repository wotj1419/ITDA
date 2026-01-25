import apiClient from './client';
import type { AuthService } from '../auth.interface';
import type { ApiResponse } from '../../types/api/common';
import type { LoginRequest, SignupRequest, User, LoginResponse } from '../../types/api/auth';

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

    logout() {
        // 선택 사항: 서버 로그아웃이 필요한 경우 호출
        // await apiClient.post('/auth/logout');
    }
};
