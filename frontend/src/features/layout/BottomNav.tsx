import React from 'react';
import { User, Users, MessageCircle, MapPin, Calendar } from 'lucide-react';
import { useChat } from '@/features/chat/ChatContext';
import { useIncomingFriendCount } from '@/shared/hooks/useIncomingFriendCount';

interface BottomNavProps {
  page: string;
  setPage: (p: string) => void;
}

const ITEMS = [
  { key: 'my-page', label: 'Я', icon: User, match: ['my-page', 'settings', 'notifications', 'neighbor-profile', 'photos', 'neighbor-photos', 'neighbor-events'] },
  { key: 'friends', label: 'Друзья', icon: Users, match: ['friends', 'neighbor-friends'] },
  { key: 'chats', label: 'Чаты', icon: MessageCircle, match: ['chats'] },
  { key: 'district', label: 'Район', icon: MapPin, match: ['district'] },
  { key: 'events', label: 'События', icon: Calendar, match: ['events'] },
] as const;

export default function BottomNav({ page, setPage }: BottomNavProps) {
  const { chatsCount } = useChat();
  const incomingCount = useIncomingFriendCount();

  return (
    <nav
      className="myraion-bottom-nav md:hidden fixed bottom-0 inset-x-0 z-30 border-t border-[#C8C4CE] bg-[#FFFCFA]"
      style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
    >
      <div className="grid grid-cols-5">
        {ITEMS.map((item) => {
          const Icon = item.icon;
          const active = (item.match as readonly string[]).includes(page);
          const badge = item.key === 'friends' ? incomingCount : item.key === 'chats' ? chatsCount : 0;
          return (
            <button
              key={item.key}
              type="button"
              onClick={() => setPage(item.key)}
              className={`relative flex flex-col items-center justify-center gap-0.5 min-h-[52px] text-[10px] tracking-wide ${
                active ? 'text-[#5C4B7A]' : 'text-[#8A8494]'
              }`}
            >
              <Icon className="w-5 h-5" strokeWidth={active ? 2.5 : 2} />
              <span>{item.label}</span>
              {badge > 0 && (
                <span className="absolute top-1 right-[18%] min-w-[16px] h-4 px-1 rounded-full bg-[#5C4B7A] text-white text-[10px] font-bold leading-4">
                  {badge > 99 ? '99+' : badge}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </nav>
  );
}
