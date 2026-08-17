import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Card from '@/shared/ui/Card';
import Button from '@/shared/ui/Button';
import { adminApi, adminErrorMessage, categoryLabel, reasonLabel, statusLabel, type AdminPerson, type AdminReport } from '../adminApi';

export default function AdminPersonPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const userId = Number(id);
    const [person, setPerson] = useState<AdminPerson | null>(null);
    const [reports, setReports] = useState<AdminReport[]>([]);
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);

    const load = async () => {
        const [p, r] = await Promise.all([adminApi.person(userId), adminApi.personReports(userId)]);
        setPerson(p.data);
        setReports(r.data || []);
    };

    useEffect(() => {
        if (!userId) return;
        load().catch(() => setError('Не удалось открыть карточку'));
    }, [userId]);

    const toggleBan = async () => {
        if (!person) return;
        setBusy(true);
        setError('');
        try {
            if (person.accountStatus === 'BANNED') {
                if (!window.confirm('Разбанить этого человека?')) return;
                await adminApi.unbanUser(person.id);
            } else {
                if (!window.confirm('Забанить этого человека на районе?')) return;
                await adminApi.banUser(person.id);
            }
            await load();
        } catch (e: unknown) {
            setError(adminErrorMessage(e, 'Не удалось изменить статус'));
        } finally {
            setBusy(false);
        }
    };

    if (!person) {
        return <div className="p-8 text-[#8A8494]">{error || 'Загружаем…'}</div>;
    }

    const banned = person.accountStatus === 'BANNED';
    const warned = person.reportsUpheld >= 1 && !banned;

    return (
        <div className="max-w-4xl mx-auto px-4 py-8 space-y-5">
            <button type="button" className="text-sm text-[#5C4B7A] font-semibold" onClick={() => navigate('/moderation/people')}>
                К списку
            </button>
            <div className="flex flex-wrap items-center gap-2">
                <h1 className="myraion-display text-3xl text-[#1C1824]">{person.firstName} {person.lastName} · id {person.id}</h1>
                <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#EDE6F5] text-[#5C4B7A]">{statusLabel(person.accountStatus)}</span>
            </div>
            <div className="grid grid-cols-3 gap-3">
                <Card className="text-center">
                    <div className="text-2xl font-bold">{person.id}</div>
                    <div className="text-xs text-[#8A8494] mt-1">id человека</div>
                </Card>
                <Card className="text-center">
                    <div className="text-2xl font-bold">{person.reportsTotal}</div>
                    <div className="text-xs text-[#8A8494] mt-1">Всего жалоб</div>
                </Card>
                <Card className="text-center">
                    <div className="text-2xl font-bold">{person.reportsUpheld}</div>
                    <div className="text-xs text-[#8A8494] mt-1">Удовлетворено</div>
                </Card>
            </div>
            {warned && (
                <Card className="border border-amber-200 bg-amber-50">
                    Уже получал предупреждение. Следующая удовлетворённая жалоба — бан.
                </Card>
            )}
            <div className="grid md:grid-cols-2 gap-4">
                <Card>
                    <div className="text-xs uppercase tracking-wide text-[#8A8494] mb-3">Действие</div>
                    <p className="text-sm text-[#8A8494] mb-3">{person.email}</p>
                    <div className="flex flex-wrap gap-2">
                        <Button disabled={busy} onClick={toggleBan}>{banned ? 'Разбанить' : 'Забанить на районе'}</Button>
                        <Button
                            variant="secondary"
                            disabled={busy || banned}
                            onClick={async () => {
                                if (!window.confirm('Сделать этого человека админом? Соседский вход у него пропадёт.')) return;
                                setBusy(true);
                                setError('');
                                try {
                                    await adminApi.createAdmin({ userId: person.id });
                                    navigate('/moderation/staff');
                                } catch (e: unknown) {
                                    setError(adminErrorMessage(e, 'Не удалось назначить админом'));
                                } finally {
                                    setBusy(false);
                                }
                            }}
                        >
                            Сделать админом
                        </Button>
                    </div>
                    {error && <p className="text-sm text-rose-600 mt-3">{error}</p>}
                </Card>
                <Card>
                    <div className="text-xs uppercase tracking-wide text-[#8A8494] mb-3">Жалобы на этого id</div>
                    {reports.length === 0 && <p className="text-sm text-[#8A8494]">Пока нет</p>}
                    <div className="space-y-2">
                        {reports.map((r) => (
                            <button
                                key={r.id}
                                type="button"
                                className="w-full text-left text-sm px-3 py-2 rounded-2xl bg-[#F7F4FA] hover:bg-[#EDE6F5]"
                                onClick={() => navigate(`/moderation/reports/${r.id}`)}
                            >
                                {categoryLabel(r.category)} · {reasonLabel(r.reason)} · цель id {r.targetId}
                            </button>
                        ))}
                    </div>
                </Card>
            </div>
        </div>
    );
}
