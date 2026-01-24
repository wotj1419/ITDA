import apiClient from './client';
import type { AuthService } from '../auth.interface';
import type { LoginRequest, SignupRequest, User, ApiResponse } from '../../types';

export const apiAuthService: AuthService = {
    async login(credentials: LoginRequest) {
        const response = await apiClient.post<{ user: User; token: string }>('/auth/login', credentials);
        return response.data;
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
