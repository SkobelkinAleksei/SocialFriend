import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Flag, Users, Shield, LogOut } from 'lucide-react';
import { theme } from '@/shared/ui/theme';

export default function AdminSidebar({ onLogout }: { onLogout: () => void }) {
    const navigate = useNavigate();
    const location = useLocation();
    const onStaff = location.pathname.startsWith('/moderation/staff');
    const onPeople = location.pathname.startsWith('/moderation/people');
    const onReports = location.pathname.startsWith('/moderation') && !onPeople && !onStaff;

    const item = (active: boolean, extra = '') =>
        `w-full flex items-center gap-3 px-4 py-2.5 text-sm font-semibold rounded-full transition ${
            active
                ? `${theme.accent.bg} ${theme.text.onAccent}`
                : `${theme.text.body} hover:bg-[#EDE6F5]`
        } ${extra}`;

    return (
        <aside className={`hidden md:flex w-64 shrink-0 h-dvh sticky top-0 flex-col ${theme.surface.page} border-r ${theme.surface.border} px-4 py-6`}>
            <div className="px-2 mb-8">
                <div className="myraion-display text-2xl text-[#1C1824]">MyRaion</div>
                <div className="text-[11px] font-semibold uppercase tracking-wide text-[#8A8494] mt-1">Модерация</div>
            </div>
            <nav className="flex flex-col gap-1">
                <button type="button" className={item(onReports)} onClick={() => navigate('/moderation')}>
                    <Flag className="w-4 h-4" />
                    Жалобы
                </button>
                <button type="button" className={item(onPeople)} onClick={() => navigate('/moderation/people')}>
                    <Users className="w-4 h-4" />
                    Люди
                </button>
                <button type="button" className={item(onStaff)} onClick={() => navigate('/moderation/staff')}>
                    <Shield className="w-4 h-4" />
                    Админы
                </button>
            </nav>
            <div className="mt-auto pt-6">
                <button type="button" className={item(false)} onClick={onLogout}>
                    <LogOut className="w-4 h-4" />
                    Выйти
                </button>
            </div>
        </aside>
    );
}
