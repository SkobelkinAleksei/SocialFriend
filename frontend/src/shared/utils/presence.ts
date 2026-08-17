import api from '@/shared/lib/api';

export const PRESENCE_INTERVAL_MS = 30_000;

export type PresenceInfo = {
    online: boolean;
    lastSeenAt?: string | null;
};

export async function pingPresence(): Promise<void> {
    await api.post('/api/v1/social/users/me/presence');
}

export async function fetchPresence(ids: Array<string | number>): Promise<Record<string, PresenceInfo>> {
    const unique = Array.from(new Set(
        ids.map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0)
    ));
    if (unique.length === 0) return {};
    const res = await api.get('/api/v1/social/users/presence', {
        params: { ids: unique.join(',') },
    });
    const data = res.data || {};
    const out: Record<string, PresenceInfo> = {};
    Object.entries(data).forEach(([key, value]) => {
        if (value && typeof value === 'object') {
            const item = value as { online?: boolean; lastSeenAt?: string | null };
            out[String(key)] = { online: Boolean(item.online), lastSeenAt: item.lastSeenAt || null };
        } else {
            out[String(key)] = { online: Boolean(value), lastSeenAt: null };
        }
    });
    return out;
}

const MONTHS_SHORT = ['янв.', 'фев.', 'мар.', 'апр.', 'мая', 'июн.', 'июл.', 'авг.', 'сен.', 'окт.', 'нояб.', 'дек.'];

function startOfDay(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function formatLastSeen(lastSeenAt?: string | null): string | null {
    if (!lastSeenAt) return null;
    const seen = new Date(lastSeenAt);
    if (Number.isNaN(seen.getTime())) return null;

    const now = new Date();
    const time = seen.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    const today = startOfDay(now);
    const seenDay = startOfDay(seen);
    const diffDays = Math.round((today.getTime() - seenDay.getTime()) / 86_400_000);

    if (diffDays <= 0) return `Был(а) в ${time}`;
    if (diffDays === 1) return `Был(а) вчера в ${time}`;
    if (seen.getFullYear() === now.getFullYear()) {
        return `Был(а) ${seen.getDate()} ${MONTHS_SHORT[seen.getMonth()]} в ${time}`;
    }
    const date = seen.toLocaleDateString('ru-RU');
    return `Был(а) ${date} в ${time}`;
}

export function presenceLabel(online: boolean, lastSeenAt?: string | null): string {
    if (online) return 'В сети';
    return formatLastSeen(lastSeenAt) || 'Не в сети';
}
