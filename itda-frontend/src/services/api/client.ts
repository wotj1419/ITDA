import axios from 'axios';

// Create a configured axios instance
// In Vite, use import.meta.env for environment variables
// VITE_API_BASE_URL should be defined in .env files
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

const apiClient = axios.create({
    baseURL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000, // 10s timeout
});

// Request interceptor for API calls
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
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

        // Handle 401 Unauthorized errors (token expired)
        if (error.response?.status === 401 && !originalRequest._retry) {
            // Logic for refreshing token could go here
            // For now, simpler handling or logout redirect
            // window.location.href = '/auth'; 
        }

        return Promise.reject(error);
    }
);

export default apiClient;
