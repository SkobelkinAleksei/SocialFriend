import React from 'react';
import { MessageSquare } from 'lucide-react';

export default function SharedEventCard({
  text,
  mine,
}: {
  text: string;
  mine?: boolean;
}) {
  const match = text.match(/\[SHARE_EVENT:(\d+)\]/);
  const sharedEventId = match ? Number(match[1]) : null;
  const titleMatch = text.match(/Приглашаю на встречу:\s*["']?([^"'\n]+)["']?/);
  const eventTitle = titleMatch ? titleMatch[1] : `Встреча #${sharedEventId}`;
  const parts = text.split('\n');
  const userComment = parts.length > 1 ? parts.slice(1).join('\n').trim() : '';

  return (
    <div className="space-y-2 my-1 select-none pointer-events-auto flex flex-col items-start w-full">
      <div className="bg-white border border-slate-200/80 rounded-2xl p-3.5 flex flex-col gap-2.5 max-w-[260px] shadow-sm">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[#8A76B0] to-[#5C4B7A] flex items-center justify-center text-white shrink-0 shadow-sm">
            <MessageSquare className="w-4 h-4 stroke-[2.5]" />
          </div>
          <div className="min-w-0 flex-1">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider block leading-none mb-1">
              Событие: {eventTitle}
            </span>
          </div>
        </div>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            if (sharedEventId) {
              window.dispatchEvent(new CustomEvent('openSharedEventModal', { detail: { eventId: sharedEventId } }));
            }
          }}
          className="w-full py-2 px-3 rounded-xl bg-white border border-slate-200 hover:border-[#5C4B7A]/40 hover:bg-[#EDE6F5] text-[#5C4B7A] font-bold text-xs transition active:scale-95 cursor-pointer shadow-sm text-center"
        >
          Открыть событие →
        </button>
      </div>
      {userComment ? (
        <div className={`text-xs font-medium mt-1 px-1 break-words max-w-sm ${mine ? 'text-white/90' : 'text-slate-800'}`}>
          {userComment}
        </div>
      ) : null}
    </div>
  );
}
