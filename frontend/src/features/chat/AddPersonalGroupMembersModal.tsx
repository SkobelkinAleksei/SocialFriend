import React, { useEffect, useMemo, useState } from 'react';
import api from '@/shared/lib/api';
import { useAuth } from '@/shared/context/AuthContext';
import { getAvatarUrl } from '@/shared/utils/navigation';
import { Search, X } from 'lucide-react';

export default function AddPersonalGroupMembersModal({
    chatId,
    excludeUserIds,
    onClose,
    onAdded,
}: {
    chatId: number;
    excludeUserIds: number[];
    onClose: () => void;
    onAdded: () => void;
}) {
    const { user } = useAuth();
    const [friends, setFriends] = useState<any[]>([]);
    const [friendQuery, setFriendQuery] = useState('');
    const [selected, setSelected] = useState<number[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!user?.id) return;
        const load = async () => {
            try {
                const [friendsRes, membersRes] = await Promise.all([
                    api.get(`/api/v1/social/friends/public/${user.id}`, { params: { page: 0, size: 100 } }),
                    api.get(`/api/v1/social/chats/rooms/${chatId}/members`).catch(() => ({ data: [] })),
                ]);
                const alreadyIn = new Set([
                    ...excludeUserIds.map(Number),
                    ...(membersRes.data || []).map((m: any) => Number(m.userId)),
                ]);
                const rows = friendsRes.data || [];
                const ids = rows
                    .map((f: any) => Number(f.userId1) === Number(user.id) ? Number(f.userId2) : Number(f.userId1))
                    .filter((id: number) => id && !alreadyIn.has(id));
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
    }, [user?.id, chatId]);

    const visibleFriends = useMemo(() => {
        const q = friendQuery.trim().toLowerCase();
        if (!q) return friends;
        return friends.filter((f) => `${f.firstName || ''} ${f.lastName || ''}`.toLowerCase().includes(q));
    }, [friends, friendQuery]);

    const toggle = (id: number) => {
        setSelected((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
    };

    const submit = async () => {
        if (selected.length === 0) {
            setError('Выберите хотя бы одного друга');
            return;
        }
        setLoading(true);
        setError('');
        try {
            await api.post(`/api/v1/social/chats/rooms/${chatId}/members`, {
                memberIds: selected,
            }, {
                params: { firstName: user?.firstName || '', lastName: user?.lastName || '' },
            });
            onAdded();
        } catch (e: any) {
            setError(e?.response?.data?.details || 'Не удалось добавить друзей');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-[80] bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4" onClick={onClose}>
            <div className="w-full max-w-md bg-[#FFFCFA] rounded-[28px] shadow-2xl p-6" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-4">
                    <h2 className="myraion-display text-xl text-[#1C1824]">Добавить друзей</h2>
                    <button type="button" onClick={onClose} className="w-8 h-8 rounded-full hover:bg-[#EDE6F5] flex items-center justify-center text-slate-400">
                        <X className="w-4 h-4" />
                    </button>
                </div>
                <div className="relative mb-2">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                        value={friendQuery}
                        onChange={(e) => setFriendQuery(e.target.value)}
                        placeholder="Поиск по имени и фамилии"
                        className="w-full h-10 pl-9 pr-3 rounded-full bg-white border border-[#1C1824]/10 text-sm focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/30"
                    />
                </div>
                <div className="max-h-64 overflow-y-auto space-y-1 mb-4">
                    {friends.length === 0 ? (
                        <p className="text-sm text-slate-400 py-4 text-center">Нет друзей, которых можно добавить</p>
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
                {error ? <p className="text-xs text-[#B85C5C] mb-3">{error}</p> : null}
                <button
                    type="button"
                    disabled={loading}
                    onClick={submit}
                    className="w-full h-11 rounded-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white text-sm disabled:opacity-50"
                >
                    {loading ? 'Добавляем…' : 'Добавить'}
                </button>
            </div>
        </div>
    );
}
