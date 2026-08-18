import React, { useState } from 'react';
import { Mail, KeyRound } from 'lucide-react';
import Button from '@/shared/ui/Button';
import Card from '@/shared/ui/Card';
import Container from '@/shared/ui/Container';
import { theme } from '@/shared/ui/theme';
import { apiUrl } from '@/shared/lib/runtime';

interface VerifyEmailProps {
    setPage: (page: string) => void;
    initialEmail?: string;
}

export default function VerifyEmail({ setPage, initialEmail }: VerifyEmailProps) {
    const stored = (() => {
        try {
            return sessionStorage.getItem('myraion.verifyEmail') || '';
        } catch {
            return '';
        }
    })();
    const [email, setEmail] = useState(initialEmail || stored);
    const [code, setCode] = useState('');
    const [error, setError] = useState('');
    const [info, setInfo] = useState('');
    const [busy, setBusy] = useState(false);

    const submit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setInfo('');
        setBusy(true);
        try {
            const res = await fetch(apiUrl('/api/v1/social/registration/verify-email'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email.trim().toLowerCase(), code: code.trim() }),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.detail || 'Не получилось подтвердить почту');
            }
            try {
                sessionStorage.removeItem('myraion.verifyEmail');
            } catch {
                /* ignore */
            }
            setPage('login');
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Ошибка');
        } finally {
            setBusy(false);
        }
    };

    const resend = async () => {
        setError('');
        setInfo('');
        setBusy(true);
        try {
            const res = await fetch(apiUrl('/api/v1/social/registration/resend-code'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email.trim().toLowerCase() }),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.detail || 'Не отправили код');
            }
            setInfo('Письмо уже в пути. Проверьте в «Спам».');
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Ошибка');
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className={`min-h-screen min-h-dvh ${theme.surface.page} flex items-center justify-center py-12 px-4`}>
            <Container className="max-w-sm w-full">
                <div className="text-center mb-6">
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Подтвердите почту</h1>
                    <p className="text-slate-500 text-sm mt-1.5">
                        Код был отправлен на ящик
                    </p>
                </div>
                <Card className="shadow-xl">
                    <form onSubmit={submit} className="space-y-4">
                        {error && (
                            <div className="p-3 text-xs bg-red-50 text-red-600 rounded-xl border border-red-100 font-medium">
                                {error}
                            </div>
                        )}
                        {info && (
                            <div className="p-3 text-xs bg-emerald-50 text-emerald-700 rounded-xl border border-emerald-100 font-medium">
                                {info}
                            </div>
                        )}
                        <div>
                            <label className="text-xs font-semibold text-slate-600 mb-1 block">Email</label>
                            <div className="relative">
                                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                <input
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    className={`w-full pl-10 pr-4 py-3 text-sm bg-slate-50 border ${theme.surface.border} ${theme.radius.btn} focus:outline-none focus:ring-2 ${theme.accent.ring} focus:bg-white transition text-slate-900 min-h-11`}
                                    required
                                />
                            </div>
                        </div>
                        <div>
                            <label className="text-xs font-semibold text-slate-600 mb-1 block">Код из письма</label>
                            <div className="relative">
                                <KeyRound className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                <input
                                    inputMode="numeric"
                                    autoComplete="one-time-code"
                                    placeholder="123456"
                                    value={code}
                                    onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                                    className={`w-full pl-10 pr-4 py-3 text-sm bg-slate-50 border ${theme.surface.border} ${theme.radius.btn} focus:outline-none focus:ring-2 ${theme.accent.ring} focus:bg-white transition text-slate-900 min-h-11 tracking-[0.3em]`}
                                    required
                                />
                            </div>
                        </div>
                        <Button type="submit" variant="primary" className="w-full min-h-11" disabled={busy}>
                            Подтвердить
                        </Button>
                        <button
                            type="button"
                            onClick={resend}
                            disabled={busy || !email}
                            className={`w-full text-xs ${theme.accent.text} font-semibold bg-transparent border-none cursor-pointer`}
                        >
                            Прислать код ещё раз
                        </button>
                        <p className="text-center text-xs text-slate-500">
                            <button
                                type="button"
                                onClick={() => setPage('login')}
                                className="font-bold hover:underline bg-transparent border-none p-0 cursor-pointer"
                            >
                                Вернуться
                            </button>
                        </p>
                    </form>
                </Card>
            </Container>
        </div>
    );
}
