import React, { useSyncExternalStore } from 'react';
import { Pause, Play, X } from 'lucide-react';
import { formatVoiceSpeed } from '@/features/chat/chatVoiceAudio';
import {
  cycleVoiceSpeed,
  getVoicePlayerState,
  seekVoicePlayer,
  stopVoicePlayer,
  subscribeVoicePlayer,
  toggleVoicePlayer,
} from '@/features/chat/chatVoicePlayer';

export default function ChatVoiceMiniBar({ className = '' }: { className?: string }) {
  const player = useSyncExternalStore(subscribeVoicePlayer, getVoicePlayerState, getVoicePlayerState);
  if (!player.track) return null;

  const seek = (e: React.PointerEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    seekVoicePlayer((e.clientX - rect.left) / rect.width);
  };

  return (
    <div className={className}>
      <div className="relative overflow-hidden rounded-full bg-white shadow-[0_8px_24px_rgba(28,24,36,0.10)] border border-[#1C1824]/8 pl-2.5 pr-1.5 py-[7px] flex items-center gap-2">
        <button
          type="button"
          onClick={toggleVoicePlayer}
          className="w-8 h-8 rounded-full flex items-center justify-center text-[#1C1824] shrink-0 hover:bg-slate-100"
          title={player.playing ? 'Пауза' : 'Слушать'}
        >
          {player.playing ? <Pause className="w-4 h-4" fill="currentColor" /> : <Play className="w-4 h-4 ml-0.5" fill="currentColor" />}
        </button>
        <button type="button" className="flex-1 min-w-0 text-left" onClick={toggleVoicePlayer}>
          <div className="text-[13px] font-semibold text-[#1C1824] truncate leading-tight">
            {player.track.title || 'Голосовое'}
          </div>
          <div className="text-[11px] text-slate-400 truncate leading-tight mt-0.5">Голосовое сообщение</div>
        </button>
        <button
          type="button"
          onClick={cycleVoiceSpeed}
          className="min-w-[2rem] h-8 px-1 rounded-md text-[11px] font-bold tracking-wide text-[#1C1824] shrink-0 hover:bg-slate-100"
          title="Скорость"
        >
          {formatVoiceSpeed(player.speed).replace('x', 'X')}
        </button>
        <button
          type="button"
          onClick={stopVoicePlayer}
          className="w-8 h-8 rounded-full text-slate-400 hover:bg-slate-100 hover:text-[#1C1824] flex items-center justify-center shrink-0"
          title="Закрыть"
        >
          <X className="w-4 h-4" />
        </button>
        <div
          className="absolute inset-x-0 bottom-0 h-3 cursor-pointer"
          onPointerDown={seek}
        >
          <div className="absolute inset-x-0 bottom-0 h-[2px] bg-[#1C1824]/10">
            <div
              className="h-full bg-[#1C1824] rounded-full"
              style={{ width: `${Math.round(player.progress * 100)}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
