import React, { useSyncExternalStore } from 'react';
import { Pause, Play } from 'lucide-react';
import { resolveChatMediaUrl } from '@/features/chat/chatPhotos';
import { formatVoiceSpeed } from '@/features/chat/chatVoiceAudio';
import {
  cycleVoiceSpeed,
  getVoicePlayerState,
  playVoiceQueue,
  seekVoicePlayer,
  subscribeVoicePlayer,
  toggleVoicePlayer,
  type VoiceTrack,
} from '@/features/chat/chatVoicePlayer';

export function formatVoiceTime(totalSec?: number | null): string {
  const sec = Math.max(0, Math.floor(Number(totalSec) || 0));
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function barsFromUrl(url: string, count: number): number[] {
  let hash = 0;
  for (let i = 0; i < url.length; i++) hash = (hash * 31 + url.charCodeAt(i)) >>> 0;
  return Array.from({ length: count }, (_, i) => {
    const n = ((hash >> (i % 16)) ^ (i * 17)) & 255;
    return 0.22 + (n / 255) * 0.78;
  });
}

export default function ChatVoiceBubble({
  url,
  duration,
  mine,
  compact,
  trackId,
  queue,
  title,
}: {
  url?: string | null;
  duration?: number | null;
  mine?: boolean;
  compact?: boolean;
  trackId?: string;
  queue?: VoiceTrack[];
  title?: string;
}) {
  const resolved = resolveChatMediaUrl(url);
  const id = String(trackId || url || '');
  const player = useSyncExternalStore(subscribeVoicePlayer, getVoicePlayerState, getVoicePlayerState);
  const active = player.track?.id === id;
  const playing = active && player.playing;
  const progress = active ? player.progress : 0;
  const current = active ? player.currentTime : 0;
  const speed = player.speed;
  const bars = compact ? 16 : 22;
  const peaks = barsFromUrl(resolved || 'voice', bars);

  if (!resolved || !id) return null;

  const toggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (active) {
      toggleVoicePlayer();
      return;
    }
    const own: VoiceTrack = { id, url: resolved, duration, title: title || 'Голосовое' };
    const list = queue && queue.length ? queue : [own];
    playVoiceQueue(list, id);
  };

  const seek = (e: React.PointerEvent<HTMLDivElement>) => {
    e.stopPropagation();
    if (!active) {
      const own: VoiceTrack = { id, url: resolved, duration, title: title || 'Голосовое' };
      playVoiceQueue(queue && queue.length ? queue : [own], id);
    }
    const rect = e.currentTarget.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    seekVoicePlayer(ratio);
  };

  const time = formatVoiceTime(playing || current > 0 ? current : duration);

  return (
    <div className={`flex items-center gap-2 min-w-0 ${compact ? 'flex-1' : 'min-w-[220px] max-w-[280px] mb-1.5'} ${mine ? 'text-white' : 'text-[#1A1916]'}`}>
      <button
        type="button"
        onClick={toggle}
        className={`${compact ? 'w-8 h-8' : 'w-9 h-9'} rounded-full flex items-center justify-center shrink-0 ${mine ? 'bg-white/20 hover:bg-white/30' : 'bg-[#5C4B7A] text-white hover:bg-[#4c3d68]'}`}
      >
        {playing ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5 ml-0.5" />}
      </button>
      <div className="flex-1 min-w-0">
        <div
          className={`flex items-end gap-[2px] cursor-pointer ${compact ? 'h-5' : 'h-7'}`}
          onPointerDown={seek}
        >
          {peaks.map((value, i) => {
            const filled = i / peaks.length <= progress;
            const h = (compact ? 5 : 7) + value * (compact ? 10 : 16);
            return (
              <span
                key={i}
                className={`w-[3px] rounded-full ${filled ? (mine ? 'bg-white' : 'bg-[#5C4B7A]') : (mine ? 'bg-white/35' : 'bg-[#5C4B7A]/25')}`}
                style={{ height: `${h}px` }}
              />
            );
          })}
        </div>
        {!compact && (
          <div className={`text-[10px] mt-0.5 ${mine ? 'text-white/80' : 'text-slate-500'}`}>
            {time}
          </div>
        )}
      </div>
      {!compact && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            cycleVoiceSpeed();
          }}
          className={`text-[10px] font-bold tabular-nums shrink-0 px-1.5 py-0.5 rounded-full ${mine ? 'bg-white/20 text-white' : 'bg-[#5C4B7A]/10 text-[#5C4B7A]'}`}
          title="Скорость"
        >
          {formatVoiceSpeed(speed)}
        </button>
      )}
      {compact && (
        <span className={`text-[11px] tabular-nums shrink-0 w-8 ${mine ? 'text-white/80' : 'text-[#6B645C]'}`}>
          {time}
        </span>
      )}
    </div>
  );
}
