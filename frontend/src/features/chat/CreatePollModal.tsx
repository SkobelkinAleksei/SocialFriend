import React, { useState } from 'react';
import { Plus, X } from 'lucide-react';
import Button from '@/shared/ui/Button';
import api from '@/shared/lib/api';
import { showAppInfoToast } from '@/shared/utils/appToast';
import type { ChatPoll } from '@/features/chat/chatPoll';

export default function CreatePollModal({
    chatId,
    firstName,
    lastName,
    onClose,
    onCreated,
}: {
    chatId: number;
    firstName?: string;
    lastName?: string;
    onClose: () => void;
    onCreated: (message: any, poll: ChatPoll) => void;
}) {
    const [question, setQuestion] = useState('');
    const [options, setOptions] = useState(['', '']);
    const [anonymous, setAnonymous] = useState(true);
    const [multiple, setMultiple] = useState(false);
    const [busy, setBusy] = useState(false);

    const create = async () => {
        const cleanOptions = options.map((o) => o.trim()).filter(Boolean);
        if (!question.trim()) {
            showAppInfoToast('Голосование', 'Введите вопрос голосования');
            return;
        }
        if (cleanOptions.length < 2) {
            showAppInfoToast('Голосование', 'Нужно минимум два варианта ответа');
            return;
        }
        const unique = new Set(cleanOptions.map((o) => o.toLowerCase()));
        if (unique.size < cleanOptions.length) {
            showAppInfoToast('Голосование', 'Варианты не должны повторяться. Укажите минимум два разных варианта');
            return;
        }
        if (busy) return;
        setBusy(true);
        try {
            const res = await api.post(`/api/v1/social/chats/room/${chatId}/polls`, {
                question: question.trim(),
                options: cleanOptions,
                anonymous,
                multiple,
            }, { params: { firstName: firstName || '', lastName: lastName || '' } });
            onCreated(res.data, res.data.poll);
            onClose();
        } catch (e: any) {
            const reason = e?.response?.data?.details || e?.response?.data?.detail || e?.response?.data?.message || 'Не удалось создать голосование';
            showAppInfoToast('Голосование', reason);
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="fixed inset-0 z-[80] bg-black/30 flex items-center justify-center p-4" onClick={onClose}>
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-5" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-4">
                    <h3 className="myraion-display text-xl text-[#1C1824]">Новое голосование</h3>
                    <button type="button" onClick={onClose} className="p-1 rounded-md text-slate-400 hover:bg-slate-100">
                        <X className="w-4 h-4" />
                    </button>
                </div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Вопрос</label>
                <input
                    autoFocus
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    maxLength={255}
                    placeholder="О чём голосуем?"
                    className="w-full px-3 py-2.5 mb-4 text-sm bg-[#EFEAF6] border border-[#1C1824]/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/20"
                />
                <div className="text-xs font-semibold text-slate-500 mb-1">Варианты</div>
                <div className="space-y-2 mb-3">
                    {options.map((option, i) => (
                        <div key={i} className="flex items-center gap-2">
                            <input
                                value={option}
                                onChange={(e) => setOptions(options.map((item, idx) => idx === i ? e.target.value : item))}
                                maxLength={100}
                                placeholder={`Вариант ${i + 1}`}
                                className="flex-1 px-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/20"
                            />
                            {options.length > 2 && (
                                <button type="button" onClick={() => setOptions(options.filter((_, idx) => idx !== i))} className="p-1 text-slate-400 hover:text-red-500">
                                    <X className="w-4 h-4" />
                                </button>
                            )}
                        </div>
                    ))}
                </div>
                {options.length < 10 && (
                    <button type="button" onClick={() => setOptions([...options, ''])} className="text-xs font-semibold text-[#5C4B7A] inline-flex items-center gap-1 mb-4">
                        <Plus className="w-3.5 h-3.5" /> Добавить вариант
                    </button>
                )}
                <label className="flex items-center justify-between py-2">
                    <span className="text-sm text-slate-700">Анонимное голосование</span>
                    <input type="checkbox" checked={anonymous} onChange={(e) => setAnonymous(e.target.checked)} className="rounded text-[#5C4B7A]" />
                </label>
                <label className="flex items-center justify-between py-2 mb-3">
                    <span className="text-sm text-slate-700">Можно выбрать несколько вариантов</span>
                    <input type="checkbox" checked={multiple} onChange={(e) => setMultiple(e.target.checked)} className="rounded text-[#5C4B7A]" />
                </label>
                <p className="text-[11px] text-slate-400 mb-3 leading-snug">
                    Вопрос — до 255 символов. Вариантов от 2 до 10, каждый до 100 символов, без повторов.
                </p>
                <Button
                    type="button"
                    disabled={busy || !question.trim() || options.filter((o) => o.trim()).length < 2}
                    onClick={create}
                    className="w-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white"
                >
                    {busy ? 'Создание…' : 'Опубликовать'}
                </Button>
            </div>
        </div>
    );
}
