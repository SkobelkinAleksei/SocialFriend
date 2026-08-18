import React, { useState, useEffect, useRef } from 'react';
import { User, Mail, Phone, Lock, MapPin, Eye, EyeOff } from 'lucide-react';
import Button from '@/shared/ui/Button';
import Card from '@/shared/ui/Card';
import Container from '@/shared/ui/Container';
import { theme } from '@/shared/ui/theme';
import { apiUrl } from '@/shared/lib/runtime';
import {
    formatRuPhone,
    isoDateShift,
    passwordHints,
    SIGNUP,
    toE164Ru,
    validateBirthday,
    validateEmail,
    validateName,
    validatePassword,
    validatePhone,
} from '@/shared/lib/validation';

interface Suggestion {
    value: string;
    data: {
        city: string;
        street_with_type: string;
        house: string;
        geo_lat: string;
        geo_lon: string;
        district: string;
    };
}

interface SignUpProps {
    setPage: (page: string) => void;
    onOpenLegal?: (doc: 'privacy' | 'rules') => void;
}

export default function SignUp({ setPage, onOpenLegal }: SignUpProps) {
    const [formData, setFormData] = useState({
        firstName: '', lastName: '', email: '', numberPhone: '', password: '', birthday: ''
    });

    const [addressQuery, setAddressQuery] = useState('');
    const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
    const [selectedAddressValue, setSelectedAddressValue] = useState<string | null>(null);
    const [selectedGeo, setSelectedGeo] = useState<{
        city: string; streetAddress: string; districtName: string; lat: number; lng: number;
    } | null>(null);

    const [showPass, setShowPass] = useState(false);
    const [acceptedTerms, setAcceptedTerms] = useState(false);
    const [error, setError] = useState('');
    const [fieldError, setFieldError] = useState<string>('');
    const suggestionsRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (addressQuery.length < 3 || addressQuery === selectedAddressValue) {
            setSuggestions([]);
            return;
        }

        const delayDebounce = setTimeout(async () => {
            try {
                const response = await fetch(apiUrl('/api/v1/geo/suggest/address'), {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    },
                    body: JSON.stringify({query: addressQuery, count: 5})
                });

                const result = await response.json();
                setSuggestions(result.suggestions || []);
            } catch (err) {
                console.error("Ошибка DaData:", err);
            }
        }, 400);

        return () => clearTimeout(delayDebounce);
    }, [addressQuery, selectedAddressValue]);

    useEffect(() => {
        if (suggestions.length === 0) return;
        suggestionsRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }, [suggestions]);

    const handleSelectAddress = (s: Suggestion) => {
        const street = `${s.data.street_with_type || ''} ${s.data.house ? 'д. ' + s.data.house : ''}`.trim();
        setSelectedAddressValue(s.value);
        setAddressQuery(s.value);
        setSelectedGeo({
            city: s.data.city || 'Москва',
            streetAddress: street || s.value,
            districtName: s.data.district || 'Центральный',
            lat: parseFloat(s.data.geo_lat || '55.7558'),
            lng: parseFloat(s.data.geo_lon || '37.6173')
        });
        setSuggestions([]);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        const first = validateName(formData.firstName);
        const last = validateName(formData.lastName);
        const email = validateEmail(formData.email);
        const phone = validatePhone(formData.numberPhone);
        const pass = validatePassword(formData.password);
        const born = validateBirthday(formData.birthday);
        const local = first || last || email || phone || pass || born;
        if (local) {
            setFieldError(local);
            return;
        }
        if (!acceptedTerms) {
            setFieldError('Чтобы создать аккаунт, примите Правила сообщества и Политику конфиденциальности.');
            return;
        }
        if (!selectedGeo) {
            setFieldError('Пожалуйста, выберите точный адрес дома из выпадающего списка подсказок.');
            return;
        }
        setFieldError('');
        const payload = {
            firstName: formData.firstName.trim(),
            lastName: formData.lastName.trim(),
            email: formData.email.trim().toLowerCase(),
            numberPhone: toE164Ru(formData.numberPhone),
            password: formData.password,
            birthday: formData.birthday,
            acceptedTerms: true,
            city: selectedGeo.city,
            streetAddress: selectedGeo.streetAddress,
            districtName: selectedGeo.districtName,
            homeLatitude: selectedGeo.lat,
            homeLongitude: selectedGeo.lng
        };
        try {
            const res = await fetch(apiUrl('/api/v1/social/registration/signUp'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const data = await res.json();
                const fieldMsg = Array.isArray(data.fieldErrors) && data.fieldErrors.length > 0
                    ? data.fieldErrors.map((fe: { message?: string }) => fe.message).filter(Boolean).join('; ')
                    : null;
                throw new Error(fieldMsg || data.detail || data.message || 'Ошибка регистрации');
            }
            try {
                sessionStorage.setItem('myraion.verifyEmail', payload.email);
            } catch {
                /* ignore */
            }
            setPage('verify-email');
        } catch (err: any) {
            setError(err.message);
        }
    };

    const hints = passwordHints(formData.password);

    return (
        <div className={`min-h-screen min-h-dvh ${theme.surface.page} flex items-start sm:items-center justify-center py-8 sm:py-12 px-4 overflow-x-hidden`}>
            <Container className="max-w-md w-full min-w-0">
                <div className="text-center mb-6">
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Создать аккаунт</h1>
                    <p className="text-slate-500 text-sm mt-1.5">Добро пожаловать в соседскую сеть «На районе»</p>
                </div>
                <Card className="shadow-xl">
                    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
                        {(error || fieldError) && (
                            <div className="p-3 text-xs bg-red-50 text-red-600 rounded-xl border border-red-100 font-medium">
                                {fieldError || error}
                            </div>
                        )}
                        <div className="grid grid-cols-2 gap-3">
                            <FormInput
                                label="Имя"
                                icon={User}
                                placeholder="Иван"
                                value={formData.firstName}
                                maxLength={SIGNUP.nameMax}
                                onChange={(v: string) => setFormData({...formData, firstName: v})}
                            />
                            <FormInput
                                label="Фамилия"
                                icon={User}
                                placeholder="Иванов"
                                value={formData.lastName}
                                maxLength={SIGNUP.nameMax}
                                onChange={(v: string) => setFormData({...formData, lastName: v})}
                            />
                        </div>
                        <FormInput
                            label="Email"
                            type="email"
                            icon={Mail}
                            placeholder="name@example.com"
                            value={formData.email}
                            onChange={(v: string) => setFormData({...formData, email: v})}
                        />
                        <FormInput
                            label="Телефон"
                            type="tel"
                            icon={Phone}
                            placeholder="+7 (___) ___-__-__"
                            value={formData.numberPhone}
                            onChange={(v: string) => setFormData({...formData, numberPhone: formatRuPhone(v)})}
                        />
                        <FormInput
                            label="Дата рождения"
                            type="date"
                            value={formData.birthday}
                            min={isoDateShift(-SIGNUP.maxAge)}
                            max={isoDateShift(-SIGNUP.minAge)}
                            onChange={(v: string) => setFormData({...formData, birthday: v})}
                        />

                        <div>
                            <label className="text-xs font-semibold text-slate-600 mb-1 block">Адрес проживания (город, улица, дом)</label>
                            <div className="relative">
                                <MapPin className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                                <input
                                    type="text"
                                    inputMode="text"
                                    placeholder="Введите город, улицу и дом..."
                                    value={addressQuery}
                                    onChange={e => {
                                        const next = e.target.value;
                                        setAddressQuery(next);
                                        if (selectedAddressValue && next !== selectedAddressValue) {
                                            setSelectedAddressValue(null);
                                            setSelectedGeo(null);
                                        }
                                    }}
                                    autoComplete="off"
                                    autoCorrect="off"
                                    autoCapitalize="none"
                                    spellCheck={false}
                                    name="home-address"
                                    className={`w-full pl-10 pr-4 py-2.5 text-base sm:text-sm bg-slate-50 border ${theme.surface.border} ${theme.radius.btn} focus:outline-none focus:ring-2 ${theme.accent.ring} focus:bg-white transition text-slate-900 placeholder:text-slate-400`}
                                />
                            </div>
                            <p className="text-[11px] text-slate-400 mt-1">Выберите адрес из подсказок.</p>

                            {suggestions.length > 0 && (
                                <div
                                    ref={suggestionsRef}
                                    className={`mt-2 bg-white border ${theme.surface.border} ${theme.radius.card} shadow-lg overflow-hidden`}
                                >
                                    {suggestions.map((s, idx) => (
                                        <button
                                            key={idx}
                                            type="button"
                                            onPointerDown={(e) => {
                                                e.preventDefault();
                                                handleSelectAddress(s);
                                            }}
                                            className="w-full text-left px-4 py-3 text-sm text-slate-700 hover:bg-slate-50 transition border-b border-slate-50 last:border-0 focus:outline-none"
                                        >
                                            {s.value}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div>
                            <label className="text-xs font-semibold text-slate-600 mb-1 block">Пароль</label>
                            <div className="relative">
                                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                <input
                                    type={showPass ? 'text' : 'password'}
                                    placeholder="••••••••"
                                    value={formData.password}
                                    maxLength={SIGNUP.passwordMax}
                                    onChange={e => setFormData({...formData, password: e.target.value})}
                                    className={`w-full pl-10 pr-10 py-2.5 text-sm bg-slate-50 border ${theme.surface.border} ${theme.radius.btn} focus:outline-none focus:ring-2 ${theme.accent.ring} focus:bg-white transition text-slate-900 placeholder:text-slate-400`}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPass(!showPass)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none"
                                >
                                    {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                            </div>
                            <p className="text-[11px] text-slate-400 mt-1.5 flex flex-wrap gap-x-2 gap-y-0.5">
                                {hints.map((h) => (
                                    <span key={h.text} className={h.ok ? 'text-emerald-600' : undefined}>
                                        {h.ok ? '✓' : '·'} {h.text}
                                    </span>
                                ))}
                            </p>
                        </div>

                        <label className="flex items-start gap-2.5 pt-1 cursor-pointer">
                            <input
                                type="checkbox"
                                checked={acceptedTerms}
                                onChange={(e) => {
                                    setAcceptedTerms(e.target.checked);
                                    if (e.target.checked) setFieldError('');
                                }}
                                className="mt-1 w-4 h-4 shrink-0 accent-[#5C4B7A] cursor-pointer"
                            />
                            <span className="text-xs text-slate-600 leading-relaxed">
                                Я принимаю{' '}
                                <button
                                    type="button"
                                    onClick={() => onOpenLegal?.('rules')}
                                    className={`${theme.accent.text} font-semibold underline underline-offset-2 bg-transparent border-none p-0 cursor-pointer`}
                                >
                                    Правила сообщества
                                </button>
                                {' '}и{' '}
                                <button
                                    type="button"
                                    onClick={() => onOpenLegal?.('privacy')}
                                    className={`${theme.accent.text} font-semibold underline underline-offset-2 bg-transparent border-none p-0 cursor-pointer`}
                                >
                                    Политику конфиденциальности
                                </button>
                            </span>
                        </label>
                        <Button type="submit" variant="primary" className="w-full mt-2" disabled={!acceptedTerms}>
                            Зарегистрироваться
                        </Button>
                        <p className="text-center text-xs text-slate-500 mt-4">
                            Уже есть аккаунт?{' '}
                            <button
                                type="button"
                                onClick={() => setPage('login')}
                                className={`${theme.accent.text} font-bold hover:underline bg-transparent border-none p-0 cursor-pointer focus:outline-none`}
                            >
                                Войти
                            </button>
                        </p>
                    </form>
                </Card>
            </Container>
        </div>
    );
}

function FormInput({ label, type = 'text', icon: Icon, placeholder, value, onChange, maxLength, min, max }: any) {
    const isDateTime = type === 'date' || type === 'time';
    return (
        <div className="min-w-0 w-full">
            <label className="text-xs font-semibold text-slate-600 mb-1 block">{label}</label>
            <div className="relative w-full min-w-0">
                {!isDateTime && Icon ? (
                    <Icon className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                ) : null}
                <input
                    type={type}
                    placeholder={placeholder}
                    value={value}
                    maxLength={maxLength}
                    min={min}
                    max={max}
                    onChange={(e: any) => onChange(e.target.value)}
                    className={`w-full min-w-0 max-w-full ${isDateTime ? 'px-3.5' : 'pl-10 pr-4'} py-2.5 text-sm bg-slate-50 border ${theme.surface.border} ${theme.radius.btn} focus:outline-none focus:ring-2 ${theme.accent.ring} focus:bg-white transition text-slate-900 placeholder:text-slate-400`}
                />
            </div>
        </div>
    );
}
