import { apiAuthService } from './api/auth';
import { mockAuthService } from './mock/auth';
import { apiAiService } from './api/ai';
import { mockAiService } from './mock/ai';

// Toggle between mock and real API
// In Vite, environment variables are accessed via import.meta.env
// VITE_USE_MOCK can be set in .env files
const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export const authService = useMock ? mockAuthService : apiAuthService;

// AI Service
export const aiService = useMock ? mockAiService : apiAiService;

// Export other services as we implement them
// export const projectService = useMock ? mockProjectService : apiProjectService;
