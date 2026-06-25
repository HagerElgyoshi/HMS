import axios, { AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios';

// API base URL is provided exclusively by the environment (VITE_API_BASE_URL),
// centralized in .env.production. No hardcoded fallback — a missing value is a
// configuration error that should surface immediately rather than silently
// defaulting to localhost.
const BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Helpers ───────────────────────────────────────────────────────────────────

function clearAuthAndRedirect() {
  localStorage.removeItem('hms_token');
  localStorage.removeItem('hms_refresh_token');
  localStorage.removeItem('hms_user');
  window.location.href = '/login';
}

// ── Request interceptor: attach Bearer token ──────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('hms_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// ── Response interceptor: handle 401 → try refresh → retry → logout ──────────

// Flag to prevent multiple concurrent refresh attempts
let isRefreshing = false;

// Queue of requests waiting for the refresh to complete
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((item) => {
    if (error) {
      item.reject(error);
    } else {
      item.resolve(token!);
    }
  });
  failedQueue = [];
}

api.interceptors.response.use(
  (response: AxiosResponse) => response,

  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

    // ── 401 handling ─────────────────────────────────────────────────────────
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('hms_refresh_token');

      // No refresh token stored → logout immediately
      if (!refreshToken) {
        clearAuthAndRedirect();
        return Promise.reject(error);
      }

      // If a refresh is already in-flight, queue this request until it resolves
      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            originalRequest.headers = {
              ...(originalRequest.headers ?? {}),
              Authorization: `Bearer ${newToken}`,
            };
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      // Mark as retried so we don't enter an infinite loop
      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Call refresh endpoint (public — no auth header needed)
        const { data } = await axios.post(
          `${BASE_URL}/api/auth/refresh-token`,
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } },
        );

        // Backend returns { accessToken, refreshToken, type }
        const newAccessToken: string = data.accessToken;
        const newRefreshToken: string = data.refreshToken;

        // Persist new tokens
        localStorage.setItem('hms_token', newAccessToken);
        localStorage.setItem('hms_refresh_token', newRefreshToken);

        // Update default header for future requests
        api.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`;

        // Resolve all queued requests with the new token
        processQueue(null, newAccessToken);

        // Retry the original request with the new token
        originalRequest.headers = {
          ...(originalRequest.headers ?? {}),
          Authorization: `Bearer ${newAccessToken}`,
        };
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed (expired, invalid, revoked) → full logout
        processQueue(refreshError, null);
        clearAuthAndRedirect();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // ── Normalise error message for all other errors ───────────────────────
    const data = error.response?.data as Record<string, unknown> | undefined;
    const message =
      (data?.detail as string) ||
      (data?.message as string) ||
      (data?.error as string) ||
      error.message ||
      'An unexpected error occurred';

    return Promise.reject(new Error(message));
  },
);

export default api;
