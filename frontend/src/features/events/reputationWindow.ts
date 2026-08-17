export type DateLike = string | Date | number[] | null | undefined;

export function toMillis(value: DateLike): number {
    if (value == null || value === '') {
        return NaN;
    }
    if (value instanceof Date) {
        return value.getTime();
    }
    if (Array.isArray(value) && value.length >= 5) {
        return new Date(
            Number(value[0]),
            Number(value[1]) - 1,
            Number(value[2]),
            Number(value[3] ?? 0),
            Number(value[4] ?? 0),
            Number(value[5] ?? 0),
        ).getTime();
    }
    return new Date(value as string).getTime();
}

export function formatClockTime(value: DateLike): string {
    if (Array.isArray(value) && value.length >= 5) {
        const hour = String(value[3]).padStart(2, '0');
        const minute = String(value[4]).padStart(2, '0');
        return `${hour}:${minute}`;
    }
    const ms = toMillis(value);
    if (Number.isNaN(ms)) {
        return '';
    }
    return new Date(ms).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

export function reputationPendingLabel(opensAt: DateLike, windowState: { closed: boolean }): string {
    if (windowState.closed) {
        return 'Окно оценок закрыто';
    }
    const clock = formatClockTime(opensAt);
    return clock ? `Оценки откроются в ${clock}` : 'Оценки скоро откроются';
}

export function reputationWindow(
    eventDate?: DateLike,
    opensAt?: DateLike,
    closesAt?: DateLike,
) {
    const start = toMillis(eventDate);
    if (Number.isNaN(start)) {
        return { started: false, open: false, closed: false };
    }
    const now = Date.now();
    const openAt = toMillis(opensAt);
    const closeAt = toMillis(closesAt);
    const started = now >= start;
    const closed = !Number.isNaN(closeAt) && now >= closeAt;
    const open = !Number.isNaN(openAt) && !Number.isNaN(closeAt) && now >= openAt && now < closeAt;
    return { started, open, closed };
}
