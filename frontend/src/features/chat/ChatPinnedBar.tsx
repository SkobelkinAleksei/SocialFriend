import React, { useEffect, useState } from 'react';
import { Pin, X, List } from 'lucide-react';
import { stripMentionMarkers } from '@/features/chat/chatMentions';

export type PinnedMessageItem = {
  id: number;
  messageId: number;
  content?: string;
  senderName?: string;
  photos?: string[];
};

function pinPreview(pin: PinnedMessageItem): string {
  return stripMentionMarkers(pin.content) || (pin.photos?.length ? 'Фото' : 'Сообщение');
}

export default function ChatPinnedBar({
  pins,
  canManage,
  onJump,
  onUnpin,
}: {
  pins: PinnedMessageItem[];
  canManage: boolean;
  onJump: (messageId: number) => void;
  onUnpin: (messageId: number) => void;
}) {
  const [open, setOpen] = useState(false);
  const [offset, setOffset] = useState(0);
  const pinKey = pins.map((p) => p.messageId).join(',');

  useEffect(() => {
    setOpen(false);
    setOffset((o) => (pins.length ? o % pins.length : 0));
  }, [pinKey, pins.length]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open]);

  if (!pins.length) return null;
  const current = pins[offset] || pins[0];
  const preview = pinPreview(current);

  const isPrimaryPointer = (e: React.PointerEvent) => e.button === 0 || e.pointerType !== 'mouse';

  const goToCurrent = (e: React.PointerEvent) => {
    if (!isPrimaryPointer(e)) return;
    e.preventDefault();
    e.stopPropagation();
    setOpen(false);
    onJump(current.messageId);
    if (pins.length > 1) setOffset((o) => (o + 1) % pins.length);
  };

  return (
    <div className="relative z-30 shrink-0 isolate">
      <div className="relative z-20 border-b border-slate-100 bg-[#EDE6F5]/55">
        <div className="w-full pl-3 pr-1 py-1 flex items-center gap-0.5">
          <button
            type="button"
            onPointerDown={goToCurrent}
            className="min-w-0 flex-1 py-1.5 flex items-center gap-2 text-left rounded-lg hover:bg-white/50"
          >
            <Pin className="w-3.5 h-3.5 text-[#5C4B7A] shrink-0" />
            <div className="min-w-0 flex-1">
              <div className="text-[10px] font-semibold uppercase tracking-wide text-[#5C4B7A]">
                Закреплено{pins.length > 1 ? ` · ${pins.length}` : ''}
              </div>
              <div className="text-xs text-slate-700 truncate [overflow-wrap:anywhere] break-all">{preview}</div>
            </div>
          </button>
          <button
            type="button"
            onPointerDown={(e) => {
              if (!isPrimaryPointer(e)) return;
              e.preventDefault();
              e.stopPropagation();
              setOpen((v) => !v);
            }}
            className={`relative z-30 w-11 h-11 shrink-0 rounded-full flex items-center justify-center transition ${
              open ? 'bg-white text-[#5C4B7A]' : 'text-slate-500 hover:bg-white/70 hover:text-[#5C4B7A]'
            }`}
            title="Список закреплённых"
            aria-label="Список закреплённых"
            aria-expanded={open}
          >
            <List className="w-4 h-4" />
          </button>
          <button
            type="button"
            onPointerDown={(e) => {
              if (!isPrimaryPointer(e)) return;
              e.preventDefault();
              e.stopPropagation();
              setOpen(false);
              onUnpin(current.messageId);
            }}
            className="relative z-30 w-11 h-11 shrink-0 rounded-full flex items-center justify-center text-slate-500 hover:bg-rose-50 hover:text-rose-500"
            title="Открепить"
            aria-label="Открепить сообщение"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>
      {open ? (
        <>
          <div
            className="fixed inset-0 z-10"
            aria-hidden
            onPointerDown={(e) => {
              e.preventDefault();
              e.stopPropagation();
              setOpen(false);
            }}
          />
          <div className="absolute left-0 right-0 top-full z-20 max-h-56 overflow-y-auto bg-white border-x border-b border-slate-200 shadow-[0_16px_32px_-18px_rgba(28,24,36,0.45)] rounded-b-xl">
            {pins.map((pin, index) => (
              <div
                key={pin.id}
                className={`flex items-center gap-1 px-2 py-1 ${index === offset ? 'bg-[#EDE6F5]/70' : 'hover:bg-slate-50'}`}
              >
                <button
                  type="button"
                  onPointerDown={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setOffset(index);
                    setOpen(false);
                    onJump(pin.messageId);
                  }}
                  className="min-w-0 flex-1 px-2 py-2 text-left"
                >
                  <div className="text-[11px] font-semibold text-slate-800 truncate">{pin.senderName || 'Сообщение'}</div>
                  <div className="text-[11px] text-slate-500 truncate [overflow-wrap:anywhere] break-all">{pinPreview(pin)}</div>
                </button>
                {canManage ? (
                  <button
                    type="button"
                    onPointerDown={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      onUnpin(pin.messageId);
                    }}
                    className="w-11 h-11 shrink-0 rounded-full flex items-center justify-center text-slate-400 hover:bg-rose-50 hover:text-rose-500"
                    title="Открепить"
                    aria-label="Открепить сообщение"
                  >
                    <X className="w-4 h-4" />
                  </button>
                ) : null}
              </div>
            ))}
          </div>
        </>
      ) : null}
    </div>
  );
}
