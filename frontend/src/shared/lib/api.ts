import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { API_ORIGIN, apiUrl } from '@/shared/lib/runtime';

const api = axios.create({
    baseURL: API_ORIGIN,
    headers: {
        'Content-Type': 'application/json',
    },
});

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
            if (typeof (config.headers as { delete?: (name: string) => void }).delete === 'function') {
                (config.headers as { delete: (name: string) => void }).delete('Content-Type');
            } else {
                delete (config.headers as Record<string, unknown>)['Content-Type'];
            }
        }
        return config;
    },
    (error) => Promise.reject(error)
);

let isRefreshing = false;
let pendingQueue: Array<{
    resolve: (token: string) => void;
    reject: (err: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
    pendingQueue.forEach((p) => {
        if (error) p.reject(error);
        else if (token) p.resolve(token);
    });
    pendingQueue = [];
}

api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

        if (!error.response || error.response.status !== 401 || !originalRequest || originalRequest._retry) {
            if (error.response?.status === 401) {
                // refresh тоже провалился или запрос без возможности retry
                const url = originalRequest?.url || '';
                if (url.includes('/auth/refresh') || url.includes('/auth/login')) {
                    clearAuthAndRedirect();
                }
            }
            return Promise.reject(error);
        }

        // Не пытаемся рефрешить сам refresh/login
        const url = originalRequest.url || '';
        if (url.includes('/auth/refresh') || url.includes('/auth/login')) {
            clearAuthAndRedirect();
            return Promise.reject(error);
        }

        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            clearAuthAndRedirect();
            return Promise.reject(error);
        }

        if (isRefreshing) {
            return new Promise((resolve, reject) => {
                pendingQueue.push({
                    resolve: (token: string) => {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        resolve(api(originalRequest));
                    },
                    reject,
                });
            });
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
            const res = await axios.post(
                `${apiUrl('/api/v1/social/auth/refresh')}`,
                { refreshToken },
                { headers: { 'Content-Type': 'application/json' } }
            );
            const newAccess = res.data.token as string;
            const newRefresh = res.data.refreshToken as string;
            localStorage.setItem('token', newAccess);
            if (newRefresh) {
                localStorage.setItem('refreshToken', newRefresh);
            }
            processQueue(null, newAccess);
            originalRequest.headers.Authorization = `Bearer ${newAccess}`;
            return api(originalRequest);
        } catch (refreshError) {
            processQueue(refreshError, null);
            clearAuthAndRedirect();
            return Promise.reject(refreshError);
        } finally {
            isRefreshing = false;
        }
    }
);

function clearAuthAndRedirect() {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');
    if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
    }
}

export function logOutUser() {
    const refreshToken = localStorage.getItem('refreshToken');
    // Сразу чистим storage, чтобы refresh не остался даже при сбое запроса/редиректе
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');

    const redirect = () => {
        if (!window.location.pathname.includes('/login')) {
            window.location.href = '/login';
        }
    };

    if (refreshToken) {
        axios
            .post(apiUrl('/api/v1/social/auth/logout'), { refreshToken })
            .catch(() => undefined)
            .finally(redirect);
    } else {
        redirect();
    }
}

export const notificationsApi = {
    getNotifications: (page = 0, size = 10) =>
        api.get(`/api/v1/social/notifications?page=${page}&size=${size}`),
    getUnreadCount: () =>
        api.get('/api/v1/social/notifications/unread-count'),
    markAsRead: (notificationId: number) =>
        api.patch(`/api/v1/social/notifications/${notificationId}/read`),
    markAllAsRead: () =>
        api.patch('/api/v1/social/notifications/read-all'),
    markGroupAsRead: (notificationIds: number[]) =>
        api.patch('/api/v1/social/notifications/mark-group-read', notificationIds),
};

export async function uploadEventPhoto(file: File): Promise<string> {
    const form = new FormData();
    form.append('file', file);
    const res = await api.post('/api/v1/social/events/photos', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.url as string;
}

export async function uploadChatPhoto(file: File): Promise<string> {
    const form = new FormData();
    form.append('file', file);
    const res = await api.post('/api/v1/social/chats/photos', form);
    return res.data.url as string;
}

export async function uploadPostPhoto(file: File): Promise<string> {
    const form = new FormData();
    form.append('file', file);
    const res = await api.post('/api/v1/social/posts/photos', form);
    return res.data.url as string;
}

export async function uploadChatFile(file: File): Promise<{ url: string; name: string; size: number; mimeType: string }> {
    const form = new FormData();
    form.append('file', file);
    const res = await api.post('/api/v1/social/chats/files', form);
    return {
        url: res.data.url as string,
        name: (res.data.name as string) || file.name,
        size: Number(res.data.size) || file.size,
        mimeType: (res.data.mimeType as string) || file.type,
    };
}

export async function uploadChatVoice(file: Blob): Promise<string> {
    const form = new FormData();
    const ext = file.type.includes('ogg') ? 'ogg' : file.type.includes('mp4') ? 'm4a' : 'webm';
    form.append('file', file, `voice.${ext}`);
    const res = await api.post('/api/v1/social/chats/voice', form);
    return res.data.url as string;
}

export default api;
