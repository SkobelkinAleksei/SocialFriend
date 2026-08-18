import React, { useSyncExternalStore } from 'react';
import { Pause, Play, X } from 'lucide-react';
import { formatVoiceTime } from '@/features/chat/ChatVoiceBubble';
import {
  getVoicePlayerState,
  seekVoicePlayer,
  stopVoicePlayer,
  subscribeVoicePlayer,
  toggleVoicePlayer,
} from '@/features/chat/chatVoicePlayer';

export default function ChatVoiceMiniBar() {
  const player = useSyncExternalStore(subscribeVoicePlayer, getVoicePlayerState, getVoicePlayerState);
  if (!player.track) return null;

  const time = formatVoiceTime(player.playing || player.currentTime > 0 ? player.currentTime : player.track.duration);

  return (
    <div className="myraion-voice-mini pointer-events-none fixed inset-x-0 z-[80] flex justify-center px-3">
      <div className="pointer-events-auto w-full max-w-md rounded-2xl bg-white/95 shadow-lg border border-[#5C4B7A]/12 px-3 py-2 flex items-center gap-2.5 backdrop-blur-md">
        <button
          type="button"
          onClick={toggleVoicePlayer}
          className="w-9 h-9 rounded-full bg-[#5C4B7A] text-white flex items-center justify-center shrink-0"
        >
          {player.playing ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5 ml-0.5" />}
        </button>
        <button
          type="button"
          className="flex-1 min-w-0 text-left"
          onClick={toggleVoicePlayer}
        >
          <div className="text-[12px] font-semibold text-[#1C1824] truncate">{player.track.title || 'Голосовое'}</div>
          <div
            className="mt-1 h-1 rounded-full bg-[#5C4B7A]/15 overflow-hidden"
            onPointerDown={(e) => {
              e.stopPropagation();
              const rect = e.currentTarget.getBoundingClientRect();
              seekVoicePlayer((e.clientX - rect.left) / rect.width);
            }}
          >
            <div className="h-full bg-[#5C4B7A] rounded-full" style={{ width: `${Math.round(player.progress * 100)}%` }} />
          </div>
        </button>
        <span className="text-[11px] tabular-nums text-slate-500 shrink-0">{time}</span>
        <button
          type="button"
          onClick={stopVoicePlayer}
          className="p-1.5 rounded-full text-slate-400 hover:bg-slate-100 shrink-0"
          title="Остановить"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
