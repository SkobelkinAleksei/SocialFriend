import React, {useEffect, useState} from 'react';
import { useChat, isPersonalGroupRoom } from '@/features/chat/ChatContext';
import { theme } from '@/shared/ui/theme';
import { X, MessageCircle, Users, Search, Check } from 'lucide-react';
import Button from '@/shared/ui/Button';
import { getAvatarUrl } from '@/shared/utils/navigation';
import { useAppBackHandler } from '@/shared/hooks/useAppBackHandler';

interface ForwardTarget {
    recipientId?: number;
    chatId?: number;
}

interface ForwardModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSelectChats: (targets: ForwardTarget[], comment?: string) => void;
}

export default function ForwardModal({ isOpen, onClose, onSelectChats }: ForwardModalProps) {
    const { chats, eventRooms, personalGroups } = useChat();
    const [tab, setTab] = useState<'personal' | 'events'>('personal');
    const [search, setSearch] = useState('');

    const [selectedTargets, setSelectedTargets] = useState<string[]>([]);
    const [commentText, setCommentText] = useState('');

    useEffect(() => {
        if (!isOpen) {
            setSelectedTargets([]);
            setCommentText('');
        }
    }, [isOpen]);

    useEffect(() => {
        if (!isOpen) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [isOpen, onClose]);

    useAppBackHandler(isOpen, onClose);

    if (!isOpen) return null;

    const toggleTarget = (type: 'recipient' | 'chat', id: number) => {
        const key = `${type}:${id}`;
        setSelectedTargets((prev) =>
            prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key]
        );
    };

    const handleConfirmForward = () => {
        if (selectedTargets.length === 0) return;

        const targets: ForwardTarget[] = selectedTargets.map((key) => {
            const [type, idStr] = key.split(':');
            const id = Number(idStr);
            return type === 'recipient' ? { recipientId: id } : { chatId: id };
        });

        onSelectChats(targets, commentText.trim());

        setSelectedTargets([]);
        setCommentText('');
        onClose();
    };

    const q = search.toLowerCase();
    const groupRows = (personalGroups || []).filter((r) => r && (r.title || '').toLowerCase().includes(q));
    const dmRows = (chats || []).filter((c) => c?.name?.toLowerCase().includes(q));

    return (
        <div
            className="fixed inset-0 z-[90] bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4"
            onClick={(e) => e.stopPropagation()}
        >
            <div
                className="w-full max-w-md bg-white border border-slate-200 rounded-2xl shadow-xl flex flex-col max-h-[500px]"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
                    <div className="font-semibold text-slate-900 text-base">Переслать в диалоги</div>
                    <button onClick={onClose} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100 transition">
                        <X className="w-4 h-4" />
                    </button>
                </div>

                <div className="flex gap-1 p-1 bg-slate-100 mx-5 mt-4 rounded-xl text-xs font-medium">
                    <button
                        onClick={() => setTab('personal')}
                        className={`flex-1 py-1.5 rounded-lg transition flex items-center justify-center gap-1 ${
                            tab === 'personal' ? 'bg-[#FFFCFA] text-[#5C4B7A]' : 'text-[#8A8494]'
                        }`}
                    >
                        <MessageCircle className="w-3.5 h-3.5" /> Личные
                    </button>
                    <button
                        onClick={() => setTab('events')}
                        className={`flex-1 py-1.5 rounded-lg transition flex items-center justify-center gap-1 ${
                            tab === 'events' ? 'bg-[#FFFCFA] text-[#5C4B7A]' : 'text-[#8A8494]'
                        }`}
                    >
                        <Users className="w-3.5 h-3.5" /> События
                    </button>
                </div>

                <div className="relative mx-5 mt-3 mb-2">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="Поиск диалога..."
                        className="w-full pl-9 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/30 transition"
                    />
                </div>

                <div className="flex-1 overflow-y-auto px-3 pb-2 space-y-0.5">
                    {tab === 'personal' ? (
                        <>
                            {groupRows.map((r) => {
                                const isSelected = selectedTargets.includes(`chat:${r.id}`);
                                return (
                                    <button
                                        key={`g-${r.id}`}
                                        onClick={() => toggleTarget('chat', Number(r.id))}
                                        className={`w-full text-left px-3 py-2.5 rounded-xl flex items-center justify-between transition ${
                                            isSelected ? 'bg-[#EDE6F5]' : 'hover:bg-slate-50'
                                        }`}
                                    >
                                        <div className="flex items-center gap-3 min-w-0">
                                            {r.avatarUrl ? (
                                                <img src={getAvatarUrl(null, r.avatarUrl)} alt="" className="w-8 h-8 rounded-full object-cover shrink-0" />
                                            ) : (
                                                <div className="w-8 h-8 rounded-full bg-[#EDE6F5] flex items-center justify-center text-[#5C4B7A] shrink-0">
                                                    <Users className="w-4 h-4" />
                                                </div>
                                            )}
                                            <div className="min-w-0">
                                                <div className="text-xs font-medium text-slate-800 truncate">{r.title}</div>
                                                <div className="text-[10px] font-semibold uppercase tracking-wide text-[#5C4B7A]">Группа</div>
                                            </div>
                                        </div>
                                        <div className={`w-4 h-4 rounded border flex items-center justify-center transition-all shrink-0 ${
                                            isSelected ? 'border-[#5C4B7A] bg-[#5C4B7A] text-white' : 'border-slate-300 bg-white'
                                        }`}>
                                            {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                                        </div>
                                    </button>
                                );
                            })}
                            {dmRows.map((c) => {
                                const isSelected = selectedTargets.includes(`recipient:${c.id}`);
                                return (
                                    <button
                                        key={c.id}
                                        onClick={() => toggleTarget('recipient', Number(c.id))}
                                        className={`w-full text-left px-3 py-2.5 rounded-xl flex items-center justify-between transition ${
                                            isSelected ? 'bg-[#EDE6F5]' : 'hover:bg-slate-50'
                                        }`}
                                    >
                                        <div className="flex items-center gap-3 min-w-0">
                                            <div className="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-600 font-bold text-xs shrink-0">
                                                {c.name.substring(0, 2).toUpperCase()}
                                            </div>
                                            <div className="text-xs font-medium text-slate-800 truncate">{c.name}</div>
                                        </div>
                                        <div className={`w-4 h-4 rounded border flex items-center justify-center transition-all shrink-0 ${
                                            isSelected ? 'border-[#5C4B7A] bg-[#5C4B7A] text-white' : 'border-slate-300 bg-white'
                                        }`}>
                                            {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                                        </div>
                                    </button>
                                );
                            })}
                        </>
                    ) : (
                        eventRooms
                            ?.filter((r) => r && !isPersonalGroupRoom(r) && r?.title?.toLowerCase().includes(q))
                            .map((r) => {
                                const isSelected = selectedTargets.includes(`chat:${r.id}`);
                                return (
                                    <button
                                        key={r.id}
                                        onClick={() => toggleTarget('chat', Number(r.id))}
                                        className={`w-full text-left px-3 py-2.5 rounded-xl flex items-center justify-between transition ${
                                            isSelected ? 'bg-[#EDE6F5]' : 'hover:bg-slate-50'
                                        }`}
                                    >
                                        <div className="flex items-center gap-3 min-w-0">
                                            <div className="w-8 h-8 rounded-full bg-[#EDE6F5] flex items-center justify-center text-[#5C4B7A] text-xs shrink-0">
                                                {r.title.substring(0, 2).toUpperCase()}
                                            </div>
                                            <div className="text-xs font-medium text-slate-800 truncate">{r.title}</div>
                                        </div>
                                        <div className={`w-4 h-4 rounded border flex items-center justify-center transition-all shrink-0 ${
                                            isSelected ? 'border-[#5C4B7A] bg-[#5C4B7A] text-white' : 'border-slate-300 bg-white'
                                        }`}>
                                            {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                                        </div>
                                    </button>
                                );
                            })
                    )}
                </div>

                <div className="px-5 py-3 border-t border-slate-100 bg-white">
                    <input
                        type="text"
                        value={commentText}
                        onChange={(e) => setCommentText(e.target.value)}
                        placeholder="Добавить комментарий к пересылке..."
                        className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/30 transition font-medium text-slate-700"
                    />
                </div>

                <div className="p-4 border-t border-slate-100 flex items-center justify-end bg-slate-50/50 rounded-b-2xl">
                    <Button
                        onClick={handleConfirmForward}
                        disabled={selectedTargets.length === 0}
                        size="md"
                        className="w-full font-bold shadow-sm"
                    >
                        Поделиться ({selectedTargets.length})
                    </Button>
                </div>
            </div>
        </div>
    );
}
