/**
 * Адрес API.
 * `npm run dev` — пустой origin: Vite проксирует /api и /ws на gateway :8080, Docker фронт не нужен.
 * Docker/прод за Nginx — тот же хост, что и сайт.
 */
const fromEnv = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '');
const DEFAULT_API = '';

export const API_ORIGIN = fromEnv || DEFAULT_API;

export function wsOrigin(): string {
    const fromEnvWs = (import.meta.env.VITE_WS_URL as string | undefined)?.replace(/\/$/, '');
    if (fromEnvWs) return fromEnvWs;
    if (!API_ORIGIN && typeof window !== 'undefined') {
        const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        return `${proto}//${window.location.host}`;
    }
    return API_ORIGIN.replace(/^http/i, 'ws');
}

export function apiUrl(path: string): string {
    const p = path.startsWith('/') ? path : `/${path}`;
    return `${API_ORIGIN}${p}`;
}

export function wsUrl(path: string): string {
    const p = path.startsWith('/') ? path : `/${path}`;
    return `${wsOrigin()}${p}`;
}

/** Handshake WebSocket: gateway берёт JWT из query `token`. userId в URL не нужен. */
export function wsAuthUrl(path: string): string {
    const token = localStorage.getItem('token') || '';
    return `${wsUrl(path)}?token=${encodeURIComponent(token)}`;
}

/** Абсолютный URL картинки/файла через шлюз. */
export function mediaUrl(src?: string | null): string {
    if (!src) return '';
    const trimmed = String(src).trim();
    if (/^(javascript|vbscript):/i.test(trimmed)) return '';
    if (/^data:/i.test(trimmed) && !/^data:image\//i.test(trimmed)) return '';
    if (/^(https?:|blob:|data:)/i.test(trimmed)) return trimmed;
    return apiUrl(trimmed);
}

/** Для сохранения в БД — относительный путь без домена. */
export function toRelativeApiPath(src?: string | null): string {
    if (!src) return '';
    const resolved = mediaUrl(src);
    if (!resolved || resolved.startsWith('blob:') || resolved.startsWith('data:')) return '';
    if (API_ORIGIN && resolved.startsWith(API_ORIGIN)) {
        return resolved.slice(API_ORIGIN.length);
    }
    return resolved.replace(/^https?:\/\/[^/]+/i, '');
}
