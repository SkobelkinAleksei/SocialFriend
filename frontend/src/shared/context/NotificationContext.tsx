import React, {createContext, ReactNode, useContext, useEffect, useRef, useState} from 'react';
import { createPortal } from 'react-dom';
import { Client } from '@stomp/stompjs';
import { useAuth } from '@/shared/context/AuthContext';
import api, { notificationsApi } from '@/shared/lib/api';
import { wsAuthUrl } from '@/shared/lib/runtime';
import { parsePhotoContextLabel, routeCommentNotification } from '@/shared/utils/photoGallery';
import { useChat } from '@/features/chat/ChatContext';
import { requestOpenChatFromNotification, isCompactViewport } from '@/shared/utils/navigation';
import { bindNotificationSoundUnlock, playNotificationSound } from '@/shared/utils/notificationSound';
import { isViewingThisChat } from '@/shared/lib/viewingChat';

interface NotificationContextType {
    notificationsCount: number;
    setNotificationsCount: React.Dispatch<React.SetStateAction<number>>;
    toasts: any[];
    showInfoToast: (title: string, message: string) => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

function buildPostSnippet(content: string, max = 80): string {
    const trimmed = content.trim().replace(/\s+/g, ' ');
    if (!trimmed) return '';
    return trimmed.length <= max ? trimmed : `${trimmed.slice(0, max).trim()}...`;
}

function truncateChatPreview(text: string, max = 28): string {
    const trimmed = String(text || '').trim().replace(/\s+/g, ' ');
    if (!trimmed) return '';
    return trimmed.length <= max ? trimmed : `${trimmed.slice(0, max).trim()}…`;
}

function normalizeChatLabel(label?: string | null): string {
    return String(label || '').replace(/^(MENTION:|PIN:)/, '');
}

function isGroupChatLabel(label?: string | null): boolean {
    return normalizeChatLabel(label).startsWith('GROUP:');
}

function groupTitleFromLabel(label?: string | null): string {
    const raw = normalizeChatLabel(label);
    if (!raw.startsWith('GROUP:')) return 'Встреча';
    const title = raw.slice('GROUP:'.length).trim();
    return title || 'Встреча';
}

function SwipeableBanner({
    children,
    onClick,
    onDismiss,
}: {
    children: React.ReactNode;
    onClick: () => void;
    onDismiss: () => void;
}) {
    const startY = useRef(0);
    const lastY = useRef(0);
    const dragging = useRef(false);
    const [offset, setOffset] = useState(0);
    const [leaving, setLeaving] = useState(false);
    const swiped = useRef(false);

    const finishDismiss = () => {
        setLeaving(true);
        setOffset(-140);
        window.setTimeout(onDismiss, 180);
    };

    return (
        <div
            onClick={() => {
                if (swiped.current || leaving) return;
                onClick();
            }}
            onTouchStart={(e) => {
                if (!isCompactViewport() || e.touches.length !== 1) return;
                startY.current = e.touches[0].clientY;
                lastY.current = startY.current;
                dragging.current = true;
                swiped.current = false;
            }}
            onTouchMove={(e) => {
                if (!dragging.current) return;
                const y = e.touches[0].clientY;
                lastY.current = y;
                const dy = y - startY.current;
                if (dy > 8) return;
                if (e.cancelable) e.preventDefault();
                setOffset(Math.min(0, dy));
                if (dy < -12) swiped.current = true;
            }}
            onTouchEnd={() => {
                if (!dragging.current) return;
                dragging.current = false;
                const dy = lastY.current - startY.current;
                if (dy <= -48) {
                    finishDismiss();
                    return;
                }
                setOffset(0);
            }}
            className="w-full lg:w-[400px] bg-[#FFFCFA] rounded-[22px] p-4 flex gap-3.5 items-start shadow-[0_20px_44px_-28px_rgba(92,75,122,0.45)] transform myraion-toast-animate lg:hover:scale-[1.02] cursor-pointer"
            style={{
                transform: `translateY(${offset}px)`,
                opacity: leaving ? 0 : Math.max(0.35, 1 + offset / 140),
                transition: dragging.current ? 'none' : 'transform 0.18s ease, opacity 0.18s ease',
            }}
        >
            {children}
        </div>
    );
}

/** Открыт ли сейчас тот чат, из которого пришло сообщение */
function isChatAlreadyOpen(body: any): boolean {
    return isViewingThisChat(body);
}

// ОБНОВЛЕНО: Провайдер теперь принимает setPage из роутинга App.tsx
export function NotificationProvider({ children }: { children: ReactNode }) {
    const { user } = useAuth();
    const { chats, personalGroups, eventRooms } = useChat();
    const muteRef = useRef({ chats, personalGroups, eventRooms });
    muteRef.current = { chats, personalGroups, eventRooms };
    const [notificationsCount, setNotificationsCount] = useState(0);
    const [toasts, setToasts] = useState<any[]>([]);
    const [confirmDialog, setConfirmDialog] = useState<any | null>(null);
    const stompClientRef = useRef<Client | null>(null);
    const [stompClient, setStompClient] = useState<Client | null>(null);
    const [stompConnected, setStompConnected] = useState(false);
    const toastTimers = useRef<Map<string, number>>(new Map());

    const dismissToast = (id: string) => {
        const timer = toastTimers.current.get(id);
        if (timer) {
            window.clearTimeout(timer);
            toastTimers.current.delete(id);
        }
        setToasts((prev) => prev.filter((t) => t.id !== id));
    };

    const scheduleDismiss = (id: string) => {
        const prev = toastTimers.current.get(id);
        if (prev) window.clearTimeout(prev);
        toastTimers.current.set(id, window.setTimeout(() => dismissToast(id), 4000));
    };

    const addToast = (notif: any) => {
        const isChat = notif.type === 'NEW_CHAT_MESSAGE';
        const newToast = {
            id: notif.id || `local-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
            senderId: notif.senderId,
            message: isChat ? truncateChatPreview(notif.message) : notif.message,
            senderFirstName: notif.senderFirstName,
            senderLastName: notif.senderLastName,
            type: notif.type,
            targetId: notif.targetId,
            commentId: notif.commentId,
            contextLabel: notif.contextLabel,
            infoTitle: notif.infoTitle
        };
        if (notif.type === 'APP_INFO') {
            setToasts((prev) => [...prev.filter((t) => t.type !== 'APP_INFO'), newToast]);
            scheduleDismiss(newToast.id);
            return;
        }
        playNotificationSound();
        const limit = isCompactViewport() ? 1 : 3;
        setToasts((prev) => {
            const infos = prev.filter((t) => t.type === 'APP_INFO');
            const banners = prev.filter((t) => t.type !== 'APP_INFO');
            const next = [...banners, newToast];
            const overflow = next.slice(0, Math.max(0, next.length - limit));
            overflow.forEach((t) => {
                const timer = toastTimers.current.get(t.id);
                if (timer) window.clearTimeout(timer);
                toastTimers.current.delete(t.id);
            });
            return [...infos, ...next.slice(-limit)];
        });
        scheduleDismiss(newToast.id);
    };

    const showInfoToast = (title: string, message: string) => {
        addToast({
            type: 'APP_INFO',
            infoTitle: title,
            message,
            senderFirstName: '',
            senderLastName: ''
        });
    };

    useEffect(() => bindNotificationSoundUnlock(), []);

    useEffect(() => {
        const onAppInfo = (e: Event) => {
            const detail = (e as CustomEvent).detail || {};
            showInfoToast(String(detail.title || 'Готово'), String(detail.message || ''));
        };
        const onAppConfirm = (e: Event) => {
            const detail = (e as CustomEvent).detail || {};
            setConfirmDialog({
                requestId: detail.requestId,
                title: String(detail.title || 'Подтверждение'),
                message: String(detail.message || ''),
                confirmText: String(detail.confirmText || 'Подтвердить'),
                cancelText: String(detail.cancelText || 'Отмена'),
                danger: Boolean(detail.danger)
            });
        };
        window.addEventListener('appInfoToast', onAppInfo);
        window.addEventListener('appConfirm', onAppConfirm);
        return () => {
            window.removeEventListener('appInfoToast', onAppInfo);
            window.removeEventListener('appConfirm', onAppConfirm);
        };
    }, []);

    const resolveConfirm = (confirmed: boolean) => {
        if (!confirmDialog?.requestId) {
            setConfirmDialog(null);
            return;
        }
        window.dispatchEvent(new CustomEvent('appConfirmResult', {
            detail: { requestId: confirmDialog.requestId, confirmed }
        }));
        setConfirmDialog(null);
    };

    const enrichLikeToastMessage = async (body: any): Promise<any> => {
        if (body?.type !== 'POST_LIKE' || !body?.targetId) return body;
        const current = String(body.message || '');
        // Уже есть цитата поста — не трогаем
        if (current.includes('"') || current.includes('«')) return body;
        try {
            const res = await api.get(`/api/v1/social/posts/id/${body.targetId}`);
            const snippet = buildPostSnippet(String(res.data?.content || ''));
            if (!snippet) return body;
            return {
                ...body,
                message: `оценил(а) ваш пост: "${snippet}"`
            };
        } catch {
            return body;
        }
    };

    useEffect(() => {
        if (!localStorage.getItem('token') || !user?.id) return;

        notificationsApi.getUnreadCount().then(res => setNotificationsCount(Number(res.data) || 0));

        const client = new Client({
            brokerURL: wsAuthUrl('/ws/notifications'),
            reconnectDelay: 5000,
            debug: () => {},
        });
        client.beforeConnect = () => {
            client.brokerURL = wsAuthUrl('/ws/notifications');
        };
        stompClientRef.current = client;

        client.onConnect = () => {
            setStompClient(client);
            setStompConnected(true);
            client.subscribe(`/topic/notifications-${user.id}`, (message) => {
                let body: any;
                try {
                    body = JSON.parse(message.body);
                } catch {
                    return;
                }
                if (!body) return;

                if (body.message === 'STATUS_REFRESH_SIGNAL') {
                    window.dispatchEvent(new CustomEvent('refreshNeighborStatuses', { detail: body }));
                    window.dispatchEvent(new CustomEvent('eventStatusRefresh', { detail: body }));
                    return;
                }

                // Системные sync-сигналы лайков (без тоста и колокольчика)
                if (body.message === '__SYSTEM_LIKE_REMOVED__' || body.message === '__DUPLICATE_LIKE__' || body.message === '__LIKE_SYNC__') {
                    if (body.type === 'POST_LIKE') {
                        window.dispatchEvent(new CustomEvent('liveLikeUpdate', {
                            detail: {
                                postId: body.targetId,
                                action: 'SYNC',
                                isRemoved: body.message === '__SYSTEM_LIKE_REMOVED__',
                                senderId: Number(body.senderId)
                            }
                        }));
                    }
                    return;
                }

                // Обычный лайк: всегда обновляем счётчик; тост — на каждый новый пост (анти-спам на бэке)
                if (body.type === 'POST_LIKE') {
                    window.dispatchEvent(new CustomEvent('liveLikeUpdate', {
                        detail: { postId: Number(body.targetId), action: 'SYNC', senderId: Number(body.senderId) }
                    }));
                }
                if (body.type === 'EVENT_KICK') {
                    window.dispatchEvent(new CustomEvent('refreshChatRoomsList', { detail: { reason: 'kick' } }));
                }
                if (body.type === 'EVENT_JOIN_SUCCESS'
                    || (body.type === 'EVENT_JOIN_REQUEST' && body.contextLabel === 'OPEN_JOIN')) {
                    window.dispatchEvent(new CustomEvent('refreshChatRoomsList', { detail: { reason: 'join' } }));
                }
                if (body.type !== 'NEW_CHAT_MESSAGE') {
                    // Актуальный счётчик с бэка (не +1: схлопывание дублей не должно накручивать)
                    notificationsApi.getUnreadCount()
                        .then(res => setNotificationsCount(Number(res.data) || 0))
                        .catch(() => setNotificationsCount(prev => prev + 1));
                } else {
                    window.dispatchEvent(new CustomEvent('liveChatMessageNotify', { detail: body }));
                    if (isChatAlreadyOpen(body)) return;
                    const label = String(body.contextLabel || '');
                    const rooms = [...(muteRef.current.personalGroups || []), ...(muteRef.current.eventRooms || [])];
                    const muted = isGroupChatLabel(label)
                        ? rooms.some((r) => r && Number(r.id) === Number(body.targetId) && r.muted)
                        : (muteRef.current.chats || []).some((c) => c && Number(c.id) === Number(body.senderId) && c.muted);
                    if (muted) return;
                }

                void (async () => {
                    const enriched = await enrichLikeToastMessage(body);
                    addToast(enriched);
                })();
            });

        };

        client.onDisconnect = () => setStompConnected(false);
        client.onWebSocketClose = () => setStompConnected(false);
        client.onStompError = (frame) => {
            console.error('[Notifications WS] STOMP error', frame.headers['message'], frame.body);
        };
        client.onWebSocketError = (event) => {
            console.error('[Notifications WS] WebSocket error', event);
        };

        client.activate();
        return () => { setStompConnected(false); setStompClient(null); client.deactivate(); };
    }, [user?.id]);

    const getToastTitle = (toast: any) => {
        const type = typeof toast === 'string' ? toast : toast?.type;
        switch (type) {
            case 'NEW_COMMENT': return 'Новый комментарий';
            case 'COMMENT_REPLY': return 'Ответ в комментариях';
            case 'NEW_CHAT_MESSAGE': {
                const label = typeof toast === 'string' ? '' : toast?.contextLabel;
                if (String(label || '').startsWith('MENTION:')) {
                    return isGroupChatLabel(label) ? `Вас упомянули · ${groupTitleFromLabel(label)}` : 'Вас упомянули';
                }
                if (String(label || '').startsWith('PIN:')) {
                    return `Закрепили сообщение · ${groupTitleFromLabel(label)}`;
                }
                if (isGroupChatLabel(label)) {
                    const title = groupTitleFromLabel(label);
                    const short = title.length > 32 ? `${title.slice(0, 32).trim()}…` : title;
                    return `Групповой чат · ${short}`;
                }
                return 'Личный чат';
            }
            case 'EVENT_JOIN_REQUEST': {
                const label = typeof toast === 'string' ? '' : toast?.contextLabel;
                if (label === 'OPEN_JOIN') return 'Новый участник встречи';
                if (label === 'PRIVATE_REAPPLY') return 'Повторная заявка';
                return 'Заявка на встречу';
            }
            case 'EVENT_JOIN_SUCCESS': return 'Добавлен в групповой чат';
            case 'EVENT_JOIN_REJECTED': return 'Заявка отклонена (можно подать снова)';
            case 'EVENT_JOIN_BANNED': return 'Заявка отклонена окончательно';
            case 'REPUTATION_UPDATE': return 'Обновление репутации';
            case 'POST_LIKE': return 'Лайк к вашей записи';
            case 'PHOTO_LIKE': return 'Лайк к вашему фото';
            case 'FRIEND_REQUEST_SENT': return 'Новая заявка в друзья';
            case 'FRIEND_REQUEST_ACCEPTED': return 'Заявка в друзья принята';
            case 'FRIEND_REQUEST_REJECTED': return 'Заявка в друзья отклонена';
            case 'FRIEND_REQUEST_CANCELLED': return 'Заявка в друзья отменена';
            case 'EVENT_KICK': return 'Исключение из встречи';
            case 'EVENT_REMIND_1H': return 'Встреча скоро начнётся';
            case 'EVENT_REPUTATION_OPEN': return 'Можно поставить оценки';
            case 'EVENT_CANCELLED': return 'Встреча отменена';
            case 'ADMIN_CONTENT_REMOVED': return 'Решение модератора';
            case 'EVENT_CHAT_KEEP_VOTE': return 'Голосование за чат встречи';
            case 'APP_INFO': return (typeof toast === 'string' ? 'Готово' : (toast?.infoTitle || 'Готово'));
            default: return 'Уведомление';
        }
    };

    const handleToastClick = async (toast: any) => {
        // Новая логика: отправляем запрос на бэк о прочтении тоста
        if (toast.id) {
            try {
                await notificationsApi.markAsRead(toast.id);
                // Уменьшаем счетчик на колокольчике, не давая ему упасть ниже нуля
                setNotificationsCount(prev => Math.max(0, prev - 1));
            } catch (err) {
                console.error("Не удалось отметить всплывающее уведомление как прочитанное:", err);
            }
        }

        let targetPage = 'notifications'; // Дефолтная страница
        switch (toast.type) {
            case 'NEW_CHAT_MESSAGE':
                requestOpenChatFromNotification({
                    isGroup: isGroupChatLabel(toast.contextLabel),
                    groupId: toast.targetId,
                    personalUserId: toast.senderId,
                });
                targetPage = '';
                break;
            case 'NEW_COMMENT':
            case 'COMMENT_REPLY':
                routeCommentNotification(toast);
                targetPage = '';
                break;
            case 'POST_LIKE':
                if (toast.targetId) {
                    localStorage.removeItem('openCommentId');
                }
                targetPage = '';
                break;
            case 'PHOTO_LIKE':
                window.dispatchEvent(new CustomEvent('forceOpenPhoto', {
                    detail: { contextLabel: toast.contextLabel }
                }));
                setToasts(prev => prev.filter(t => t.id !== toast.id));
                return;
            case 'EVENT_JOIN_SUCCESS':
                if (toast.targetId) {
                    localStorage.setItem('activeGroupId', toast.targetId.toString());
                }
                window.dispatchEvent(new CustomEvent('refreshChatRoomsList', { detail: { reason: 'join' } }));
                targetPage = 'chats';
                break;
            case 'EVENT_JOIN_REQUEST':
                if (toast.targetId && toast.contextLabel === 'OPEN_JOIN') {
                    localStorage.setItem('activeGroupId', toast.targetId.toString());
                    window.dispatchEvent(new CustomEvent('refreshChatRoomsList', { detail: { reason: 'join' } }));
                    targetPage = 'chats';
                } else if (toast.targetId) {
                    window.dispatchEvent(new CustomEvent('openGlobalRequestsModal', {
                        detail: { eventId: toast.targetId }
                    }));
                    targetPage = '';
                }
                break;

            case 'EVENT_JOIN_REJECTED':
            case 'EVENT_JOIN_BANNED':
                if (toast.targetId) {
                    // Стреляем событием открытия карточки для отклоненного соседа
                    window.dispatchEvent(new CustomEvent('openGlobalDetailsModal', {
                        detail: { eventId: toast.targetId }
                    }));
                }
                targetPage = ''; // Никуда не уходим
                break;
            case 'FRIEND_REQUEST_SENT':
                localStorage.setItem('friendActiveTab', 'requests');
                localStorage.setItem('friendSubTab', 'incoming');
                targetPage = 'friends';
                break;
            case 'APP_INFO':
                setToasts((prev) => prev.filter((t) => t.id !== toast.id));
                return;
            case 'ADMIN_CONTENT_REMOVED':
                setToasts((prev) => prev.filter((t) => t.id !== toast.id));
                return;
            case 'FRIEND_REQUEST_ACCEPTED':
                localStorage.setItem('friendActiveTab', 'friends');
                localStorage.removeItem('friendSubTab');
                targetPage = 'friends';
                break;
            case 'REPUTATION_UPDATE':
                targetPage = 'my-page';
                break;
            case 'EVENT_REMIND_1H':
            case 'EVENT_REPUTATION_OPEN':
            case 'EVENT_CANCELLED':
            case 'EVENT_CHAT_KEEP_VOTE':
                if (toast.targetId) {
                    localStorage.setItem('activeGroupId', toast.targetId.toString());
                    window.dispatchEvent(new CustomEvent('refreshChatRoomsList', { detail: { reason: 'lifecycle' } }));
                }
                targetPage = 'chats';
                break;
            default:
                targetPage = 'notifications';
                break;
        }

        if (toast.type === 'POST_LIKE' || toast.type === 'NEW_COMMENT' || toast.type === 'COMMENT_REPLY') {
            if ((toast.type === 'NEW_COMMENT' || toast.type === 'COMMENT_REPLY') && parsePhotoContextLabel(toast.contextLabel)) {
                window.dispatchEvent(new CustomEvent('forceOpenPhoto', {
                    detail: { contextLabel: toast.contextLabel, commentId: toast.commentId }
                }));
                setToasts(prev => prev.filter(t => t.id !== toast.id));
                return;
            }
            if (toast.type === 'NEW_COMMENT' || toast.type === 'COMMENT_REPLY') {
                setToasts(prev => prev.filter(t => t.id !== toast.id));
                return;
            }
            window.dispatchEvent(new CustomEvent('forceOpenNotification', {
                detail: { postId: Number(toast.targetId) }
            }));

            setToasts(prev => prev.filter(t => t.id !== toast.id));
            return;
        }

        if (targetPage) {
            window.dispatchEvent(new CustomEvent('changePage', { detail: targetPage }));
        }
        setToasts(prev => prev.filter(t => t.id !== toast.id));
    };


    return (
        <NotificationContext.Provider value={{
            notificationsCount,
            setNotificationsCount,
            toasts,
            showInfoToast,
            stompClient,
            stompConnected,
        } as any}>
            {children}

            <style>{`
        @keyframes slideInUp {
          from { transform: translateY(1.5rem); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
        @keyframes slideInDown {
          from { transform: translateY(-1.25rem); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
        @keyframes fadeScaleIn {
          from { transform: translate(-50%, -48%) scale(0.96); opacity: 0; }
          to { transform: translate(-50%, -50%) scale(1); opacity: 1; }
        }
        .myraion-toast-animate {
          animation: slideInDown 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }
        @media (min-width: 1024px) {
          .myraion-toast-animate {
            animation: slideInUp 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
          }
        }
        .myraion-info-toast-animate {
          animation: fadeScaleIn 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }
      `}</style>

            {/* Информационные тосты по центру (вместо alert) */}
            <div className="fixed inset-0 pointer-events-none" style={{ zIndex: 10000 }}>
                {toasts.filter((t) => t.type === 'APP_INFO').map((toast) => (
                    <div
                        key={toast.id}
                        className="pointer-events-auto absolute left-1/2 top-1/2 w-[min(420px,92vw)] bg-[#FFFCFA] shadow-2xl rounded-[28px] p-6 myraion-info-toast-animate text-center"
                    >
                        <div className="myraion-display text-[22px] text-[#1C1824] leading-tight mb-2">
                            {toast.infoTitle || 'Готово'}
                        </div>
                        <p className="text-sm text-[#5A5566] leading-relaxed text-center">{toast.message}</p>
                        <button
                            type="button"
                            onClick={() => setToasts((prev) => prev.filter((t) => t.id !== toast.id))}
                            className="mt-5 w-full h-11 rounded-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white text-xs transition"
                        >
                            Понятно
                        </button>
                    </div>
                ))}
            </div>

            {/* Подтверждение по центру экрана (вместо window.confirm) */}
            {confirmDialog && createPortal(
                <div
                    className="fixed inset-0"
                    style={{ zIndex: 10001, background: 'rgba(28, 24, 36, 0.45)' }}
                    onClick={() => resolveConfirm(false)}
                >
                    <div
                        className="absolute left-1/2 top-1/2 w-[min(420px,92vw)] bg-[#FFFCFA] shadow-2xl rounded-[28px] p-6 myraion-info-toast-animate text-center"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="myraion-display text-[22px] text-[#1C1824] leading-tight mb-2">
                            {confirmDialog.title}
                        </div>
                        <p className="text-sm text-[#5A5566] leading-relaxed text-center whitespace-pre-line">
                            {confirmDialog.message}
                        </p>
                        <div className="mt-5 grid grid-cols-2 gap-3">
                            <button
                                type="button"
                                onClick={() => resolveConfirm(false)}
                                className="h-11 rounded-full bg-white text-[#1C1824] text-xs hover:bg-[#EFEAF6] transition"
                            >
                                {confirmDialog.cancelText}
                            </button>
                            <button
                                type="button"
                                onClick={() => resolveConfirm(true)}
                                className={`h-11 rounded-full text-white text-xs transition ${
                                    confirmDialog.danger
                                        ? 'bg-[#B85C5C] hover:bg-[#9E4A4A]'
                                        : 'bg-[#5C4B7A] hover:bg-[#4A3C66]'
                                }`}
                            >
                                {confirmDialog.confirmText}
                            </button>
                        </div>
                    </div>
                </div>,
                document.body
            )}

            {/* КОНТЕЙНЕР ДЛЯ ВСПЛЫВАЮЩИХ ТОСТОВ */}
            <div
                className="fixed inset-x-3 top-[calc(3.25rem+env(safe-area-inset-top)+0.5rem)] flex flex-col gap-2 pointer-events-auto md:inset-x-auto md:left-[calc(16rem+0.75rem)] md:right-6 md:top-4 lg:top-auto lg:bottom-6 lg:left-auto lg:right-6 lg:gap-3"
                style={{ zIndex: 9999 }}
            >
                {toasts.filter((t) => t.type !== 'APP_INFO').map((toast) => (
                    <div key={toast.id} className="myraion-toast-animate">
                    <SwipeableBanner
                        onClick={() => handleToastClick(toast)}
                        onDismiss={() => dismissToast(toast.id)}
                    >
                        <div className="p-2.5 bg-[#EDE6F5] rounded-full shrink-0">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-[#5C4B7A]">
                                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                                <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                            </svg>
                        </div>

                        <div className="flex-1 min-w-0 pr-4 text-left">
                            <span className="text-[10px] tracking-wider text-[#5C4B7A] uppercase block mb-0.5">
    {getToastTitle(toast)}
  </span>

                            {!(
                                toast.type === 'EVENT_JOIN_SUCCESS'
                                || toast.contextLabel === 'JOINER_CHAT'
                                || !toast.senderId
                                || String(toast.senderFirstName || '').trim().toLowerCase() === 'система'
                                || String(toast.senderFirstName || '').trim().toLowerCase() === 'system'
                                || String(toast.senderLastName || '').replace(/[«»]/g, '').trim().toLowerCase() === 'myraion'
                            ) && (
                                <span className="text-[#1C1824] text-[14px] mr-1">
                                    {toast.senderFirstName} {toast.senderLastName || ''}
                                </span>
                            )}

                            <p className="text-[13px] text-[#8A8494] leading-relaxed mt-0.5 break-all line-clamp-2">
                                {toast.type === 'COMMENT_REPLY'
                                    ? 'ответил(а) вам в комментариях'
                                    : toast.type === 'NEW_COMMENT'
                                        ? `оставил(а) новый комментарий к ${parsePhotoContextLabel(toast.contextLabel) ? 'вашему фото' : 'вашему посту'}${
                                            String(toast.message || '').trim()
                                                ? `: «${buildPostSnippet(String(toast.message), 72)}»`
                                                : ''
                                        }`
                                        : toast.message}
                            </p>
                        </div>

                        <button
                            onClick={(e) => {
                                e.stopPropagation();
                                dismissToast(toast.id);
                            }}
                            className="p-1 rounded-full text-[#8A8494] hover:bg-[#EFEAF6] hover:text-[#1C1824] transition shrink-0"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </SwipeableBanner>
                    </div>
                ))}
            </div>
        </NotificationContext.Provider>
    );
}

export const useNotification = () => {
    const context = useContext(NotificationContext);
    if (!context) throw new Error('useNotification must be used within a NotificationProvider');
    return context;
};
