import apiClient from './client';
import type { AuthService } from '../auth.interface';
import type { LoginRequest, SignupRequest, User } from '../../types';

export const apiAuthService: AuthService = {
    async login(credentials: LoginRequest) {
        const response = await apiClient.post<{ user: User; token: string }>('/auth/login', credentials);
        return response.data;
    },

    async signup(data: SignupRequest) {
        await apiClient.post('/auth/signup', data);
    },

    async fetchMe() {
        const response = await apiClient.get<User>('/users/me');
        return response.data;
    },

    logout() {
        // Optional: Call logout endpoint if server requires it
        // await apiClient.post('/auth/logout');
    }
};
