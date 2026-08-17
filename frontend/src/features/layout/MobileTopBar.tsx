import React from 'react';
import { Bell, Settings } from 'lucide-react';
import { useNotification } from '@/shared/context/NotificationContext';

interface MobileTopBarProps {
  page: string;
  setPage: (p: string) => void;
}

export default function MobileTopBar({ page, setPage }: MobileTopBarProps) {
  const { notificationsCount } = useNotification();

  return (
    <header
      className="md:hidden fixed top-0 inset-x-0 z-30 flex items-center justify-between gap-3 px-4 bg-[#EFEAF6]/90 backdrop-blur-md border-b border-[#1A1916]/5"
      style={{ paddingTop: 'env(safe-area-inset-top)', height: 'calc(3.25rem + env(safe-area-inset-top))' }}
    >
      <div className="min-w-0">
        <div className="myraion-display text-[17px] text-[#1A1916] leading-none">На районе</div>
        <div className="text-[10px] text-[#6B645C] mt-0.5">соседская сеть</div>
      </div>
      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={() => setPage('notifications')}
          className={`relative w-11 h-11 rounded-full flex items-center justify-center ${
            page === 'notifications' ? 'bg-[#EDE6F5] text-[#5C4B7A]' : 'text-[#5C4B7A]'
          }`}
          aria-label="Уведомления"
        >
          <Bell className="w-5 h-5" />
          {notificationsCount > 0 && (
            <span className="absolute top-1.5 right-1.5 min-w-[16px] h-4 px-1 rounded-full bg-[#5C4B7A] text-white text-[10px] font-bold leading-4">
              {notificationsCount > 99 ? '99+' : notificationsCount}
            </span>
          )}
        </button>
        <button
          type="button"
          onClick={() => setPage('settings')}
          className={`w-11 h-11 rounded-full flex items-center justify-center ${
            page === 'settings' ? 'bg-[#EDE6F5] text-[#5C4B7A]' : 'text-[#5C4B7A]'
          }`}
          aria-label="Настройки"
        >
          <Settings className="w-5 h-5" />
        </button>
      </div>
    </header>
  );
}
