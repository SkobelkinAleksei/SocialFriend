import React, { useEffect } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/shared/context/AuthContext';
import { theme } from '@/shared/ui/theme';
import AdminSidebar from './AdminSidebar';
import AdminReportsPage from './pages/AdminReportsPage';
import AdminReportDetailPage from './pages/AdminReportDetailPage';
import AdminPeoplePage from './pages/AdminPeoplePage';
import AdminPersonPage from './pages/AdminPersonPage';
import AdminStaffPage from './pages/AdminStaffPage';

export default function AdminApp() {
    const { logout, user } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        if (!location.pathname.startsWith('/moderation')) {
            navigate('/moderation', { replace: true });
        }
    }, [location.pathname, navigate]);

    return (
        <div className={`min-h-screen min-h-dvh ${theme.surface.page} text-[#1C1824] flex`}>
            <AdminSidebar onLogout={() => {
                logout();
                window.location.href = '/login';
            }} />
            <main className="flex-1 min-w-0">
                <div className="md:hidden px-4 py-3 flex items-center justify-between border-b border-[#1C1824]/10">
                    <div>
                        <div className="myraion-display text-xl">Модерация</div>
                        <div className="text-[11px] text-[#8A8494]">{user?.email}</div>
                    </div>
                    <button type="button" className="text-sm font-semibold text-[#5C4B7A]" onClick={() => {
                        logout();
                        window.location.href = '/login';
                    }}>
                        Выйти
                    </button>
                </div>
                <div className="md:hidden flex gap-2 px-4 py-3">
                    <button type="button" className="text-sm font-semibold" onClick={() => navigate('/moderation')}>Жалобы</button>
                    <button type="button" className="text-sm font-semibold" onClick={() => navigate('/moderation/people')}>Люди</button>
                    <button type="button" className="text-sm font-semibold" onClick={() => navigate('/moderation/staff')}>Админы</button>
                </div>
                <Routes>
                    <Route path="/moderation" element={<AdminReportsPage />} />
                    <Route path="/moderation/reports/:id" element={<AdminReportDetailPage />} />
                    <Route path="/moderation/people" element={<AdminPeoplePage />} />
                    <Route path="/moderation/people/:id" element={<AdminPersonPage />} />
                    <Route path="/moderation/staff" element={<AdminStaffPage />} />
                    <Route path="*" element={<Navigate to="/moderation" replace />} />
                </Routes>
            </main>
        </div>
    );
}
