import React, { useState } from 'react';
import { Mail, KeyRound, Lock, Eye, EyeOff } from 'lucide-react';
import Button from '@/shared/ui/Button';
import Card from '@/shared/ui/Card';
import Container from '@/shared/ui/Container';
import { theme } from '@/shared/ui/theme';
import { apiUrl } from '@/shared/lib/runtime';
import { passwordHints, validatePassword } from '@/shared/lib/validation';

interface ForgotPasswordProps {
    setPage: (page: string) => void;
}

export default function ForgotPassword({ setPage }: ForgotPasswordProps) {
    const [step, setStep] = useState<'email' | 'reset'>('email');
    const [email, setEmail] = useState('');
    const [code, setCode] = useState('');
    const [password, setPassword] = useState('');
    const [passwordRepeat, setPasswordRepeat] = useState('');
    const [showPass, setShowPass] = useState(false);
    const [showPassRepeat, setShowPassRepeat] = useState(false);
    const [error, setError] = useState('');
    const [info, setInfo] = useState('');
    const [busy, setBusy] = useState(false);

    const requestCode = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setBusy(true);
        try {
            const res = await fetch(apiUrl('/api/v1/social/registration/forgot-password'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email.trim().toLowerCase() }),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.detail || 'Не отправили письмо');
            }
            setInfo('Если такой ящик есть в районе — код уже в письме. Загляните в «Спам».');
            setStep('reset');
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Ошибка');
        } finally {
            setBusy(false);
        }
    };

    const reset = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        const passErr = validatePassword(password);
        if (passErr) {
            setError(passErr);
            return;
        }
        if (password !== passwordRepeat) {
            setError('Пароли не совпадают');
            return;
        }
        setBusy(true);
        try {
            const res = await fetch(apiUrl('/api/v1/social/registration/reset-password'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: email.trim().toLowerCase(),
                    code: code.trim(),
                    newPassword: password,
                }),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                const fieldMsg = Array.isArray(data.fieldErrors) && data.fieldErrors.length > 0
                    ? data.fieldErrors.map((fe: { message?: string }) => fe.message).filter(Boolean).join('; ')
                    : null;
                throw new Error(fieldMsg || data.detail || 'Не сменили пароль');
            }
            setPage('login');
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : 'Ошибка');
        } finally {
            setBusy(false);
        }
    };

    const hints = passwordHints(password);

    return (
        <div className={`min-h-screen min-h-dvh ${theme.surface.page} flex items-center justify-center py-12 px-4`}>
            <Container className="max-w-sm w-full">
                <div className="text-center mb-6">
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Забыли пароль</h1>
                    <p className="text-slate-500 text-sm mt-1.5">
                        {step === 'email' ? 'Пришлём код на почту, которой регистрировались' : 'Код из письма и новый пароль'}
                    </p>
                </div>
                <Card className="shadow-xl">
                    <form onSubmit={step === 'email' ? requestCode : reset} className="space-y-4">
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
                        {step === 'reset' && (
                            <>
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
                                <div>
                                    <label className="text-xs font-semibold text-slate-600 mb-1 block">Новый пароль</label>
                                    <div className="relative">
                                        <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                        <input
                                            type={showPass ? 'text' : 'password'}
                                            name="new-password"
                                            autoComplete="new-password"
                                            autoCorrect="off"
                                            spellCheck={false}
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            onCopy={(e) => e.preventDefault()}
                                            onCut={(e) => e.preventDefault()}
                                            className={`w-full pl-10 pr-10 py-3 text-sm bg-slate-50 border ${theme.surface.border} ${theme.radius.btn} focus:outline-none focus:ring-2 ${theme.accent.ring} focus:bg-white transition text-slate-900 min-h-11`}
                                            required
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPass(!showPass)}
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                                        >
                                            {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                        </button>
                                    </div>
                                    {password.length > 0 && (
                                        <ul className="mt-2 space-y-0.5 text-[11px] text-slate-500">
                                            {hints.map((h) => (
                                                <li key={h.text}>{h.ok ? '✓' : '·'} {h.text}</li>
                                            ))}
                                        </ul>
                                    )}
                                </div>
                                <div>
                                    <label className="text-xs font-semibold text-slate-600 mb-1 block">Повтор нового пароля</label>
                                    <div className="relative">
                                        <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                        <input
                                            type={showPassRepeat ? 'text' : 'password'}
                                            name="new-password-repeat"
                                            autoComplete="new-password"
                                            autoCorrect="off"
                                            spellCheck={false}
                                            value={passwordRepeat}
                                            onChange={(e) => setPasswordRepeat(e.target.value)}
                                            onCopy={(e) => e.preventDefault()}
                                            onCut={(e) => e.preventDefault()}
                                            className={`w-full pl-10 pr-10 py-3 text-sm bg-slate-50 border ${theme.surface.border} ${theme.radius.btn} focus:outline-none focus:ring-2 ${theme.accent.ring} focus:bg-white transition text-slate-900 min-h-11`}
                                            required
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPassRepeat(!showPassRepeat)}
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                                        >
                                            {showPassRepeat ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                        </button>
                                    </div>
                                </div>
                            </>
                        )}
                        <Button type="submit" variant="primary" className="w-full min-h-11" disabled={busy}>
                            {step === 'email' ? 'Прислать код' : 'Сохранить пароль'}
                        </Button>
                        <p className="text-center text-xs text-slate-500">
                            <button
                                type="button"
                                onClick={() => setPage('login')}
                                className="font-bold hover:underline bg-transparent border-none p-0 cursor-pointer"
                            >
                                Ко входу
                            </button>
                        </p>
                    </form>
                </Card>
            </Container>
        </div>
    );
}
