export const UNREAD_MARKER_ID = 'telegram-unread-line-marker';

export function isUnreadMarker(id: string | number | undefined | null): boolean {
    return String(id) === UNREAD_MARKER_ID;
}
