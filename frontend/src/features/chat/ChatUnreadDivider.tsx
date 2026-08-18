import React from 'react';
import type { Msg } from '@/features/chat/useChatActions';
import { UNREAD_MARKER_ID, isUnreadMarker } from '@/features/chat/chatUnread';

export { UNREAD_MARKER_ID, isUnreadMarker };

export function unreadMarkerMessage(): Msg {
    return {
        id: UNREAD_MARKER_ID,
        senderId: 0,
        from: 'them',
        text: 'Новые сообщения',
        time: '',
        isSystem: true,
    };
}

/** Линия перед первыми непрочитанными, как в чатах встреч. */
export function insertUnreadDivider(messages: Msg[], unreadCount: number): Msg[] {
    const unread = Math.max(0, Math.floor(Number(unreadCount) || 0));
    if (unread <= 0 || messages.length === 0) return messages;
    const without = messages.filter((m) => !isUnreadMarker(m.id));
    const markerIndex = Math.max(0, without.length - unread);
    return [
        ...without.slice(0, markerIndex),
        unreadMarkerMessage(),
        ...without.slice(markerIndex),
    ];
}

export default function ChatUnreadDivider({ text = 'Новые сообщения' }: { text?: string }) {
    return (
        <div
            id={UNREAD_MARKER_ID}
            className="flex items-center my-6 select-none pointer-events-none w-full animate-fadeIn"
        >
            <div className="flex-1 h-px bg-[#5C4B7A]/60" />
            <span className="mx-4 text-[11px] font-semibold text-[#5C4B7A] tracking-wide uppercase px-3 py-1 bg-[#EDE6F5] rounded-full border border-[#5C4B7A]/15 shadow-sm">
                {text}
            </span>
            <div className="flex-1 h-px bg-[#5C4B7A]/60" />
        </div>
    );
}
