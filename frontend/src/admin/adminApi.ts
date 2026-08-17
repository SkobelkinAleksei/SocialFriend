import api from '@/shared/lib/api';

export type AdminReport = {
    id: number;
    createdAt: string;
    category: 'POST' | 'COMMENT' | 'PHOTO' | 'EVENT' | 'MESSAGE' | 'CHAT';
    reason: string;
    status: string;
    reporterId: number;
    reporterName: string;
    accusedId: number;
    accusedName: string;
    accusedStatus: string;
    accusedReportsTotal: number;
    accusedReportsUpheld: number;
    targetId: number;
    roomId?: number | null;
    targetTitle?: string;
    snapshotText?: string;
    details?: string;
};

export type AdminReportPage = {
    items: AdminReport[];
    total: number;
    page: number;
    size: number;
    openCount: number;
    upheldCount: number;
    warnedCount: number;
};

export type AdminPerson = {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    accountStatus: string;
    platformRole: string;
    reportsTotal: number;
    reportsUpheld: number;
};

export type AdminPersonPage = {
    items: AdminPerson[];
    total: number;
    page: number;
    size: number;
};

export const adminApi = {
    reports: (category = 'ALL', page = 0) =>
        api.get<AdminReportPage>('/api/v1/social/admin/reports', { params: { category, page, size: 20 } }),
    report: (id: number) => api.get<AdminReport>(`/api/v1/social/admin/reports/${id}`),
    dismiss: (id: number) => api.post<AdminReport>(`/api/v1/social/admin/reports/${id}/dismiss`),
    deleteTarget: (id: number) => api.post<AdminReport>(`/api/v1/social/admin/reports/${id}/delete-target`),
    banFromReport: (id: number) => api.post<AdminReport>(`/api/v1/social/admin/reports/${id}/ban`),
    people: (q = '', page = 0) =>
        api.get<AdminPersonPage>('/api/v1/social/admin/users', { params: { q, page, size: 20 } }),
    person: (id: number) => api.get<AdminPerson>(`/api/v1/social/admin/users/${id}`),
    personReports: (id: number) => api.get<AdminReport[]>(`/api/v1/social/admin/users/${id}/reports`),
    banUser: (id: number) => api.post(`/api/v1/social/admin/users/${id}/ban`),
    unbanUser: (id: number) => api.post(`/api/v1/social/admin/users/${id}/unban`),
    previewPost: (id: number) => api.get(`/api/v1/social/admin/posts/${id}`),
    previewEvent: (id: number) => api.get(`/api/v1/social/admin/events/${id}`),
    previewChat: (id: number) => api.get(`/api/v1/social/admin/chats/messages/${id}`),
    previewComment: (id: number) => api.get(`/api/v1/social/admin/comments/${id}`),
    staff: () => api.get<AdminPerson[]>('/api/v1/social/admin/staff'),
    createAdmin: (payload: {
        userId?: number;
        firstName?: string;
        lastName?: string;
        email?: string;
        numberPhone?: string;
        password?: string;
    }) => api.post<AdminPerson>('/api/v1/social/admin/staff', payload),
    revokeAdmin: (id: number) => api.delete(`/api/v1/social/admin/staff/${id}`),
};

export function categoryLabel(category?: string) {
    if (category === 'POST') return 'Пост';
    if (category === 'COMMENT') return 'Комментарий';
    if (category === 'PHOTO') return 'Фото';
    if (category === 'EVENT') return 'Событие';
    if (category === 'MESSAGE') return 'Сообщение';
    if (category === 'CHAT') return 'Чат';
    return category || '';
}

export function reasonLabel(reason?: string) {
    if (reason === 'SPAM') return 'Спам';
    if (reason === 'ABUSE') return 'Оскорбления';
    if (reason === 'FAKE') return 'Фейк';
    if (reason === 'OTHER') return 'Другое';
    return reason || '';
}

export function statusLabel(status?: string) {
    if (status === 'BANNED') return 'забанен';
    if (status === 'DELETED') return 'удалён';
    if (status === 'WARNED') return 'есть предупреждение';
    return 'активен';
}

export function adminErrorMessage(error: unknown, fallback: string) {
    const data = (error as { response?: { data?: { detail?: string; message?: string; details?: string } } })?.response?.data;
    return data?.detail || data?.message || data?.details || fallback;
}
