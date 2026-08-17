import React, { useEffect, useState } from 'react';
import { useAuth } from '@/shared/context/AuthContext';
import Card from '@/shared/ui/Card';
import Button from '@/shared/ui/Button';
import { adminApi, adminErrorMessage, type AdminPerson } from '../adminApi';

const emptyForm = {
    firstName: '',
    lastName: '',
    email: '',
    numberPhone: '',
    password: '',
};

export default function AdminStaffPage() {
    const { user } = useAuth();
    const [staff, setStaff] = useState<AdminPerson[]>([]);
    const [form, setForm] = useState(emptyForm);
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);

    const load = () => adminApi.staff().then((res) => setStaff(res.data || []));

    useEffect(() => {
        load().catch(() => setError('Не удалось загрузить админов'));
    }, []);

    const create = async (e: React.FormEvent) => {
        e.preventDefault();
        setBusy(true);
        setError('');
        try {
            await adminApi.createAdmin({
                firstName: form.firstName.trim(),
                lastName: form.lastName.trim(),
                email: form.email.trim(),
                numberPhone: form.numberPhone.trim(),
                password: form.password,
            });
            setForm(emptyForm);
            await load();
        } catch (err: unknown) {
            setError(adminErrorMessage(err, 'Не удалось создать админа'));
        } finally {
            setBusy(false);
        }
    };

    const revoke = async (person: AdminPerson) => {
        if (person.id === user?.id) {
            setError('Нельзя снять права с самого себя');
            return;
        }
        if (!window.confirm(`Снять права админа с ${person.firstName} ${person.lastName}?`)) return;
        setBusy(true);
        setError('');
        try {
            await adminApi.revokeAdmin(person.id);
            await load();
        } catch (err: unknown) {
            setError(adminErrorMessage(err, 'Не удалось снять права'));
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
            <div>
                <h1 className="myraion-display text-3xl text-[#1C1824]">Админы</h1>
                <p className="text-sm text-[#8A8494] mt-1">
                    Служебные аккаунты модерации. Это не соседские страницы: после входа сразу админка.
                </p>
            </div>
            <Card>
                <div className="text-xs uppercase tracking-wide text-[#8A8494] mb-3">Новый админ</div>
                <form className="grid md:grid-cols-2 gap-3" onSubmit={create}>
                    <input
                        required
                        value={form.firstName}
                        onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                        placeholder="Имя"
                        className="h-11 px-4 rounded-full border border-[#1C1824]/10 bg-[#FFFCFA] text-sm"
                    />
                    <input
                        required
                        value={form.lastName}
                        onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                        placeholder="Фамилия"
                        className="h-11 px-4 rounded-full border border-[#1C1824]/10 bg-[#FFFCFA] text-sm"
                    />
                    <input
                        required
                        type="email"
                        value={form.email}
                        onChange={(e) => setForm({ ...form, email: e.target.value })}
                        placeholder="Email для входа"
                        className="h-11 px-4 rounded-full border border-[#1C1824]/10 bg-[#FFFCFA] text-sm"
                    />
                    <input
                        required
                        value={form.numberPhone}
                        onChange={(e) => setForm({ ...form, numberPhone: e.target.value })}
                        placeholder="Телефон +7…"
                        className="h-11 px-4 rounded-full border border-[#1C1824]/10 bg-[#FFFCFA] text-sm"
                    />
                    <input
                        required
                        type="password"
                        value={form.password}
                        onChange={(e) => setForm({ ...form, password: e.target.value })}
                        placeholder="Пароль: 8+ символов, заглавная, строчная, цифра"
                        className="h-11 px-4 rounded-full border border-[#1C1824]/10 bg-[#FFFCFA] text-sm md:col-span-2"
                    />
                    <div className="md:col-span-2">
                        <Button type="submit" disabled={busy}>Создать админа</Button>
                    </div>
                </form>
            </Card>
            {error && <div className="text-sm text-rose-600">{error}</div>}
            <Card padded={false} className="overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="text-left text-[11px] uppercase tracking-wide text-[#8A8494] border-b border-[#1C1824]/10">
                        <tr>
                            <th className="px-4 py-3">id</th>
                            <th className="px-4 py-3">Админ</th>
                            <th className="px-4 py-3">Email</th>
                            <th className="px-4 py-3" />
                        </tr>
                    </thead>
                    <tbody>
                        {staff.map((row) => (
                            <tr key={row.id} className="border-b border-[#1C1824]/6 last:border-0">
                                <td className="px-4 py-3 text-[#8A8494]">{row.id}</td>
                                <td className="px-4 py-3">{row.firstName} {row.lastName}{row.id === user?.id ? ' · вы' : ''}</td>
                                <td className="px-4 py-3">{row.email}</td>
                                <td className="px-4 py-3 text-right">
                                    <Button
                                        variant="secondary"
                                        size="sm"
                                        disabled={busy || row.id === user?.id}
                                        onClick={() => revoke(row)}
                                    >
                                        Снять права
                                    </Button>
                                </td>
                            </tr>
                        ))}
                        {staff.length === 0 && (
                            <tr>
                                <td colSpan={4} className="px-4 py-10 text-center text-[#8A8494]">
                                    Пока нет админов в базе
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </Card>
        </div>
    );
}
