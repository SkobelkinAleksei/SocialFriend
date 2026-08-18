import React, { useEffect, useMemo, useState } from 'react';
import { ChevronDown, ChevronUp, Search, X } from 'lucide-react';
import type { Msg } from '@/features/chat/useChatActions';
import { isUnreadMarker } from '@/features/chat/chatUnread';

function searchableText(m: Msg): string {
    const poll = m.poll;
    const pollText = poll
        ? `${poll.question} ${(poll.options || []).map((option) => option.text).join(' ')}`
        : '';
    return `${m.text || ''} ${pollText}`.toLowerCase();
}

export default function ChatSearchBar({
    messages,
    onJump,
    onClose,
}: {
    messages: Msg[];
    onJump: (id: string) => void;
    onClose: () => void;
}) {
    const [query, setQuery] = useState('');
    const [index, setIndex] = useState(0);

    const matches = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (q.length < 1) return [];
        return messages.filter((m) => {
            if (m.isSystem || m.isDeleted || isUnreadMarker(m.id)) return false;
            return searchableText(m).includes(q);
        });
    }, [messages, query]);

    useEffect(() => {
        setIndex(0);
        if (matches[0]) onJump(matches[0].id);
    }, [query]);

    const go = (dir: number) => {
        if (matches.length === 0) return;
        const next = (index + dir + matches.length) % matches.length;
        setIndex(next);
        onJump(matches[next].id);
    };

    return (
        <div className="px-4 py-2 border-b border-slate-100 bg-white flex items-center gap-2 shrink-0">
            <Search className="w-4 h-4 text-slate-400 shrink-0" />
            <input
                autoFocus
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => {
                    if (e.key === 'Enter') go(e.shiftKey ? -1 : 1);
                    if (e.key === 'Escape') onClose();
                }}
                placeholder="Поиск по переписке"
                className="flex-1 text-sm bg-transparent focus:outline-none text-slate-800 placeholder:text-slate-400"
            />
            <span className="text-[11px] text-slate-400 tabular-nums shrink-0">
                {query.trim() ? `${matches.length ? index + 1 : 0}/${matches.length}` : ''}
            </span>
            <button type="button" disabled={!matches.length} onClick={() => go(-1)} className="p-1 rounded-md text-slate-500 hover:bg-slate-100 disabled:opacity-30">
                <ChevronUp className="w-4 h-4" />
            </button>
            <button type="button" disabled={!matches.length} onClick={() => go(1)} className="p-1 rounded-md text-slate-500 hover:bg-slate-100 disabled:opacity-30">
                <ChevronDown className="w-4 h-4" />
            </button>
            <button type="button" onClick={onClose} className="p-1 rounded-md text-slate-400 hover:bg-slate-100">
                <X className="w-4 h-4" />
            </button>
        </div>
    );
}
