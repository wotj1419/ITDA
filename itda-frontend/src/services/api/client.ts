import axios from 'axios';
import { API_BASE_URL } from './constants';
import { getAccessToken, clearAuthTokens } from './authTokens';
import { redirectToAccessDenied, redirectToAuth } from './redirects';

// Create a configured axios instance

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 60000, // 60s timeout
});

// Request interceptor for API calls
apiClient.interceptors.request.use(
    (config) => {
        const token = getAccessToken();
        if (token) {
            config.headers = config.headers || {};
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
        if (status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/login')) {
            clearAuthTokens();
            redirectToAuth();
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
