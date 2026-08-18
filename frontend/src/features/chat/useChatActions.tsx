import { useState, useEffect, useRef, type MouseEvent, type TouchEvent } from 'react';
import api from '@/shared/lib/api';
import { isCompactViewport, isDesktopViewport } from '@/shared/utils/navigation';
import { isUnreadMarker } from '@/features/chat/chatUnread';
import { isNearChatBottom, pinChatToBottom, scheduleChatOpenScroll } from '@/features/chat/chatScroll';
import { showAppInfoToast } from '@/shared/utils/appToast';

const sameMsgId = (a: unknown, b: unknown) => String(a ?? '') === String(b ?? '');

const clearNativeTextSelection = () => {
    const sel = window.getSelection();
    if (sel && sel.rangeCount > 0) sel.removeAllRanges();
};

const eventTargetElement = (e: Event): Element | null => {
    const raw = e.target;
    if (raw instanceof Element) return raw;
    if (raw instanceof Node) return raw.parentElement;
    return null;
};

const eventHitsMessageMenu = (e: Event): boolean => {
    const path = typeof (e as PointerEvent).composedPath === 'function'
        ? (e as PointerEvent).composedPath()
        : [];
    for (const node of path) {
        if (node instanceof Element && (node.hasAttribute('data-message-menu') || node.closest('[data-message-menu]'))) {
            return true;
        }
    }
    return Boolean(eventTargetElement(e)?.closest('[data-message-menu]'));
};

export interface ReplyRef {
    id: string;
    author: string;
    text: string;
}

export interface ForwardedMessageInfo {
    id: number;
    senderId: number;
    senderFirstName?: string;
    senderLastName?: string;
    content?: string;
    photos?: string[];
    files?: Array<{ url?: string; name?: string; size?: number; mimeType?: string }>;
    voiceUrl?: string;
    voiceDuration?: number;
}

export interface Msg {
    id: string;
    senderId: number;
    senderFirstName?: string;
    senderLastName?: string;
    senderAvatarUrl?: string | null;
    from: 'me' | 'them';
    text: string;
    time: string;
    timestamp?: string;
    read?: boolean;
    reply_to?: ReplyRef[];
    edited?: boolean;
    isSystem?: boolean;
    isDeleted?: boolean;
    pinned?: boolean;
    forwardedFrom?: ForwardedMessageInfo;
    bundledForwards?: ForwardedMessageInfo[];
    photos?: string[];
    files?: Array<{ url?: string; name?: string; size?: number; mimeType?: string }>;
    voiceUrl?: string;
    voiceDuration?: number;
    /** Для системных сообщений с кликабельным ФИ (кик и т.п.) */
    systemLinkUserId?: number;
    systemLinkName?: string;
    systemLinkSuffix?: string;
    poll?: import('@/features/chat/chatPoll').ChatPoll;
}

const asDeletedMessage = (m: Msg): Msg => ({
    ...m,
    text: 'Сообщение удалено',
    isDeleted: true,
    photos: [],
    files: [],
    voiceUrl: undefined,
    voiceDuration: undefined,
});

interface UseChatActionsProps {
    refreshRooms: (() => void) | undefined;
}
export function useChatActions({ refreshRooms }: UseChatActionsProps) {
    const [draft, setDraft] = useState('');
    const [messages, setMessages] = useState<Msg[]>([]);
    const [editingId, setEditingId] = useState<string | null>(null);

    // Единая логика цитирования и множественного выбора (как в Telegram)
    const [replyTo, setReplyTo] = useState<ReplyRef[]>([]);
    const [selectedParentIds, setSelectedParentIds] = useState<string[]>([]);
    const [isSelectionMode, setIsSelectionMode] = useState<boolean>(false);
    const [isDeleteSelectionType, setIsDeleteSelectionType] = useState<boolean>(false);

    // Контекстное меню и нативная подсветка прыжка к цитате
    const [contextMenu, setContextMenu] = useState<{ x: number; y: number } | null>(null);
    const [contextMenuMsgId, setContextMenuMsgId] = useState<string | null>(null);
    const [highlightId, setHighlightId] = useState<string | null>(null);

    const messagesContainerRef = useRef<HTMLDivElement | null>(null);
    const inputRef = useRef<HTMLTextAreaElement | null>(null);
    const isMySentAction = useRef(false);
    const jumpingToMessageRef = useRef(false);
    const loadOlderMessagesRef = useRef<(() => Promise<boolean>) | null>(null);
    const [showScrollDown, setShowScrollDown] = useState<boolean>(false);
    const [forwardBuffer, setForwardBuffer] = useState<Msg[]>([]);

    const scrollToBottom = () => {
        pinChatToBottom(messagesContainerRef.current, 'smooth');
    };

    const revealLatest = (force = false) => {
        const container = messagesContainerRef.current;
        if (!container) return;
        if (force || isNearChatBottom(container)) pinChatToBottom(container, 'auto');
    };

    const handleComposerFocus = () => {
        if (!isCompactViewport()) return;
        jumpingToMessageRef.current = true;
        const run = () => pinChatToBottom(messagesContainerRef.current, 'auto');
        run();
        requestAnimationFrame(run);
        window.setTimeout(run, 80);
        window.setTimeout(run, 280);
        window.setTimeout(() => { jumpingToMessageRef.current = false; }, 420);
    };

    const scrollOpenThread = () => {
        jumpingToMessageRef.current = true;
        scheduleChatOpenScroll(messagesContainerRef.current);
        window.setTimeout(() => { jumpingToMessageRef.current = false; }, 450);
    };

    // Слушатель прокрутки контейнера чата
    const handleChatScroll = () => {
        const container = messagesContainerRef.current;
        if (!container) return;
        const isScrolledUp = container.scrollHeight - container.scrollTop - container.clientHeight > 400;
        setShowScrollDown(isScrolledUp);
    };

    const resetForwardBuffer = () => {
        setForwardBuffer([]);
    };
    // Умный автофокус на инпут при редактировании или цитировании с переносом каретки в конец
    useEffect(() => {
        if (editingId || replyTo.length > 0 || (selectedParentIds.length > 0 && !isSelectionMode)) {
            setTimeout(() => {
                const input = inputRef.current;
                if (input) {
                    input.focus();
                    if (editingId && input.value) {
                        const length = input.value.length;
                        input.setSelectionRange(length, length);
                    }
                }
            }, 50);
        }
    }, [editingId, replyTo, selectedParentIds, isSelectionMode]);

    // Закрытие меню: на десктопе сразу с первого левого клика; на касании — после
    // жеста открытия, чтобы touchend не закрыл меню в тот же момент.
    useEffect(() => {
        if (!contextMenu) return;
        const closeMenu = (e: Event) => {
            const me = e as unknown as MouseEvent;
            if (typeof me.button === 'number' && me.button === 2) return;
            if (eventHitsMessageMenu(e)) return;
            setContextMenu(null);
        };
        const closeOnScroll = () => setContextMenu(null);
        const delay = isDesktopViewport() ? 0 : 450;
        const timer = window.setTimeout(() => {
            window.addEventListener('pointerdown', closeMenu, true);
        }, delay);
        window.addEventListener('wheel', closeOnScroll, { capture: true, passive: true });
        window.addEventListener('scroll', closeOnScroll, true);
        const scroller = messagesContainerRef.current;
        scroller?.addEventListener('scroll', closeOnScroll);
        return () => {
            window.clearTimeout(timer);
            window.removeEventListener('pointerdown', closeMenu, true);
            window.removeEventListener('wheel', closeOnScroll, true);
            window.removeEventListener('scroll', closeOnScroll, true);
            scroller?.removeEventListener('scroll', closeOnScroll);
        };
    }, [contextMenu]);

    useEffect(() => {
        const inComposer = (el: Element | null) => Boolean(
            el?.closest?.('[data-chat-composer]') || (inputRef.current && el && inputRef.current.contains(el))
        );
        const inChatChrome = (el: Element | null) => Boolean(
            el?.closest?.('.myraion-chat-messages') || el?.closest?.('[data-message-menu]') || el?.closest?.('.message-bubble')
        );
        const onSelectStart = (e: Event) => {
            if (isDesktopViewport()) return;
            const el = eventTargetElement(e);
            if (inComposer(el)) return;
            if (inChatChrome(el)) e.preventDefault();
        };
        const onSelectionChange = () => {
            if (isDesktopViewport()) return;
            const sel = window.getSelection();
            if (!sel || sel.rangeCount === 0) return;
            const node = sel.anchorNode;
            const el = node instanceof Element ? node : node?.parentElement || null;
            if (inComposer(el)) return;
            if (inChatChrome(el)) sel.removeAllRanges();
        };
        const blockNativeMenu = (e: Event) => {
            if (isDesktopViewport()) return;
            const el = eventTargetElement(e);
            if (inComposer(el)) return;
            if (inChatChrome(el)) e.preventDefault();
        };
        document.addEventListener('selectstart', onSelectStart, true);
        document.addEventListener('selectionchange', onSelectionChange);
        document.addEventListener('contextmenu', blockNativeMenu, true);
        return () => {
            document.removeEventListener('selectstart', onSelectStart, true);
            document.removeEventListener('selectionchange', onSelectionChange);
            document.removeEventListener('contextmenu', blockNativeMenu, true);
        };
    }, []);

    useEffect(() => {
        if (messages.length === 0) return;
        const container = messagesContainerRef.current;
        if (!container) return;
        if (isMySentAction.current) {
            pinChatToBottom(container, 'smooth');
            isMySentAction.current = false;
            return;
        }
        if (jumpingToMessageRef.current) return;
        if (isNearChatBottom(container)) pinChatToBottom(container, 'smooth');
    }, [messages.length]);

    useEffect(() => {
        const onViewport = () => {
            const input = inputRef.current;
            if (!input || document.activeElement !== input) return;
            pinChatToBottom(messagesContainerRef.current, 'auto');
        };
        window.visualViewport?.addEventListener('resize', onViewport);
        window.addEventListener('resize', onViewport);
        return () => {
            window.visualViewport?.removeEventListener('resize', onViewport);
            window.removeEventListener('resize', onViewport);
        };
    }, []);

    // Открытие кастомного контекстного меню с идеальным расчетом границ экрана (+15px сдвиг)
    const longPressRef = useRef<{ timer: number | null; x: number; y: number }>({ timer: null, x: 0, y: 0 });

    const openMessageMenu = (clientX: number, clientY: number, msgId: string) => {
        const menuWidth = 200;
        const menuHeight = 300;
        const pad = 8;
        const composerReserve = 96;
        let posX = clientX + 15;
        let posY = clientY - 10;
        if (posX + menuWidth > window.innerWidth - pad) { posX = clientX - menuWidth - 15; }
        const maxAllowedY = window.innerHeight - menuHeight - composerReserve;
        if (posY > maxAllowedY) { posY = Math.max(pad, maxAllowedY); }
        if (posX < pad) posX = pad;
        if (posY < pad) posY = pad;
        setContextMenu({ x: posX, y: posY });
        setContextMenuMsgId(String(msgId));
    };

    const openMessageMenuRef = useRef(openMessageMenu);
    openMessageMenuRef.current = openMessageMenu;

    const lastDownRef = useRef<{ t: number; button: number } | null>(null);
    const lastHoverRef = useRef<{ x: number; y: number; target: EventTarget | null } | null>(null);
    const lastMsgRowRef = useRef<HTMLElement | null>(null);
    const lastMouseRef = useRef<{ x: number; y: number } | null>(null);

    const rowFromNode = (node: EventTarget | null): HTMLElement | null => {
        const container = messagesContainerRef.current;
        const el = node instanceof Element ? node : (node instanceof Node ? node.parentElement : null);
        const row = el?.closest('[data-msg-id]');
        if (row instanceof HTMLElement && (!container || container.contains(row))) return row;
        return null;
    };

    const rowFromPoint = (x: number, y: number): HTMLElement | null => {
        const container = messagesContainerRef.current;
        const stack = typeof document.elementsFromPoint === 'function'
            ? document.elementsFromPoint(x, y)
            : [document.elementFromPoint(x, y)];
        for (const node of stack) {
            const row = rowFromNode(node);
            if (row) return row;
        }
        if (!container) return null;
        const rows = container.querySelectorAll('[data-msg-id]');
        for (let i = 0; i < rows.length; i++) {
            const row = rows[i];
            if (!(row instanceof HTMLElement)) continue;
            const r = row.getBoundingClientRect();
            if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) return row;
        }
        return null;
    };

    const cssHoveredRow = (): HTMLElement | null => {
        const container = messagesContainerRef.current;
        const hovered = container?.querySelector('[data-msg-id]:hover');
        return hovered instanceof HTMLElement ? hovered : null;
    };

    const mapScreenToClient = (e: { screenX: number; screenY: number }) => {
        const x = e.screenX - window.screenX - (window.outerWidth - window.innerWidth);
        const y = e.screenY - window.screenY - (window.outerHeight - window.innerHeight);
        return { x, y };
    };

    const coordsLookBogus = (e: { button: number; clientX: number; clientY: number }) =>
        e.button === -1 || (e.clientX <= 2 && e.clientY <= 2);

    const resolveEventPoint = (e: { button: number; clientX: number; clientY: number; screenX: number; screenY: number }) => {
        if (!coordsLookBogus(e)) return { x: e.clientX, y: e.clientY, from: 'client' as const };
        if (lastMouseRef.current && (lastMouseRef.current.x > 2 || lastMouseRef.current.y > 2)) {
            return { x: lastMouseRef.current.x, y: lastMouseRef.current.y, from: 'lastMouse' as const };
        }
        const mapped = mapScreenToClient(e);
        if (mapped.x > 2 || mapped.y > 2) return { x: mapped.x, y: mapped.y, from: 'screen' as const };
        return { x: e.clientX, y: e.clientY, from: 'client' as const };
    };

    const trackPointer = (e: { clientX: number; clientY: number; target: EventTarget | null }) => {
        lastMouseRef.current = { x: e.clientX, y: e.clientY };
        const row = rowFromNode(e.target) || rowFromPoint(e.clientX, e.clientY);
        if (!row) return;
        lastHoverRef.current = { x: e.clientX, y: e.clientY, target: row };
        lastMsgRowRef.current = row;
    };

    const leftButtonHeld = (e: { buttons?: number }) => {
        if (typeof e.buttons === 'number' && (e.buttons & 1) === 1) return true;
        const lastDown = lastDownRef.current;
        return Boolean(lastDown && lastDown.button === 0);
    };

    const handleContextMenu = (e: MouseEvent, msgId: string) => {
        e.preventDefault();
        e.stopPropagation();
        if (isDesktopViewport()) return;
        openMessageMenu(e.clientX, e.clientY, String(msgId));
    };

    const openRowMenu = (row: HTMLElement, clientX: number, clientY: number) => {
        if (row.getAttribute('data-deleted') === 'true') return;
        const msgId = row.getAttribute('data-msg-id');
        if (!msgId) return;
        lastMsgRowRef.current = row;
        openMessageMenuRef.current(clientX, clientY, msgId);
    };

    useEffect(() => {
        const onPointerDown = (e: PointerEvent) => {
            if (e.button === 0) lastDownRef.current = { t: Date.now(), button: 0 };
            trackPointer(e);
            if (!isDesktopViewport() || e.button !== 2) return;
            const row = rowFromNode(e.target) || rowFromPoint(e.clientX, e.clientY) || lastMsgRowRef.current;
            if (!row) return;
            e.preventDefault();
            openRowMenu(row, e.clientX, e.clientY);
        };
        const onPointerUp = (e: PointerEvent) => {
            if (e.type === 'pointercancel' || e.button === 0) lastDownRef.current = null;
        };
        const onContextMenu = (e: Event) => {
            if (!isDesktopViewport()) return;
            const me = e as globalThis.MouseEvent;
            if (leftButtonHeld(me)) {
                me.preventDefault();
                return;
            }
            const hover = lastHoverRef.current;
            const point = resolveEventPoint(me);
            const hit = rowFromNode(me.target) || rowFromPoint(point.x, point.y);
            const broken = coordsLookBogus(me);
            const row = hit || (broken
                ? (cssHoveredRow() || rowFromNode(hover?.target || null) || (hover ? rowFromPoint(hover.x, hover.y) : null) || lastMsgRowRef.current)
                : null);
            if (row) {
                me.preventDefault();
                me.stopImmediatePropagation();
                const x = hit && !broken ? me.clientX : point.x;
                const y = hit && !broken ? me.clientY : point.y;
                openRowMenu(row, x, y);
                return;
            }
            setContextMenu(null);
        };
        window.addEventListener('pointermove', trackPointer, true);
        window.addEventListener('mousemove', trackPointer, true);
        window.addEventListener('mouseover', trackPointer, true);
        window.addEventListener('pointerdown', onPointerDown, true);
        window.addEventListener('pointerup', onPointerUp, true);
        window.addEventListener('pointercancel', onPointerUp, true);
        window.addEventListener('contextmenu', onContextMenu, true);
        return () => {
            window.removeEventListener('pointermove', trackPointer, true);
            window.removeEventListener('mousemove', trackPointer, true);
            window.removeEventListener('mouseover', trackPointer, true);
            window.removeEventListener('pointerdown', onPointerDown, true);
            window.removeEventListener('pointerup', onPointerUp, true);
            window.removeEventListener('pointercancel', onPointerUp, true);
            window.removeEventListener('contextmenu', onContextMenu, true);
        };
    }, []);

    const startMessageLongPress = (e: TouchEvent, msgId: string) => {
        if (isDesktopViewport()) return;
        const t = e.touches[0];
        if (!t) return;
        longPressRef.current.x = t.clientX;
        longPressRef.current.y = t.clientY;
        if (longPressRef.current.timer) window.clearTimeout(longPressRef.current.timer);
        longPressRef.current.timer = window.setTimeout(() => {
            longPressRef.current.timer = null;
            clearNativeTextSelection();
            openMessageMenu(longPressRef.current.x, longPressRef.current.y, msgId);
        }, 480);
        clearNativeTextSelection();
    };

    const moveMessageLongPress = (e: TouchEvent) => {
        const t = e.touches[0];
        if (!t || !longPressRef.current.timer) return;
        if (Math.hypot(t.clientX - longPressRef.current.x, t.clientY - longPressRef.current.y) > 12) {
            window.clearTimeout(longPressRef.current.timer);
            longPressRef.current.timer = null;
        }
    };

    const endMessageLongPress = () => {
        if (longPressRef.current.timer) {
            window.clearTimeout(longPressRef.current.timer);
            longPressRef.current.timer = null;
        }
    };

    // Прыжок к сообщению; если его нет на экране — подгружаем страницы истории.
    const findMessageNode = (id: any): HTMLElement | null => {
        if (id === undefined || id === null) return null;
        const cleanId = id.toString().replace(/\s+/g, '').toLowerCase();
        let el = document.getElementById(`msg-container-${cleanId}`);
        if (!el) el = document.querySelector(`[data-msg-id="${cleanId}"]`);
        if (el) return el;
        const containers = document.querySelectorAll('[id^="msg-container-"]');
        for (let i = 0; i < containers.length; i++) {
            const currentId = containers[i].id.replace('msg-container-', '').toLowerCase().trim();
            if (currentId === cleanId || Number(currentId) === Number(cleanId)) {
                return containers[i] as HTMLDivElement;
            }
        }
        return null;
    };

    const highlightMessage = (el: HTMLElement) => {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        const finalMsgId = el.id.replace('msg-container-', '').trim();
        setHighlightId(finalMsgId);
        window.setTimeout(() => setHighlightId(null), 1200);
    };

    const jumpToMessage = (id: any) => {
        if (id === undefined || id === null) return;
        void (async () => {
            jumpingToMessageRef.current = true;
            const waitPaint = () => new Promise<void>((resolve) => {
                requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
            });
            try {
                let el = findMessageNode(id);
                if (el) {
                    highlightMessage(el);
                    return;
                }
                const loader = loadOlderMessagesRef.current;
                if (!loader) return;
                for (let i = 0; i < 50; i++) {
                    const loaded = await loader();
                    await waitPaint();
                    if (loaded) {
                        await new Promise<void>((resolve) => window.setTimeout(resolve, 40));
                    }
                    el = findMessageNode(id);
                    if (el) {
                        highlightMessage(el);
                        return;
                    }
                    if (!loaded) break;
                }
            } finally {
                window.setTimeout(() => { jumpingToMessageRef.current = false; }, 400);
            }
        })();
    };



    // Переключение чекбоксов множественного выбора и синхронизация шапки ответа
    const toggleSelectParentId = (id: string) => {
        if (isUnreadMarker(id)) return;
        setEditingId(null);
        setSelectedParentIds(prev => {
            const next = prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id];
            const pickedMessages = messages.filter((m) => next.includes(m.id));

            const refs: ReplyRef[] = pickedMessages.map((m) => ({
                id: m.id.toString().replace(/\s+/g, ''), // ПРОДАКШН-ФИКС ТИПОВ: Принудительный string
                author: m.from === 'me' ? 'Вы' : `${m.senderFirstName || 'Участник'} ${m.senderLastName || ''}`.trim(),
                text: m.text,
            }));
            setReplyTo(refs);
            if (next.length === 0) setIsSelectionMode(false);
            return next;
        });
    };

    const deleteInFlightRef = useRef(false);

    // Одиночное удаление сообщения (Честный Soft Delete на бэкенде)
    const handleDeleteMessage = async (id: string) => {
        const key = String(id || '').trim();
        if (!key || deleteInFlightRef.current) return;
        const snapshot = messages.find((m) => sameMsgId(m.id, key));
        setMessages((prev) => prev.map((m) => sameMsgId(m.id, key) ? asDeletedMessage(m) : m));
        if (sameMsgId(editingId, key)) { setEditingId(null); setDraft(''); }
        deleteInFlightRef.current = true;
        try {
            await api.delete(`/api/v1/social/chats/message/${key}`);
            if (refreshRooms) { refreshRooms(); }
        } catch (err) {
            console.error("Ошибка одиночного удаления сообщения:", err);
            if (snapshot) {
                setMessages((prev) => prev.map((m) => sameMsgId(m.id, key) ? snapshot : m));
            }
            showAppInfoToast('Чат', 'Не удалось удалить сообщение');
        } finally {
            deleteInFlightRef.current = false;
        }
    };

    // Пакетное удаление сообщений (Batch Soft Delete по эндпоинту /message/batch)
    const handleDeleteSelectedMessages = async () => {
        if (deleteInFlightRef.current || selectedParentIds.length === 0) return;
        const keys = selectedParentIds.map((id) => String(id)).filter(Boolean);
        const idsToNumbers = keys.map((id) => Number(id)).filter((n) => Number.isFinite(n) && n > 0);
        if (idsToNumbers.length === 0) return;
        const keySet = new Set(keys);
        const snapshots = messages.filter((m) => keySet.has(String(m.id)));
        setMessages((prev) => prev.map((m) => keySet.has(String(m.id)) ? asDeletedMessage(m) : m));
        resetSelectionMode();
        deleteInFlightRef.current = true;
        try {
            try {
                await api.post('/api/v1/social/chats/message/batch', idsToNumbers);
            } catch {
                const results = await Promise.allSettled(
                    idsToNumbers.map((id) => api.delete(`/api/v1/social/chats/message/${id}`))
                );
                if (results.every((item) => item.status === 'rejected')) {
                    throw (results[0] as PromiseRejectedResult).reason;
                }
            }
            if (refreshRooms) { refreshRooms(); }
        } catch (err) {
            console.error("Ошибка пакетного удаления сообщений:", err);
            if (snapshots.length) {
                setMessages((prev) => prev.map((m) => {
                    const snap = snapshots.find((s) => sameMsgId(s.id, m.id));
                    return snap || m;
                }));
            }
            showAppInfoToast('Чат', 'Не удалось удалить сообщения');
        } finally {
            deleteInFlightRef.current = false;
        }
    };

    // Полный сброс режимов выбора и очистка стейтов
    const resetSelectionMode = () => {
        setSelectedParentIds([]);
        setIsSelectionMode(false);
        setIsDeleteSelectionType(false);
        setReplyTo([]);
    };

    return {
        draft, setDraft,
        messages, setMessages,
        editingId, setEditingId,
        replyTo, setReplyTo,
        selectedParentIds, setSelectedParentIds,
        isSelectionMode, setIsSelectionMode,
        isDeleteSelectionType, setIsDeleteSelectionType,
        contextMenu, setContextMenu,
        contextMenuMsgId, setContextMenuMsgId,
        highlightId,
        messagesContainerRef,
        inputRef,
        forwardBuffer,
        setForwardBuffer,
        resetForwardBuffer,
        isMySentAction,
        handleContextMenu,
        startMessageLongPress,
        moveMessageLongPress,
        endMessageLongPress,
        jumpToMessage,
        loadOlderMessagesRef,
        toggleSelectParentId,
        handleDeleteMessage,
        handleDeleteSelectedMessages,
        resetSelectionMode,
        showScrollDown,
        scrollToBottom,
        handleChatScroll,
        revealLatest,
        handleComposerFocus,
        scrollOpenThread,
    };
}
