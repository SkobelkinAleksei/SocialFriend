import React, { useEffect } from 'react';
import { User, Users, MessageCircle, MapPin, Settings, Sparkles, Calendar, LogOut, Bell } from 'lucide-react';
import { theme } from '@/shared/ui/theme';
import { useAuth } from '@/shared/context/AuthContext';
import { useChat } from '@/features/chat/ChatContext';
import { useNotification } from '@/shared/context/NotificationContext';
import { getAvatarUrl } from '@/shared/utils/navigation';
import { presenceLabel } from '@/shared/utils/presence';
import { useIncomingFriendCount } from '@/shared/hooks/useIncomingFriendCount';

interface SidebarProps {
  page: string;
  setPage: (p: string) => void;
}

const NAV = [
  { key: 'my-page', label: 'Моя страница', icon: User },
  { key: 'friends', label: 'Мои друзья', icon: Users },
  { key: 'chats', label: 'Чаты', icon: MessageCircle },
  { key: 'district', label: 'На районе', icon: MapPin },
  { key: 'events', label: 'События', icon: Calendar },
  { key: 'notifications', label: 'Уведомления', icon: Bell },
  { key: 'settings', label: 'Настройки', icon: Settings },
];

export default function Sidebar({ page, setPage }: SidebarProps) {
  const { user, logout } = useAuth();

  // ИСПРАВЛЕНО: Из чатов берем только chatsCount, а из уведомлений — живой notificationsCount
  const { chatsCount } = useChat();
  const { notificationsCount, setNotificationsCount } = useNotification();
  const incomingCount = useIncomingFriendCount();

  useEffect(() => {
      const handleLiveNotification = (e: any) => {
          const newNotif = e.detail;

          // ДЕБАГ-ЛОГ: Поможет увидеть структуру, если фикс не сработает сразу
          console.log("[SIDEBAR WebSocket ДЕБАГ] Пришло живое уведомление:", newNotif);

          if (!newNotif) return;

          // Проверяем тип напрямую, а также на случай, если бэк завернул его в объект
          const notificationType = newNotif.type || (newNotif.notification && newNotif.notification.type);

          // ПРОДУКТОВЫЙ ФИКС: Если это сообщение, полностью игнорируем его в колокольчике!
          if (notificationType === 'NEW_CHAT_MESSAGE') {
              console.log("[SIDEBAR] Это сообщение чата. Счетчик Уведомлений НЕ увеличиваем.");
              return;
          }

          // Для всех остальных системных уведомлений (лайки, друзья) — инкрементируем
          setNotificationsCount(prev => prev + 1);
      };

      window.addEventListener('liveNotification', handleLiveNotification);
      return () => {
          window.removeEventListener('liveNotification', handleLiveNotification);
      };
  }, [setNotificationsCount]);

  return (
      <aside className="hidden md:flex fixed left-0 top-0 h-screen w-64 bg-transparent flex-col z-30">
        {/* Логотип */}
        <div className="px-6 py-6">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[#5C4B7A] flex items-center justify-center">
              <Sparkles className="w-5 h-5 text-white" strokeWidth={2} />
            </div>
            <div>
              <div className="myraion-display text-[18px] text-[#1A1916] leading-tight">На районе</div>
              <div className="text-[11px] text-[#6B645C]">соседская сеть</div>
            </div>
          </div>
        </div>

        {/* Навигация */}
        <nav className="flex-1 px-3 py-4 space-y-1">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = page === item.key;
            return (
                <button
                    key={item.key}
                    onClick={() => setPage(item.key)}
                    onPointerUp={(e) => {
                      if (e.pointerType !== 'mouse') e.currentTarget.blur();
                    }}
                    className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-full text-sm transition group ${
                        active
                          ? 'bg-[#1C1824] text-[#FFFCFA]'
                          : 'text-[#8A8494] [@media(hover:hover)_and_(pointer:fine)]:hover:bg-[#FFFCFA] [@media(hover:hover)_and_(pointer:fine)]:hover:text-[#1C1824]'
                    }`}
                >
                  <Icon
                      className={`w-5 h-5 ${active ? 'text-[#FFFCFA]' : 'text-[#8A8494] [@media(hover:hover)_and_(pointer:fine)]:group-hover:text-[#1C1824]'}`}
                      strokeWidth={active ? 2.5 : 2}
                  />
                  <span className="flex items-center justify-between w-full pr-1">
                {item.label}
                    {/* Индикатор для друзей */}
                    {item.key === 'friends' && incomingCount > 0 && (
                        <span className={`text-[15px] font-bold tabular-nums leading-none ${active ? 'text-[#EDE6F5]' : 'text-[#B85C5C]'}`}>
                    {incomingCount}
                  </span>
                    )}
                    {item.key === 'chats' && chatsCount > 0 && (
                        <span className={`text-[15px] font-bold tabular-nums leading-none ${active ? 'text-[#EDE6F5]' : 'text-[#5C4B7A]'}`}>
                    {chatsCount}
                  </span>
                    )}
                    {item.key === 'notifications' && notificationsCount > 0 && (
                        <span className={`text-[15px] font-bold tabular-nums leading-none ${active ? 'text-[#EDE6F5]' : 'text-[#5C4B7A]'}`}>
                    {notificationsCount}
                  </span>
                    )}
              </span>
                  {active && <span className={`ml-auto w-1.5 h-1.5 ${theme.radius.pill} bg-white`} />}
                </button>
            );
          })}
        </nav>

        {/* Футер сайдбара */}
        <div className="p-4 border-t border-[#1A1916]/10 space-y-3">
          <div className="flex items-center gap-3 px-2">
            <div className="relative">
              <img
                  src={getAvatarUrl(user?.id, user?.avatarUrl)}
                  alt={user?.firstName || 'Пользователь'}
                  className="w-9 h-9 rounded-full object-cover bg-[#EDE6F5]"
              />
              <span className={`absolute -bottom-0.5 -right-0.5 w-3 h-3 border-2 border-[#F2EBE3] rounded-full ${user ? 'bg-[#4A8B6F]' : 'bg-slate-300'}`} />
            </div>
            <div className="min-w-0">
              <div className="text-sm text-[#1A1916] truncate">
                {user ? `${user.firstName} ${user.lastName || ''}` : 'Загрузка...'}
              </div>
              <div className={`text-xs ${user ? 'text-[#3D6B56]' : 'text-[#6B645C]'}`}>
                {user ? presenceLabel(true) : '—'}
              </div>
            </div>
          </div>
          <button
              onClick={logout}
              className="w-full flex items-center gap-2.5 px-3 py-2.5 text-xs text-[#6B645C] hover:text-[#1A1916] hover:bg-[#FAF6F0] rounded-xl transition duration-150 group"
          >
            <LogOut className="w-4 h-4 text-[#6B645C] group-hover:text-[#1A1916] transition-colors" />
            <span>Выйти из аккаунта</span>
          </button>
        </div>
      </aside>
  );
}