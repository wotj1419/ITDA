import apiClient from './client';
import type { AuthService } from '../auth.interface';
import type { LoginRequest, SignupRequest, User, ApiResponse, LoginResponse } from '../../types';

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
        // Optional: Call logout endpoint if server requires it
        // await apiClient.post('/auth/logout');
    }
};
