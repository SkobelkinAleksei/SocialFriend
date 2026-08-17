import React, { useState } from 'react';
import { BarChart2, Check, X } from 'lucide-react';
import api from '@/shared/lib/api';
import { getAvatarUrl, openNeighborProfile } from '@/shared/utils/navigation';
import { formatClockTime } from '@/features/events/reputationWindow';
import type { ChatPoll } from '@/features/chat/chatPoll';

export default function ChatPollCard({
    poll,
    mine,
    onUpdated,
}: {
    poll: ChatPoll;
    mine?: boolean;
    onUpdated: (next: ChatPoll) => void;
}) {
    const [votersFor, setVotersFor] = useState<number | null>(null);
    const [busy, setBusy] = useState(false);
    const total = Math.max(poll.totalVotes, 1);

    const vote = async (optionId: number) => {
        if (busy || poll.closed) return;
        setBusy(true);
        try {
            const res = await api.post(`/api/v1/social/chats/polls/${poll.id}/vote`, { optionIds: [optionId] });
            if (res.data?.poll) onUpdated(res.data.poll);
        } catch (e) {
            console.error('Не удалось проголосовать', e);
        } finally {
            setBusy(false);
        }
    };

    const openVoters = votersFor != null ? poll.options.find((o) => o.id === votersFor) : null;

    return (
        <div className={`rounded-2xl border px-3 py-3 min-w-[220px] ${mine ? 'border-white/20 bg-white/10' : 'border-slate-100 bg-white'}`}>
            <div className="flex items-start gap-2 mb-2.5">
                <BarChart2 className={`w-4 h-4 mt-0.5 shrink-0 ${mine ? 'text-white/80' : 'text-[#5C4B7A]'}`} />
                <div className={`text-sm font-semibold leading-snug min-w-0 [overflow-wrap:anywhere] break-all ${mine ? 'text-white' : 'text-slate-900'}`}>{poll.question}</div>
            </div>
            <div className="space-y-1.5">
                {poll.options.map((option) => {
                    const pct = poll.totalVotes === 0 ? 0 : Math.round((option.votes / total) * 100);
                    const voters = option.voters || [];
                    return (
                        <div key={option.id} className="relative z-0 hover:z-20 group/option">
                            <button
                                type="button"
                                disabled={busy || poll.closed}
                                onClick={() => vote(option.id)}
                                className={`relative w-full text-left rounded-xl border pointer-events-auto overflow-hidden ${
                                    option.selected
                                        ? (mine ? 'border-white/70' : 'border-[#5C4B7A]')
                                        : (mine ? 'border-white/15' : 'border-slate-200')
                                } hover:brightness-[0.98]`}
                            >
                                <div
                                    className={`absolute inset-y-0 left-0 ${mine ? 'bg-white/20' : 'bg-[#EDE6F5]'}`}
                                    style={{ width: `${pct}%` }}
                                />
                                <div className="relative flex items-center gap-2 px-2.5 py-2">
                                    <span className={`w-4 h-4 ${poll.multiple ? 'rounded-md' : 'rounded-full'} border flex items-center justify-center shrink-0 ${
                                        option.selected
                                            ? (mine ? 'bg-white text-[#5C4B7A] border-white' : 'bg-[#5C4B7A] text-white border-[#5C4B7A]')
                                            : (mine ? 'border-white/40' : 'border-slate-300')
                                    }`}>
                                        {option.selected && <Check className="w-3 h-3" />}
                                    </span>
                                    <span className={`flex-1 text-xs font-medium min-w-0 truncate ${mine ? 'text-white' : 'text-slate-800'}`}>{option.text}</span>
                                    <span className={`text-[11px] tabular-nums font-semibold shrink-0 ${mine ? 'text-white/80' : 'text-slate-500'}`}>
                                        {pct}%
                                    </span>
                                </div>
                            </button>
                            {!poll.anonymous && voters.length > 0 && (
                                <button
                                    type="button"
                                    title="Кто проголосовал"
                                    className={`absolute left-7 top-0 -translate-y-1/2 z-30 hidden group-hover/option:flex items-center -space-x-1.5 px-1.5 py-0.5 rounded-full backdrop-blur-md shadow-sm border pointer-events-auto ${
                                        mine ? 'bg-white/25 border-white/30' : 'bg-white/80 border-white/70'
                                    }`}
                                    onClick={(e) => {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        setVotersFor(option.id);
                                    }}
                                >
                                    {voters.slice(0, 3).map((voter) => (
                                        <img
                                            key={voter.userId}
                                            src={getAvatarUrl(voter.userId, voter.avatarUrl)}
                                            alt={`${voter.firstName || ''} ${voter.lastName || ''}`.trim()}
                                            className="w-4 h-4 rounded-full object-cover ring-1 ring-white"
                                        />
                                    ))}
                                    {voters.length > 3 && (
                                        <span className={`w-4 h-4 rounded-full text-[8px] font-bold flex items-center justify-center ring-1 ring-white ${mine ? 'bg-white/40 text-white' : 'bg-slate-200 text-slate-600'}`}>
                                            +{voters.length - 3}
                                        </span>
                                    )}
                                </button>
                            )}
                        </div>
                    );
                })}
            </div>
            {poll.closesAt && !poll.closed ? (
                <div className={`text-[10px] mt-2 ${mine ? 'text-white/70' : 'text-slate-400'}`}>
                    До {formatClockTime(poll.closesAt)}
                </div>
            ) : null}
            {openVoters && (
                <div className="fixed inset-0 z-[80] bg-black/30 flex items-center justify-center p-4 pointer-events-auto" onClick={() => setVotersFor(null)}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-4" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-between mb-3">
                            <div className="font-semibold text-sm text-slate-900">{openVoters.text}</div>
                            <button type="button" onClick={() => setVotersFor(null)} className="p-1 rounded-md text-slate-400 hover:bg-slate-100">
                                <X className="w-4 h-4" />
                            </button>
                        </div>
                        {(openVoters.voters || []).length === 0 ? (
                            <div className="text-xs text-slate-400 py-4 text-center">Пока никто не выбрал этот вариант</div>
                        ) : (
                            <div className="space-y-1 max-h-64 overflow-y-auto">
                                {(openVoters.voters || []).map((v) => {
                                    const fullName = `${v.firstName || ''} ${v.lastName || ''}`.trim() || 'Участник';
                                    return (
                                        <button
                                            key={v.userId}
                                            type="button"
                                            onClick={() => {
                                                setVotersFor(null);
                                                openNeighborProfile(v.userId, v.firstName);
                                            }}
                                            className="w-full flex items-center gap-2 px-1.5 py-1.5 rounded-xl text-left hover:bg-[#EDE6F5] transition"
                                        >
                                            <img src={getAvatarUrl(v.userId, v.avatarUrl)} alt="" className="w-8 h-8 rounded-full object-cover shrink-0" />
                                            <span className="text-sm font-medium text-[#5C4B7A] hover:underline">{fullName}</span>
                                        </button>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
