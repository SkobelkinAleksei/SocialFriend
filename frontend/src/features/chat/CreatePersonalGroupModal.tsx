import React, { useEffect, useMemo, useRef, useState } from 'react';
import api, { uploadChatPhoto } from '@/shared/lib/api';
import { useAuth } from '@/shared/context/AuthContext';
import { getAvatarUrl } from '@/shared/utils/navigation';
import { Camera, ChevronDown, Search, X } from 'lucide-react';

type Policy = 'OWNER_ONLY' | 'EVERYONE';

export default function CreatePersonalGroupModal({
    onClose,
    onCreated,
}: {
    onClose: () => void;
    onCreated: (room: any) => void;
}) {
    const { user } = useAuth();
    const [title, setTitle] = useState('');
    const [avatarUrl, setAvatarUrl] = useState('');
    const [friends, setFriends] = useState<any[]>([]);
    const [friendQuery, setFriendQuery] = useState('');
    const [selected, setSelected] = useState<number[]>([]);
    const [addPolicy, setAddPolicy] = useState<Policy>('OWNER_ONLY');
    const [renamePolicy, setRenamePolicy] = useState<Policy>('OWNER_ONLY');
    const [avatarPolicy, setAvatarPolicy] = useState<Policy>('OWNER_ONLY');
    const [settingsOpen, setSettingsOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState('');
    const avatarInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (!user?.id) return;
        const load = async () => {
            try {
                const res = await api.get(`/api/v1/social/friends/public/${user.id}`, { params: { page: 0, size: 100 } });
                const rows = res.data || [];
                const ids = rows.map((f: any) => Number(f.userId1) === Number(user.id) ? Number(f.userId2) : Number(f.userId1)).filter(Boolean);
                const profiles = await Promise.all(ids.map((id: number) =>
                    api.get(`/api/v1/social/users/${id}/profile`).then((r) => r.data).catch(() => null)
                ));
                setFriends(profiles.filter(Boolean).map((p: any) => ({
                    userId: Number(p.userId || p.id),
                    firstName: p.firstName || 'Сосед',
                    lastName: p.lastName || '',
                    avatarUrl: p.avatarUrl,
                })));
            } catch (e) {
                console.error(e);
            }
        };
        void load();
    }, [user?.id]);

    const visibleFriends = useMemo(() => {
        const q = friendQuery.trim().toLowerCase();
        if (!q) return friends;
        return friends.filter((f) => `${f.firstName || ''} ${f.lastName || ''}`.toLowerCase().includes(q));
    }, [friends, friendQuery]);

    const toggle = (id: number) => {
        setSelected((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
    };

    const onPickAvatar = async (file?: File) => {
        if (!file) return;
        setUploading(true);
        setError('');
        try {
            const url = await uploadChatPhoto(file);
            setAvatarUrl(url);
        } catch (e: any) {
            setError(e?.response?.data?.details || 'Не удалось загрузить аватар');
        } finally {
            setUploading(false);
        }
    };

    const submit = async () => {
        if (!title.trim()) {
            setError('Укажите название');
            return;
        }
        if (selected.length === 0) {
            setError('Выберите хотя бы одного друга');
            return;
        }
        setLoading(true);
        setError('');
        try {
            const res = await api.post('/api/v1/social/chats/rooms/personal', {
                title: title.trim(),
                memberIds: selected,
                addMembersPolicy: addPolicy,
                renamePolicy,
                avatarPolicy,
                avatarUrl: avatarUrl || null,
            }, {
                params: { firstName: user?.firstName || '', lastName: user?.lastName || '' },
            });
            onCreated(res.data);
        } catch (e: any) {
            setError(e?.response?.data?.details || 'Не удалось создать групповой чат');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-[80] bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4" onClick={onClose}>
            <div className="w-full max-w-md bg-[#FFFCFA] rounded-[28px] shadow-2xl p-6" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-4">
                    <h2 className="myraion-display text-xl text-[#1C1824]">Новый групповой чат</h2>
                    <button type="button" onClick={onClose} className="w-8 h-8 rounded-full hover:bg-[#EDE6F5] flex items-center justify-center text-slate-400">
                        <X className="w-4 h-4" />
                    </button>
                </div>
                <div className="flex items-center gap-3 mb-4">
                    <button
                        type="button"
                        onClick={() => avatarInputRef.current?.click()}
                        className="relative w-14 h-14 rounded-full bg-[#EDE6F5] overflow-hidden shrink-0"
                        title="Аватар чата"
                    >
                        {avatarUrl ? (
                            <img src={getAvatarUrl(null, avatarUrl)} alt="" className="w-full h-full object-cover" />
                        ) : (
                            <span className="w-full h-full flex items-center justify-center text-[#5C4B7A]">
                                <Camera className="w-5 h-5" />
                            </span>
                        )}
                        <span className="absolute inset-x-0 bottom-0 bg-black/35 text-white text-[9px] py-0.5">{uploading ? '…' : 'Фото'}</span>
                    </button>
                    <input
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="Название чата"
                        maxLength={80}
                        className="flex-1 h-11 px-4 rounded-full bg-white border border-[#1C1824]/10 text-sm focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/30"
                    />
                    <input
                        ref={avatarInputRef}
                        type="file"
                        accept="image/jpeg,image/png,image/webp,image/gif"
                        className="hidden"
                        onChange={(e) => { void onPickAvatar(e.target.files?.[0]); e.target.value = ''; }}
                    />
                </div>
                <div className="text-xs font-semibold text-[#6B645C] uppercase tracking-wide mb-2">Друзья</div>
                <div className="relative mb-2">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                        value={friendQuery}
                        onChange={(e) => setFriendQuery(e.target.value)}
                        placeholder="Поиск по имени и фамилии"
                        className="w-full h-10 pl-9 pr-3 rounded-full bg-white border border-[#1C1824]/10 text-sm focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/30"
                    />
                </div>
                <div className="max-h-44 overflow-y-auto space-y-1 mb-4">
                    {friends.length === 0 ? (
                        <p className="text-sm text-slate-400 py-4 text-center">Пока нет друзей, которых можно добавить</p>
                    ) : visibleFriends.length === 0 ? (
                        <p className="text-sm text-slate-400 py-4 text-center">Никого не нашли по этому запросу</p>
                    ) : visibleFriends.map((f) => (
                        <label key={f.userId} className="flex items-center gap-3 px-2 py-2 rounded-xl hover:bg-[#EDE6F5] cursor-pointer">
                            <input type="checkbox" checked={selected.includes(f.userId)} onChange={() => toggle(f.userId)} className="accent-[#5C4B7A]" />
                            <img src={getAvatarUrl(f.userId, f.avatarUrl)} alt="" className="w-8 h-8 rounded-full object-cover" />
                            <span className="text-sm text-[#1C1824]">{f.firstName} {f.lastName}</span>
                        </label>
                    ))}
                </div>
                <div className="relative mb-4">
                    <button
                        type="button"
                        onClick={() => setSettingsOpen((v) => !v)}
                        className="w-full h-11 rounded-full bg-white border border-[#1C1824]/10 text-sm text-[#1C1824] flex items-center justify-between px-4"
                    >
                        <span>Настройки чата</span>
                        <ChevronDown className={`w-4 h-4 text-slate-400 transition-transform ${settingsOpen ? 'rotate-180' : ''}`} />
                    </button>
                    {settingsOpen && (
                        <div className="absolute left-0 right-0 top-12 z-10 bg-white border border-slate-200 shadow-xl rounded-2xl p-3 space-y-3">
                            <div>
                                <div className="text-xs font-semibold text-[#6B645C] mb-1.5">Кто может добавлять участников</div>
                                <div className="flex gap-2">
                                    <PolicyChip active={addPolicy === 'OWNER_ONLY'} onClick={() => setAddPolicy('OWNER_ONLY')}>Только я</PolicyChip>
                                    <PolicyChip active={addPolicy === 'EVERYONE'} onClick={() => setAddPolicy('EVERYONE')}>Все участники</PolicyChip>
                                </div>
                            </div>
                            <div>
                                <div className="text-xs font-semibold text-[#6B645C] mb-1.5">Кто может менять название</div>
                                <div className="flex gap-2">
                                    <PolicyChip active={renamePolicy === 'OWNER_ONLY'} onClick={() => setRenamePolicy('OWNER_ONLY')}>Только я</PolicyChip>
                                    <PolicyChip active={renamePolicy === 'EVERYONE'} onClick={() => setRenamePolicy('EVERYONE')}>Все участники</PolicyChip>
                                </div>
                            </div>
                            <div>
                                <div className="text-xs font-semibold text-[#6B645C] mb-1.5">Кто может менять аватарку</div>
                                <div className="flex gap-2">
                                    <PolicyChip active={avatarPolicy === 'OWNER_ONLY'} onClick={() => setAvatarPolicy('OWNER_ONLY')}>Только я</PolicyChip>
                                    <PolicyChip active={avatarPolicy === 'EVERYONE'} onClick={() => setAvatarPolicy('EVERYONE')}>Все участники</PolicyChip>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
                {error ? <p className="text-xs text-[#B85C5C] mb-3">{error}</p> : null}
                <button
                    type="button"
                    disabled={loading || uploading}
                    onClick={submit}
                    className="w-full h-11 rounded-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white text-sm disabled:opacity-50"
                >
                    {loading ? 'Создаём…' : 'Создать чат'}
                </button>
            </div>
        </div>
    );
}

function PolicyChip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className={`flex-1 h-9 rounded-full text-xs transition ${active ? 'bg-[#5C4B7A] text-white' : 'bg-white border border-[#1C1824]/10 text-[#1C1824]'}`}
        >
            {children}
        </button>
    );
}
