import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '@/shared/ui/Card';
import Button from '@/shared/ui/Button';
import { adminApi, statusLabel, type AdminPersonPage } from '../adminApi';

export default function AdminPeoplePage() {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');
    const [applied, setApplied] = useState('');
    const [data, setData] = useState<AdminPersonPage | null>(null);
    const [error, setError] = useState('');

    useEffect(() => {
        adminApi.people(applied)
            .then((res) => setData(res.data))
            .catch(() => setError('Не удалось загрузить людей'));
    }, [applied]);

    return (
        <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
            <div>
                <h1 className="myraion-display text-3xl text-[#1C1824]">Люди</h1>
                <p className="text-sm text-[#8A8494] mt-1">Поиск по имени, почте или id. Два счётчика: сколько жаловались и сколько подтвердили.</p>
            </div>
            <form
                className="flex gap-2"
                onSubmit={(e) => {
                    e.preventDefault();
                    setApplied(query.trim());
                }}
            >
                <input
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Имя, почта или id"
                    className="flex-1 h-11 px-4 rounded-full border border-[#1C1824]/10 bg-[#FFFCFA] text-sm"
                />
                <Button type="submit">Найти</Button>
            </form>
            {error && <div className="text-sm text-rose-600">{error}</div>}
            <Card padded={false} className="overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="text-left text-[11px] uppercase tracking-wide text-[#8A8494] border-b border-[#1C1824]/10">
                        <tr>
                            <th className="px-4 py-3">id</th>
                            <th className="px-4 py-3">Человек</th>
                            <th className="px-4 py-3">Всего жалоб</th>
                            <th className="px-4 py-3">Удовлетворено</th>
                            <th className="px-4 py-3">Статус</th>
                            <th className="px-4 py-3" />
                        </tr>
                    </thead>
                    <tbody>
                        {(data?.items || []).map((row) => (
                            <tr key={row.id} className="border-b border-[#1C1824]/6 last:border-0">
                                <td className="px-4 py-3 text-[#8A8494]">{row.id}</td>
                                <td className="px-4 py-3">{row.firstName} {row.lastName}</td>
                                <td className="px-4 py-3">{row.reportsTotal}</td>
                                <td className="px-4 py-3">{row.reportsUpheld}</td>
                                <td className="px-4 py-3">{statusLabel(row.accountStatus)}</td>
                                <td className="px-4 py-3 text-right">
                                    <Button variant="secondary" size="sm" onClick={() => navigate(`/moderation/people/${row.id}`)}>
                                        Открыть
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        {data && data.items.length === 0 && (
                            <tr>
                                <td colSpan={6} className="px-4 py-10 text-center text-[#8A8494]">
                                    Никого не нашли. Попробуйте другое имя, почту или id.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </Card>
        </div>
    );
}
