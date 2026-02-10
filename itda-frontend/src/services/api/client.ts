import axios from 'axios';
import { API_BASE_URL } from './constants';
import { getAccessToken, clearAuthTokens } from './authTokens';
import { redirectToAccessDenied, redirectToLanding } from './redirects';

// Create a configured axios instance

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: 180000, // 3m timeout
});

// Request interceptor for API calls
apiClient.interceptors.request.use(
    (config) => {
        const headers = config.headers || {};
        const isFormData =
            typeof FormData !== 'undefined' &&
            config.data instanceof FormData;
        const hasContentType = typeof (headers as { has?: (name: string) => boolean }).has === 'function'
            ? (headers as { has: (name: string) => boolean }).has('Content-Type')
            : 'Content-Type' in headers || 'content-type' in headers;

        if (isFormData) {
            if (typeof (headers as { delete?: (name: string) => void }).delete === 'function') {
                (headers as { delete: (name: string) => void }).delete('Content-Type');
            } else {
                delete (headers as Record<string, unknown>)['Content-Type'];
                delete (headers as Record<string, unknown>)['content-type'];
            }
        } else if (config.data && !hasContentType) {
            if (typeof (headers as { set?: (name: string, value: string) => void }).set === 'function') {
                (headers as { set: (name: string, value: string) => void }).set('Content-Type', 'application/json');
            } else {
                (headers as Record<string, unknown>)['Content-Type'] = 'application/json';
            }
        }

        config.headers = headers;
        const token = getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor for API calls
apiClient.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        const status = error.response?.status;

        // Handle 401 Unauthorized errors (token expired)
        // Skip for login requests, as 401 is an expected failure response there
        const isAuthLogin = originalRequest.url?.includes('/auth/login');
        const isPasswordReset =
            originalRequest.url?.includes('/auth/password/reset/request') ||
            originalRequest.url?.includes('/auth/password/reset/confirm');

        if (status === 401 && !originalRequest._retry && !isAuthLogin && !isPasswordReset) {
            clearAuthTokens();
            redirectToLanding();
            return Promise.reject(error);
        }

        if (status === 403) {
            redirectToAccessDenied();
            return Promise.reject(error);
        }

        return Promise.reject(error);
    }
);

export default apiClient;
