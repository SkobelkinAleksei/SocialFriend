import React, { createContext, useContext, useState, useEffect } from 'react';
import api, { logOutUser } from '@/shared/lib/api';
import { pingPresence, PRESENCE_INTERVAL_MS } from '@/shared/utils/presence';
import { reportPushViewing, reportPushViewingOnUnload } from '@/shared/lib/webPush';

interface UserProfile {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    numberPhone: string;
    birthday: string;
    city: string;
    streetAddress: string;
    districtName: string;
    reputation?: number;
    bio?: string;
    avatarUrl?: string | null;
    coverMode?: string | null;
    coverColor?: string | null;
    coverUrl?: string | null;
    photoVisibility?: string | null;
    platformRole?: string | null;
    accountStatus?: string | null;
    online?: boolean;
    homeLatitude?: number | null;
    homeLongitude?: number | null;
}

interface AuthContextType {
    user: UserProfile | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (accessToken: string, refreshToken?: string) => void;
    logout: () => void;
    refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<UserProfile | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const fetchProfile = async () => {
        try {
            const response = await api.get('/api/v1/social/users/me');
            const status = String(response.data?.accountStatus || 'ACTIVE').toUpperCase();
            if (status === 'BANNED' || status === 'DELETED') {
                setUser(null);
                logOutUser();
                return;
            }
            setUser(response.data);
            if (response.data?.id != null) {
                localStorage.setItem('userId', String(response.data.id));
            } else {
                localStorage.removeItem('userId');
            }
        } catch (error) {
            console.error('Ошибка загрузки профиля пользователя:', error);
            setUser(null);
            localStorage.removeItem('userId');
            const status = (error as { response?: { status?: number } })?.response?.status;
            if (status === 401 || status === 403) {
                logOutUser();
            }
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (token) {
            fetchProfile();
        } else {
            setIsLoading(false);
        }
    }, []);

    const login = (accessToken: string, refreshToken?: string) => {
        localStorage.setItem('token', accessToken);
        if (refreshToken) {
            localStorage.setItem('refreshToken', refreshToken);
        }
        setIsLoading(true);
        fetchProfile();
    };

    const logout = () => {
        setUser(null);
        logOutUser();
    };

    const refreshUser = async () => {
        const token = localStorage.getItem('token');
        if (!token) {
            setUser(null);
            return;
        }
        await fetchProfile();
    };

    useEffect(() => {
        if (!user?.id || user.platformRole === 'ADMIN') return;
        let cancelled = false;
        const ping = async () => {
            try {
                await pingPresence();
                if (document.visibilityState === 'visible') {
                    await reportPushViewing(true).catch(() => undefined);
                }
                if (!cancelled) {
                    setUser((prev) => (prev && prev.online !== true ? { ...prev, online: true } : prev));
                }
            } catch {
                // сеть/токен — статус обновится на следующем тике
            }
        };
        ping();
        const intervalId = window.setInterval(ping, PRESENCE_INTERVAL_MS);
        const onVisibility = () => {
            if (document.visibilityState === 'visible') {
                ping();
            } else {
                void reportPushViewing(false).catch(() => undefined);
            }
        };
        const onPageHide = () => reportPushViewingOnUnload();
        document.addEventListener('visibilitychange', onVisibility);
        window.addEventListener('pagehide', onPageHide);
        return () => {
            cancelled = true;
            window.clearInterval(intervalId);
            document.removeEventListener('visibilitychange', onVisibility);
            window.removeEventListener('pagehide', onPageHide);
            reportPushViewingOnUnload();
        };
    }, [user?.id, user?.platformRole]);

    return (
        <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout, refreshUser }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth должен использоваться внутри AuthProvider');
    }
    return context;
}
