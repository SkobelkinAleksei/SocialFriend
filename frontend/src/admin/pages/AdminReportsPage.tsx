import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '@/shared/ui/Card';
import Button from '@/shared/ui/Button';
import { adminApi, categoryLabel, reasonLabel, type AdminReportPage } from '../adminApi';

export default function AdminReportsPage() {
    const navigate = useNavigate();
    const [category, setCategory] = useState('ALL');
    const [data, setData] = useState<AdminReportPage | null>(null);
    const [error, setError] = useState('');

    useEffect(() => {
        setError('');
        adminApi.reports(category)
            .then((res) => setData(res.data))
            .catch(() => setError('Не удалось загрузить жалобы'));
    }, [category]);

    const tabs = [
        { id: 'ALL', label: 'Все' },
        { id: 'POST', label: 'Посты' },
        { id: 'COMMENT', label: 'Комментарии' },
        { id: 'PHOTO', label: 'Фото' },
        { id: 'EVENT', label: 'События' },
        { id: 'MESSAGE', label: 'Сообщения' },
        { id: 'CHAT', label: 'Чаты' },
    ];

    return (
        <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
            <div>
                <h1 className="myraion-display text-3xl text-[#1C1824]">Жалобы</h1>
                <p className="text-sm text-[#8A8494] mt-1">Очередь на разбор жалоб соседей.</p>
            </div>
            <div className="grid grid-cols-3 gap-3">
                <Card className="text-center">
                    <div className="text-2xl font-bold text-[#1C1824]">{data?.openCount ?? 0}</div>
                    <div className="text-xs text-[#8A8494] mt-1">Открытых</div>
                </Card>
                <Card className="text-center">
                    <div className="text-2xl font-bold text-[#1C1824]">{data?.upheldCount ?? 0}</div>
                    <div className="text-xs text-[#8A8494] mt-1">Удовлетворено</div>
                </Card>
                <Card className="text-center">
                    <div className="text-2xl font-bold text-[#1C1824]">{data?.warnedCount ?? 0}</div>
                    <div className="text-xs text-[#8A8494] mt-1">С предупреждением</div>
                </Card>
            </div>
            <div className="flex flex-wrap gap-2">
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        type="button"
                        onClick={() => setCategory(tab.id)}
                        className={`px-4 py-1.5 text-sm font-semibold rounded-full border ${
                            category === tab.id
                                ? 'bg-[#5C4B7A] text-[#FFFCFA] border-[#5C4B7A]'
                                : 'bg-[#FFFCFA] text-[#1C1824] border-[#1C1824]/10'
                        }`}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>
            {error && <div className="text-sm text-rose-600">{error}</div>}
            <Card padded={false} className="overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="text-left text-[11px] uppercase tracking-wide text-[#8A8494] border-b border-[#1C1824]/10">
                        <tr>
                            <th className="px-4 py-3">Когда</th>
                            <th className="px-4 py-3">Категория</th>
                            <th className="px-4 py-3">Причина</th>
                            <th className="px-4 py-3">Отправитель</th>
                            <th className="px-4 py-3">На кого</th>
                            <th className="px-4 py-3" />
                        </tr>
                    </thead>
                    <tbody>
                        {(data?.items || []).map((row) => (
                            <tr key={row.id} className="border-b border-[#1C1824]/6 last:border-0">
                                <td className="px-4 py-3 text-[#8A8494] whitespace-nowrap">{row.createdAt?.replace('T', ' ').slice(0, 16)}</td>
                                <td className="px-4 py-3">{categoryLabel(row.category)}</td>
                                <td className="px-4 py-3">{reasonLabel(row.reason)}</td>
                                <td className="px-4 py-3">{row.reporterName} · id {row.reporterId}</td>
                                <td className="px-4 py-3">{row.accusedName} · id {row.accusedId}</td>
                                <td className="px-4 py-3 text-right">
                                    <Button variant="secondary" size="sm" onClick={() => navigate(`/moderation/reports/${row.id}`)}>
                                        Смотреть
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        {data && data.items.length === 0 && (
                            <tr>
                                <td colSpan={6} className="px-4 py-10 text-center text-[#8A8494]">
                                    Открытых жалоб нет
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </Card>
        </div>
    );
}
