import React, { useEffect, useState } from 'react';
import {
    MessageSquare,
    MessageCircle,
    Bell,
    CheckSquare,
    ChevronRight,
    ChevronDown,
    Heart,
    Image as ImageIcon,
    Reply,
    UserPlus,
    UserCheck,
    UserX,
    UserMinus,
    CalendarPlus,
    CalendarCheck,
    CalendarX,
    CalendarOff,
    Clock,
    Star,
    Vote,
    Ban,
    Award,
} from 'lucide-react';
import { notificationsApi } from '@/shared/lib/api';
import { theme } from '@/shared/ui/theme';
import { useNotification } from "@/shared/context/NotificationContext";
import { parsePhotoContextLabel, routeCommentNotification } from '@/shared/utils/photoGallery';
import { openNeighborProfile, requestOpenChatFromNotification, isGroupChatNotificationLabel } from '@/shared/utils/navigation';

function isHumanNotificationSender(senderId?: number | null, firstName?: string | null, lastName?: string | null): boolean {
    if (!senderId) return false;
    const first = String(firstName || '').trim().toLowerCase();
    const last = String(lastName || '').replace(/[«»]/g, '').trim().toLowerCase();
    if (!first && !last) return false;
    if (first === 'система' || first === 'system' || last === 'myraion') return false;
    return true;
}

function commentPreview(message?: string, max = 72): string {
    const preview = String(message || '').trim().replace(/\s+/g, ' ');
    if (!preview) return '';
    return preview.length > max ? `${preview.slice(0, max).trim()}…` : preview;
}

function newCommentActionText(contextLabel?: string, message?: string): string {
    const target = parsePhotoContextLabel(contextLabel) ? 'вашему фото' : 'вашему посту';
    const preview = commentPreview(message);
    return preview
        ? `оставил(а) новый комментарий к ${target}: «${preview}»`
        : `оставил(а) новый комментарий к ${target}`;
}

function isInfoOnlyNotification(type: string): boolean {
    return type === 'EVENT_KICK' || type === 'EVENT_CANCELLED' || type === 'ADMIN_CONTENT_REMOVED';
}

interface NotificationItemDetail {
    id: number;
    senderId: number;
    senderFirstName?: string;
    senderLastName?: string;
    message: string;
    createdAt: string;
    commentId?: number;
}

interface NotificationItem {
    id: number;
    senderId: number;
    type: string;
    targetId: number;
    commentId?: number;
    message: string;
    read: boolean;
    createdAt: string;
    senderFirstName?: string;
    senderLastName?: string;
    contextLabel?: string;
    count: number;
    mergedIds: number[];
    items?: NotificationItemDetail[];
}

interface NotificationsProps {
    navigate: (p: string) => void;
}

export default function Notifications({ navigate }: NotificationsProps) {
    const [notifications, setNotifications] = useState<NotificationItem[]>([]);
    const [pageNumber, setPageNumber] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [loading, setLoading] = useState(false);
    const { setNotificationsCount } = useNotification();
    const [expandedGroups, setExpandedGroups] = useState<number[]>([]);

    const fetchNotifications = async (targetPage: number, append = false) => {
        setLoading(true);
        try {
            const res = await notificationsApi.getNotifications(targetPage, 10);
            if (res && res.data) {
                setNotifications(prev => append ? [...prev, ...res.data] : res.data);
                if (res.data.length < 10) setHasMore(false);
            }
        } catch (err) {
            console.error('Ошибка загрузки истории уведомлений:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchNotifications(0, false);
    }, []);

    const handleLoadMore = () => {
        const nextPage = pageNumber + 1;
        setPageNumber(nextPage);
        fetchNotifications(nextPage, true);
    };

    const handleMarkAllRead = async () => {
        try {
            await notificationsApi.markAllAsRead();
            setNotifications(prev => prev.map(n => ({ ...n, read: true })));
            setNotificationsCount(0);
        } catch (err) {
            console.error(err);
        }
    };

    const handleRowClick = async (n: NotificationItem) => {
        if (isInfoOnlyNotification(n.type)) {
            if (!n.read) {
                try {
                    await notificationsApi.markAsRead(n.id);
                    setNotifications(prev => prev.map(item => item.id === n.id ? { ...item, read: true } : item));
                    setNotificationsCount(prev => Math.max(0, prev - 1));
                } catch (err) {
                    console.error("Ошибка при прочтении уведомления:", err);
                }
            }
            return;
        }
        if (n.count > 1) {
            const isExpanded = expandedGroups.includes(n.id);
            if (!n.read && n.mergedIds) {
                try {
                    await notificationsApi.markGroupAsRead(n.mergedIds);
                    setNotifications(prev => prev.map(item =>
                        n.mergedIds.includes(item.id) ? { ...item, read: true } : item
                    ));
                    setNotificationsCount(prev => Math.max(0, prev - n.count));
                } catch (err) {
                    console.error(err);
                }
            }
            setExpandedGroups(prev => isExpanded ? prev.filter(id => id !== n.id) : [...prev, n.id]);
            return;
        }

        if (!n.read) {
            try {
                await notificationsApi.markAsRead(n.id);
                setNotifications(prev => prev.map(item => item.id === n.id ? { ...item, read: true } : item));
                setNotificationsCount(prev => Math.max(0, prev - 1));
            } catch (err) {
                console.error(err);
            }
        }
        executeRouting(n);
    };
    const executeRouting = (n: { id: number; type: string; targetId: number; senderId: number; commentId?: number; contextLabel?: string }) => {
        switch (n.type) {
            case 'NEW_CHAT_MESSAGE':
                requestOpenChatFromNotification({
                    isGroup: isGroupChatNotificationLabel(n.contextLabel),
                    groupId: n.targetId,
                    personalUserId: n.senderId,
                });
                break;
            case 'NEW_COMMENT':
            case 'COMMENT_REPLY':
                routeCommentNotification(n);
                break;
            case 'POST_LIKE':
                if (n.targetId) {
                    localStorage.removeItem('openCommentId');
                    window.dispatchEvent(new CustomEvent('forceOpenNotification', {
                        detail: { postId: Number(n.targetId) }
                    }));
                }
                break;
            case 'PHOTO_LIKE':
                window.dispatchEvent(new CustomEvent('forceOpenPhoto', {
                    detail: { contextLabel: n.contextLabel }
                }));
                break;
            case 'EVENT_JOIN_REQUEST':
                if (n.targetId && n.contextLabel === 'OPEN_JOIN') {
                    localStorage.setItem('activeGroupId', n.targetId.toString());
                    window.dispatchEvent(new CustomEvent('refreshChatRoomsList', { detail: { reason: 'join' } }));
                    navigate('chats');
                } else if (n.targetId) {
                    window.dispatchEvent(new CustomEvent('openGlobalRequestsModal', {
                        detail: { eventId: n.targetId }
                    }));
                }
                break;

            case 'EVENT_JOIN_REJECTED':
            case 'EVENT_JOIN_BANNED':
                if (n.targetId) {
                    window.dispatchEvent(new CustomEvent('openGlobalDetailsModal', {
                        detail: { eventId: n.targetId }
                    }));
                }
                break;
            case 'EVENT_JOIN_SUCCESS':
                if (n.targetId) {
                    localStorage.setItem('activeGroupId', n.targetId.toString());
                }
                window.dispatchEvent(new CustomEvent('refreshChatRoomsList', { detail: { reason: 'join' } }));
                navigate('chats');
                break;
            case 'FRIEND_REQUEST_SENT':
                localStorage.setItem('friendActiveTab', 'requests');
                localStorage.setItem('friendSubTab', 'incoming');
                navigate('friends');
                break;
            case 'FRIEND_REQUEST_ACCEPTED':
                localStorage.setItem('friendActiveTab', 'friends');
                localStorage.removeItem('friendSubTab');
                navigate('friends');
                break;
            case 'REPUTATION_UPDATE':
                navigate('my-page');
                break;
            case 'EVENT_KICK':
                navigate('events');
                break;
            case 'EVENT_REMIND_1H':
            case 'EVENT_REPUTATION_OPEN':
            case 'EVENT_CHAT_KEEP_VOTE':
                if (n.targetId) {
                    localStorage.setItem('activeGroupId', n.targetId.toString());
                }
                window.dispatchEvent(new CustomEvent('refreshChatRoomsList', { detail: { reason: 'lifecycle' } }));
                navigate('chats');
                break;
            default:
                navigate('my-page');
                break;
        }
    };

    const handleProfileClick = (senderId: number) => {
        openNeighborProfile(senderId);
    };

    const getIcon = (type: string, contextLabel?: string) => {
        const iconClass = 'w-5 h-5 text-[#5C4B7A]';
        const dangerClass = 'w-5 h-5 text-[#B85C5C]';
        switch (type) {
            case 'NEW_COMMENT': return <MessageSquare className={iconClass} />;
            case 'COMMENT_REPLY': return <Reply className={iconClass} />;
            case 'NEW_CHAT_MESSAGE': return <MessageCircle className={iconClass} />;
            case 'POST_LIKE': return <Heart className={iconClass} />;
            case 'PHOTO_LIKE': return <ImageIcon className={iconClass} />;
            case 'FRIEND_REQUEST_SENT': return <UserPlus className={iconClass} />;
            case 'FRIEND_REQUEST_ACCEPTED': return <UserCheck className={iconClass} />;
            case 'FRIEND_REQUEST_REJECTED': return <UserX className={dangerClass} />;
            case 'FRIEND_REQUEST_CANCELLED': return <UserMinus className={iconClass} />;
            case 'EVENT_JOIN_REQUEST':
                return contextLabel === 'OPEN_JOIN'
                    ? <UserCheck className={iconClass} />
                    : <CalendarPlus className={iconClass} />;
            case 'EVENT_JOIN_SUCCESS': return <CalendarCheck className={iconClass} />;
            case 'EVENT_JOIN_REJECTED': return <CalendarX className={dangerClass} />;
            case 'EVENT_JOIN_BANNED': return <Ban className={dangerClass} />;
            case 'REPUTATION_UPDATE': return <Award className={iconClass} />;
            case 'EVENT_KICK': return <UserX className={dangerClass} />;
            case 'EVENT_REMIND_1H': return <Clock className={iconClass} />;
            case 'EVENT_REPUTATION_OPEN': return <Star className={iconClass} />;
            case 'EVENT_CANCELLED': return <CalendarOff className={dangerClass} />;
            case 'ADMIN_CONTENT_REMOVED': return <Ban className={dangerClass} />;
            case 'EVENT_CHAT_KEEP_VOTE': return <Vote className={iconClass} />;
            default: return <Bell className={iconClass} />;
        }
    };


    return (
        <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8 min-h-screen pb-24">
            <div className="flex items-end justify-between gap-4 mb-8">
                <div>
                    <p className="text-[11px] uppercase tracking-[0.22em] text-[#8A8494] mb-2">Лента · новости</p>
                    <h1 className="myraion-display text-[32px] sm:text-[44px] leading-[0.92] text-[#1C1824]">Уведомления</h1>
                </div>
                {notifications.some(n => !n.read) && (
                    <button
                        onClick={handleMarkAllRead}
                        className="flex items-center gap-1.5 text-xs font-bold text-[#5C4B7A] hover:text-[#4A3C66] bg-[#FFFCFA] px-3 py-2 rounded-full transition"
                    >
                        <CheckSquare className="w-3.5 h-3.5" />
                        Прочитать все
                    </button>
                )}
            </div>

            <div className="flex flex-col gap-3">
                {notifications.length === 0 && !loading && (
                    <div className="text-center py-12 text-[#8A8494] text-sm bg-[#FFFCFA] rounded-[32px]">
                        У вас пока нет уведомлений
                    </div>
                )}

                {notifications.map((n) => {
                    const isExpanded = expandedGroups.includes(n.id);

                    const uniqueSenders = n.items
                        ? Array.from(new Set(n.items.map((i: any) => i.senderId))).filter(id => id !== n.senderId)
                        : [];
                    const uniqueSendersCount = uniqueSenders.length;

                    // ИСПРАВЛЕНО: Умное формирование текста в зависимости от типа уведомления
                    let notificationText = n.message;

                    if (n.type === 'COMMENT_REPLY' && !(n.count > 1)) {
                        notificationText = 'ответил(а) вам в комментариях';
                    } else if (n.type === 'NEW_COMMENT' && !(n.count > 1)) {
                        notificationText = newCommentActionText(n.contextLabel, n.message);
                    } else if (n.type === 'NEW_CHAT_MESSAGE') {
                        const label = String(n.contextLabel || '');
                        const preview = String(n.message || '').trim().replace(/\s+/g, ' ');
                        const short = preview.length > 28 ? `${preview.slice(0, 28).trim()}…` : preview;
                        if (label.startsWith('GROUP:')) {
                            const title = label.slice('GROUP:'.length).trim() || 'Встреча';
                            notificationText = `в «${title}»: ${short}`;
                        } else {
                            notificationText = short || 'новое сообщение';
                        }
                    } else if (n.count > 1) {
                        if (n.type === 'POST_LIKE') {
                            notificationText = uniqueSendersCount > 0
                                ? `и еще ${uniqueSendersCount} чел. оценили вашу публикацию`
                                : `оценил(а) вашу публикацию несколько раз`;
                        } else if (n.type === 'PHOTO_LIKE') {
                            notificationText = uniqueSendersCount > 0
                                ? `и еще ${uniqueSendersCount} чел. лайкнули ваше фото`
                                : `лайкнул(а) ваше фото несколько раз`;
                        } else if (n.type === 'NEW_COMMENT') {
                            notificationText = parsePhotoContextLabel(n.contextLabel)
                                ? (uniqueSendersCount > 0
                                    ? `и еще ${uniqueSendersCount} чел. прокомментировали ваше фото`
                                    : `оставил(а) комментарии к вашему фото`)
                                : (uniqueSendersCount > 0
                                    ? `и еще ${uniqueSendersCount} чел. прокомментировали ваш пост`
                                    : `оставил(а) комментарии к вашему посту`);
                        } else if (n.type === 'COMMENT_REPLY') {
                            notificationText = uniqueSendersCount > 0
                                ? `и еще ${uniqueSendersCount} чел. ответили вам в комментариях`
                                : `ответил(а) вам в комментариях`;
                        }
                    }

                    return (
                        <div key={n.id} className="flex flex-col gap-2">

                        {/* ГЛАВНАЯ КАРТОЧКА УВЕДОМЛЕНИЯ — СТАЛА СУЩЕСТВЕННО БОЛЬШЕ */}
                            <div
                                onClick={() => handleRowClick(n)}
                                className={`group relative p-4 rounded-[24px] transition-all duration-300 flex gap-3 items-center min-h-[76px] border ${
                                    isInfoOnlyNotification(n.type)
                                        ? 'cursor-default'
                                        : 'cursor-pointer'
                                } ${
                                    n.read
                                        ? `bg-[#FFFCFA] border-[#1C1824]/12 shadow-[0_10px_28px_-18px_rgba(28,24,36,0.35)] ${!isInfoOnlyNotification(n.type) ? 'hover:bg-white hover:border-[#1C1824]/18' : ''}`
                                        : `bg-white border-[#5C4B7A]/22 shadow-[0_12px_32px_-16px_rgba(92,75,122,0.45)] ${!isInfoOnlyNotification(n.type) ? 'hover:border-[#5C4B7A]/35' : ''}`
                                }`}
                            >
                                <span className="w-2 shrink-0 flex items-center justify-center" aria-hidden>
                                    {!n.read ? (
                                        <span className="w-2 h-2 bg-[#5C4B7A] rounded-full" />
                                    ) : null}
                                </span>

                                <div className={`p-3 rounded-full shrink-0 border ${
                                    n.read
                                        ? 'bg-slate-50 border-slate-100'
                                        : 'bg-[#FFFCFA] border-[#5C4B7A]/15'
                                }`}>
                                    {getIcon(n.type, n.contextLabel)}
                                </div>

                                {/* КРУПНЫЙ ТЕКСТОВЫЙ КОНТЕНТ */}
                                <div className="flex-1 min-w-0">
                                    <p className="text-[15px] md:text-base text-slate-600 font-normal leading-relaxed break-words flex items-center flex-wrap gap-1.5">
                                        {isHumanNotificationSender(n.senderId, n.senderFirstName, n.senderLastName) ? (
                                            <span
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleProfileClick(n.senderId);
                                                }}
                                                className="font-semibold text-[#1C1824] hover:text-[#5C4B7A] transition cursor-pointer text-[16px]"
                                            >
                {n.senderFirstName} {n.senderLastName || ''}
              </span>
                                        ) : null}

                                        <span className="text-slate-500 font-light">{notificationText}</span>

                                        {/* Крупный пастельный бадж общего количества действий */}
                                        {n.count > 1 && (
                                            <span className={`ml-2 px-2 py-0.5 text-xs font-medium rounded-full ${
                                                n.read ? 'bg-[#EDE6F5] text-[#8A8494]' : 'bg-[#DDD4F0] text-[#5C4B7A]'
                                            }`}>
                +{n.count}
              </span>
                                        )}
                                    </p>

                                    {/* Дата стала крупнее и читаемее */}
                                    <span className="text-xs text-slate-400 font-light mt-1 block">
            {new Date(n.createdAt).toLocaleString('ru-RU', {
                hour: '2-digit',
                minute: '2-digit',
                day: 'numeric',
                month: 'short'
            })}
          </span>
                                </div>

                                {/* Стрелочки переходов */}
                                {!isInfoOnlyNotification(n.type) && (
                                    n.count > 1 ? (
                                        isExpanded
                                            ? <ChevronDown className="w-5 h-5 text-slate-400 self-center shrink-0 transition-transform" />
                                            : <ChevronRight className="w-5 h-5 text-slate-300 self-center shrink-0 transition-transform group-hover:text-slate-400" />
                                    ) : (
                                        <ChevronRight className="w-5 h-5 text-slate-300 group-hover:text-slate-400 group-hover:translate-x-0.5 transition-all self-center shrink-0" />
                                    )
                                )}
                            </div>

                            {/* КРУПНЫЕ ВЛОЖЕННЫЕ УВЕДОМЛЕНИЯ */}
                            {n.count > 1 && isExpanded && n.items && (
                                <div className="pl-12 pr-1 flex flex-col gap-1.5 mt-1 mb-2 animate-[slideInUp_0.2s_ease-out]">
                                    {n.items.map((subItem) => (
                                        <div
                                            key={subItem.id}
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                executeRouting({
                                                    id: subItem.id,
                                                    type: n.type,
                                                    targetId: n.targetId,
                                                    senderId: subItem.senderId,
                                                    commentId: subItem.commentId,
                                                    contextLabel: n.contextLabel
                                                });
                                            }}
                                            // Увеличили падинги p-3.5 и скруглили сильнее
                                            className="p-3.5 bg-slate-50/50 hover:bg-slate-50 border border-slate-100 rounded-xl text-[14px] flex items-center justify-between transition-all cursor-pointer group/sub"
                                        >
                                            <div className="flex items-center gap-3">
                                                <div className="w-1.5 h-1.5 bg-slate-300 rounded-full" />
                                                <span className="text-slate-500 font-light">
                  {isHumanNotificationSender(subItem.senderId, subItem.senderFirstName, subItem.senderLastName) ? (
                  <span
                      onClick={(e) => {
                          e.stopPropagation();
                          handleProfileClick(subItem.senderId);
                      }}
                      className="font-semibold text-[#1C1824] hover:text-[#5C4B7A] transition cursor-pointer mr-1 text-[14px]"
                  >
                    {subItem.senderFirstName} {subItem.senderLastName || ''}
                  </span>
                  ) : null}
                                                    {n.type === 'FRIEND_REQUEST_SENT' && 'хочет добавиться в друзья'}
                                                    {n.type === 'FRIEND_REQUEST_ACCEPTED' && 'подтвердил(а) ваш запрос в друзья'}
                                                    {n.type === 'POST_LIKE' && (subItem.message || 'оценил(а) вашу публикацию')}
                                                    {n.type === 'PHOTO_LIKE' && (subItem.message || 'лайкнул(а) ваше фото')}
                                                    {n.type === 'NEW_COMMENT' && (parsePhotoContextLabel(n.contextLabel) ? 'оставил(а) новый комментарий к вашему фото' : 'оставил(а) новый комментарий к вашему посту')}
                                                    {n.type === 'COMMENT_REPLY' && 'ответил(а) вам в комментариях'}
                                                    {n.type === 'EVENT_KICK' && 'исключил(а) вас из события'}
                                                    {n.type === 'ADMIN_CONTENT_REMOVED' && (subItem.message || 'контент удалён за нарушение правил')}
                </span>
                                            </div>
                                            <ChevronRight className="w-4 h-4 text-slate-300 group-hover/sub:text-slate-400 group-hover/sub:translate-x-0.5 transition-all" />
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    );
                })}

            </div>

            {hasMore && notifications.length >= 10 && (
                <button
                    onClick={handleLoadMore}
                    disabled={loading}
                    className="w-full mt-4 py-2.5 bg-white hover:bg-slate-50 active:bg-slate-100 text-slate-600 text-xs font-bold rounded-xl transition border border-slate-200/80 shadow-sm disabled:opacity-50"
                >
                    {loading ? 'Загрузка...' : 'Загрузить еще'}
                </button>
            )}
        </div>
    );
}
