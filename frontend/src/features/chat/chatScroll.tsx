import React from 'react';
import { ArrowDown } from 'lucide-react';
import { UNREAD_MARKER_ID, isUnreadMarker } from '@/features/chat/chatUnread';

export function realMessageCount(messages: { id: string }[]): number {
    return messages.filter((m) => !isUnreadMarker(m.id)).length;
}

export function oldestNumericId(messages: { id: string }[]): number | null {
    const ids = messages
        .filter((m) => /^\d+$/.test(String(m.id)))
        .map((m) => Number(m.id));
    if (!ids.length) return null;
    return Math.min(...ids);
}

export function isNearChatBottom(container: HTMLElement, px = 250): boolean {
    return container.scrollHeight - container.scrollTop - container.clientHeight < px;
}

export function pinChatToBottom(container: HTMLElement | null, behavior: ScrollBehavior = 'auto'): void {
    if (!container) return;
    container.scrollTo({ top: container.scrollHeight, behavior });
}

export function scrollChatToUnreadOrBottom(container: HTMLElement | null): void {
    if (!container) return;
    const unread = container.querySelector(`#${UNREAD_MARKER_ID}`) as HTMLElement | null;
    if (unread) {
        container.scrollTo({ top: Math.max(0, unread.offsetTop - 16), behavior: 'auto' });
        return;
    }
    pinChatToBottom(container, 'auto');
}

export function scheduleChatOpenScroll(container: HTMLElement | null): void {
    const run = () => scrollChatToUnreadOrBottom(container);
    requestAnimationFrame(() => {
        requestAnimationFrame(run);
    });
    window.setTimeout(run, 80);
    window.setTimeout(run, 240);
}

export function ChatScrollDownButton({ show, onClick }: { show: boolean; onClick: () => void }) {
    if (!show) return null;
    return (
        <button
            type="button"
            onClick={onClick}
            className="absolute bottom-3 left-1/2 -translate-x-1/2 w-9 h-9 rounded-full bg-white border border-slate-200/80 shadow-md flex items-center justify-center text-slate-500 hover:text-[#5C4B7A] hover:bg-slate-50 transition-all duration-200 active:scale-90 z-20 animate-fadeIn"
            title="Вниз"
        >
            <ArrowDown className="w-4 h-4 stroke-[2.5]" />
        </button>
    );
}
