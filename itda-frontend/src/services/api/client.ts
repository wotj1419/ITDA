import axios from 'axios';

// Create a configured axios instance
// Base URL for local backend
const baseURL = 'http://localhost:8080/api';

const apiClient = axios.create({
    baseURL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 60000, // 60s timeout
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

        const status = error.response?.status;

        // Handle 401 Unauthorized errors (token expired)
        if (status === 401 && !originalRequest._retry) {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            // Avoid router import cycles here
            window.location.assign('/auth');
            return Promise.reject(error);
        }

        if (status === 403) {
            const currentPath = `${window.location.pathname}${window.location.search}`;
            const redirect = encodeURIComponent(currentPath);
            window.location.assign(`/access-denied?redirect=${redirect}`);
            return Promise.reject(error);
        }

        return Promise.reject(error);
    }
);

export default apiClient;
