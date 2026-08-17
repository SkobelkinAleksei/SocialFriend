import React, { useEffect, useRef, useState } from 'react';
import { Pause, Play } from 'lucide-react';
import { resolveChatMediaUrl } from '@/features/chat/chatPhotos';

export function formatVoiceTime(totalSec?: number | null): string {
  const sec = Math.max(0, Math.floor(Number(totalSec) || 0));
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export default function ChatVoiceBubble({
  url,
  duration,
  mine,
  compact,
}: {
  url?: string | null;
  duration?: number | null;
  mine?: boolean;
  compact?: boolean;
}) {
  const resolved = resolveChatMediaUrl(url);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [playing, setPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [current, setCurrent] = useState(0);
  const bars = compact ? 14 : 18;

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    const onTime = () => {
      const total = audio.duration || duration || 1;
      setCurrent(audio.currentTime);
      setProgress(total ? audio.currentTime / total : 0);
    };
    const onEnded = () => {
      setPlaying(false);
      setProgress(0);
      setCurrent(0);
    };
    audio.addEventListener('timeupdate', onTime);
    audio.addEventListener('ended', onEnded);
    return () => {
      audio.removeEventListener('timeupdate', onTime);
      audio.removeEventListener('ended', onEnded);
    };
  }, [duration]);

  if (!resolved) return null;

  const toggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    const audio = audioRef.current;
    if (!audio) return;
    if (playing) {
      audio.pause();
      setPlaying(false);
    } else {
      void audio.play();
      setPlaying(true);
    }
  };

  const time = formatVoiceTime(playing || current > 0 ? current : duration);

  return (
    <div className={`flex items-center gap-2 min-w-0 ${compact ? 'flex-1' : 'min-w-[210px] max-w-[260px] mb-1.5'} ${mine ? 'text-white' : 'text-[#1A1916]'}`}>
      <audio ref={audioRef} src={resolved} preload="metadata" />
      <button
        type="button"
        onClick={toggle}
        className={`${compact ? 'w-8 h-8' : 'w-9 h-9'} rounded-full flex items-center justify-center shrink-0 ${mine ? 'bg-white/20 hover:bg-white/30' : 'bg-[#5C4B7A] text-white hover:bg-[#4c3d68]'}`}
      >
        {playing ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5 ml-0.5" />}
      </button>
      <div className="flex-1 min-w-0">
        <div className={`flex items-end gap-[2px] ${compact ? 'h-5' : 'h-7'}`}>
          {Array.from({ length: bars }).map((_, i) => {
            const filled = i / bars <= progress;
            const h = compact ? 5 + ((i * 5) % 10) : 6 + ((i * 7) % 16);
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
      {compact && (
        <span className={`text-[11px] tabular-nums shrink-0 w-8 ${mine ? 'text-white/80' : 'text-[#6B645C]'}`}>
          {time}
        </span>
      )}
    </div>
  );
}
