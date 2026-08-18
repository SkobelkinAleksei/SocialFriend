import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Mic, Pause, Play, Send, Square, Trash2 } from 'lucide-react';
import { showAppInfoToast } from '@/shared/utils/appToast';
import { claimChatAudio, releaseChatAudio, sliceVoiceBlob } from '@/features/chat/chatVoiceAudio';
import { formatVoiceTime } from '@/features/chat/ChatVoiceBubble';

type VoiceMode = 'idle' | 'recording' | 'preview';

const MAX_MS = 5 * 60 * 1000;
const MIN_MS = 800;
const MAX_PEAK_SAMPLES = 720;

function packPeaks(peaks: number[], extra: number): number[] {
  const next = [...peaks, extra];
  if (next.length <= MAX_PEAK_SAMPLES) return next;
  const packed: number[] = [];
  for (let i = 0; i < next.length; i += 2) {
    packed.push(Math.max(next[i], next[i + 1] ?? 0));
  }
  return packed;
}

function resamplePeaks(peaks: number[], count: number): number[] {
  if (count <= 0) return [];
  if (!peaks.length) return Array.from({ length: count }, () => 0.12);
  if (peaks.length === count) return peaks;
  const out: number[] = [];
  for (let i = 0; i < count; i++) {
    const from = Math.floor((i * peaks.length) / count);
    const to = Math.max(from + 1, Math.floor(((i + 1) * peaks.length) / count));
    let max = 0;
    for (let j = from; j < to; j++) max = Math.max(max, peaks[j] || 0);
    out.push(Math.max(0.08, max));
  }
  return out;
}

function pickMime(): string {
  const options = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/mp4'];
  return options.find((type) => typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported(type)) || '';
}

export function useChatVoiceRecorder(onRecorded: (blob: Blob, durationSec: number) => void | Promise<void>) {
  const [mode, setMode] = useState<VoiceMode>('idle');
  const [elapsedMs, setElapsedMs] = useState(0);
  const [busy, setBusy] = useState(false);
  const [previewUrl, setPreviewUrl] = useState('');
  const [previewDuration, setPreviewDuration] = useState(1);
  const [peaks, setPeaks] = useState<number[]>([]);
  const [trimStart, setTrimStart] = useState(0);
  const [trimEnd, setTrimEnd] = useState(1);
  const [previewPlaying, setPreviewPlaying] = useState(false);
  const [previewProgress, setPreviewProgress] = useState(0);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);
  const startedAtRef = useRef(0);
  const timerRef = useRef<number | null>(null);
  const blobRef = useRef<Blob | null>(null);
  const durationRef = useRef(1);
  const startingRef = useRef(false);
  const previewUrlRef = useRef('');
  const stopRef = useRef<() => void>(() => {});
  const analyserRef = useRef<AnalyserNode | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const peakTimerRef = useRef<number | null>(null);
  const previewAudioRef = useRef<HTMLAudioElement | null>(null);
  const trimStartRef = useRef(0);
  const trimEndRef = useRef(1);
  trimStartRef.current = trimStart;
  trimEndRef.current = trimEnd;

  const cleanupStream = () => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    recorderRef.current = null;
    chunksRef.current = [];
    if (timerRef.current) window.clearInterval(timerRef.current);
    timerRef.current = null;
    if (peakTimerRef.current) window.clearInterval(peakTimerRef.current);
    peakTimerRef.current = null;
    analyserRef.current = null;
    audioCtxRef.current?.close().catch(() => undefined);
    audioCtxRef.current = null;
  };

  const stopPreviewAudio = () => {
    const audio = previewAudioRef.current;
    if (audio) {
      audio.pause();
      releaseChatAudio(audio);
    }
    setPreviewPlaying(false);
  };

  const revokePreview = () => {
    stopPreviewAudio();
    previewAudioRef.current = null;
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current);
    previewUrlRef.current = '';
    setPreviewUrl('');
    blobRef.current = null;
    setPeaks([]);
    setTrimStart(0);
    setTrimEnd(1);
    setPreviewProgress(0);
  };

  const stopToPreview = useCallback(async () => {
    const rec = recorderRef.current;
    const durationMs = Date.now() - startedAtRef.current;
    if (!rec) {
      cleanupStream();
      setMode('idle');
      return;
    }
    const mime = rec.mimeType || 'audio/webm';
    await new Promise<void>((resolve) => {
      rec.onstop = () => resolve();
      if (rec.state !== 'inactive') rec.stop();
      else resolve();
    });
    const blob = new Blob(chunksRef.current, { type: mime });
    cleanupStream();
    if (durationMs < MIN_MS || blob.size < 200) {
      showAppInfoToast('Голосовое', 'Слишком короткое сообщение');
      setMode('idle');
      setElapsedMs(0);
      setPeaks([]);
      return;
    }
    const url = URL.createObjectURL(blob);
    blobRef.current = blob;
    durationRef.current = Math.max(1, Math.round(durationMs / 1000));
    previewUrlRef.current = url;
    setPreviewUrl(url);
    setPreviewDuration(durationRef.current);
    setElapsedMs(durationMs);
    setTrimStart(0);
    setTrimEnd(1);
    setPreviewProgress(0);
    setMode('preview');
  }, []);

  stopRef.current = () => { void stopToPreview(); };

  const start = useCallback(async () => {
    if (startingRef.current || busy) return;
    startingRef.current = true;
    if (typeof MediaRecorder === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
      startingRef.current = false;
      showAppInfoToast('Голосовое', 'Браузер не поддерживает запись');
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      const mime = pickMime();
      const rec = mime ? new MediaRecorder(stream, { mimeType: mime }) : new MediaRecorder(stream);
      chunksRef.current = [];
      rec.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) chunksRef.current.push(e.data);
      };
      recorderRef.current = rec;
      startedAtRef.current = Date.now();
      setElapsedMs(0);
      setPeaks(Array.from({ length: 8 }, () => 0.12));
      setTrimStart(0);
      setTrimEnd(1);
      const AudioCtx = window.AudioContext || (window as typeof window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (AudioCtx) {
        const ctx = new AudioCtx();
        await ctx.resume().catch(() => undefined);
        audioCtxRef.current = ctx;
        const source = ctx.createMediaStreamSource(stream);
        const analyser = ctx.createAnalyser();
        analyser.fftSize = 256;
        source.connect(analyser);
        analyserRef.current = analyser;
        const data = new Uint8Array(analyser.fftSize);
        peakTimerRef.current = window.setInterval(() => {
          analyser.getByteTimeDomainData(data);
          let sum = 0;
          for (let i = 0; i < data.length; i++) {
            const v = (data[i] - 128) / 128;
            sum += v * v;
          }
          const rms = Math.min(1, Math.sqrt(sum / data.length) * 3.4);
          setPeaks((prev) => packPeaks(prev, Math.max(0.08, rms)));
        }, 90);
      }
      rec.start(120);
      setMode('recording');
      if (timerRef.current) window.clearInterval(timerRef.current);
      timerRef.current = window.setInterval(() => {
        const ms = Date.now() - startedAtRef.current;
        setElapsedMs(ms);
        if (ms >= MAX_MS) stopRef.current();
      }, 200);
    } catch {
      showAppInfoToast('Голосовое', 'Нет доступа к микрофону');
      cleanupStream();
      setMode('idle');
    } finally {
      startingRef.current = false;
    }
  }, [busy]);

  const cancel = useCallback(() => {
    const rec = recorderRef.current;
    if (rec && rec.state !== 'inactive') rec.stop();
    cleanupStream();
    revokePreview();
    setElapsedMs(0);
    setMode('idle');
  }, []);

  const bindPreviewAudio = (url: string) => {
    if (previewAudioRef.current && previewAudioRef.current.src === url) return previewAudioRef.current;
    const audio = new Audio(url);
    audio.preload = 'metadata';
    previewAudioRef.current = audio;
    audio.addEventListener('timeupdate', () => {
      const total = audio.duration || durationRef.current || 1;
      const start = trimStartRef.current * total;
      const end = trimEndRef.current * total;
      if (audio.currentTime >= end - 0.02) {
        audio.pause();
        audio.currentTime = start;
        setPreviewPlaying(false);
        setPreviewProgress(trimStartRef.current);
        releaseChatAudio(audio);
        return;
      }
      setPreviewProgress(total ? audio.currentTime / total : 0);
    });
    audio.addEventListener('ended', () => {
      setPreviewPlaying(false);
      setPreviewProgress(trimStartRef.current);
      releaseChatAudio(audio);
    });
    audio.addEventListener('myraion-voice-pause', () => setPreviewPlaying(false));
    return audio;
  };

  const togglePreview = useCallback(() => {
    if (!previewUrlRef.current) return;
    const audio = bindPreviewAudio(previewUrlRef.current);
    const total = audio.duration || durationRef.current || 1;
    if (!audio.paused) {
      audio.pause();
      setPreviewPlaying(false);
      releaseChatAudio(audio);
      return;
    }
    const start = trimStartRef.current * total;
    const end = trimEndRef.current * total;
    if (audio.currentTime < start || audio.currentTime >= end - 0.04) {
      audio.currentTime = start;
    }
    claimChatAudio(audio);
    void audio.play().then(() => setPreviewPlaying(true)).catch(() => setPreviewPlaying(false));
  }, []);

  const seekPreview = useCallback((ratio: number) => {
    if (!previewUrlRef.current) return;
    const audio = bindPreviewAudio(previewUrlRef.current);
    const total = audio.duration || durationRef.current || 1;
    const next = Math.min(trimEndRef.current, Math.max(trimStartRef.current, ratio));
    audio.currentTime = next * total;
    setPreviewProgress(next);
  }, []);

  const changeTrim = useCallback((start: number, end: number) => {
    const minSpan = Math.min(0.35, MIN_MS / Math.max(elapsedMs, MIN_MS));
    let nextStart = Math.max(0, Math.min(start, 1));
    let nextEnd = Math.max(0, Math.min(end, 1));
    if (nextEnd - nextStart < minSpan) {
      if (start !== trimStartRef.current) nextStart = Math.max(0, nextEnd - minSpan);
      else nextEnd = Math.min(1, nextStart + minSpan);
    }
    setTrimStart(nextStart);
    setTrimEnd(nextEnd);
    setPreviewProgress((prev) => Math.min(nextEnd, Math.max(nextStart, prev)));
  }, [elapsedMs]);

  const send = useCallback(async () => {
    const blob = blobRef.current;
    if (!blob) {
      setMode('idle');
      return;
    }
    stopPreviewAudio();
    setBusy(true);
    try {
      const start = trimStartRef.current;
      const end = trimEndRef.current;
      const full = durationRef.current;
      const duration = Math.max(1, Math.round((end - start) * full));
      const ready = await sliceVoiceBlob(blob, start, end);
      await onRecorded(ready, duration);
      revokePreview();
      setElapsedMs(0);
      setMode('idle');
    } catch {
      showAppInfoToast('Голосовое', 'Не удалось отправить запись');
    } finally {
      setBusy(false);
    }
  }, [onRecorded]);

  useEffect(() => {
    if (mode === 'idle') return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') cancel();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [mode, cancel]);

  useEffect(() => () => {
    try { recorderRef.current?.stop(); } catch { /* already stopped */ }
    cleanupStream();
    const audio = previewAudioRef.current;
    if (audio) {
      audio.pause();
      audio.removeAttribute('src');
      audio.load();
      releaseChatAudio(audio);
    }
    previewAudioRef.current = null;
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current);
    previewUrlRef.current = '';
    blobRef.current = null;
  }, []);

  return {
    mode,
    elapsedMs,
    busy,
    previewUrl,
    previewDuration,
    peaks,
    trimStart,
    trimEnd,
    previewPlaying,
    previewProgress,
    recording: mode === 'recording',
    preview: mode === 'preview',
    start,
    stopToPreview,
    cancel,
    send,
    togglePreview,
    seekPreview,
    changeTrim,
  };
}

function VoiceWave({
  peaks,
  trimStart,
  trimEnd,
  progress,
  interactive,
  onSeek,
  onTrim,
}: {
  peaks: number[];
  trimStart?: number;
  trimEnd?: number;
  progress?: number;
  interactive?: boolean;
  onSeek?: (ratio: number) => void;
  onTrim?: (start: number, end: number) => void;
}) {
  const start = trimStart ?? 0;
  const end = trimEnd ?? 1;
  const dragRef = useRef<'start' | 'end' | 'seek' | null>(null);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const [barCount, setBarCount] = useState(64);

  useEffect(() => {
    const node = rootRef.current;
    if (!node) return;
    const update = () => {
      const width = node.clientWidth || 0;
      setBarCount(Math.max(36, Math.min(160, Math.floor(width / 2.4))));
    };
    update();
    const observer = new ResizeObserver(update);
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  const bars = resamplePeaks(peaks, barCount);

  const ratioFromEvent = (clientX: number) => {
    const rect = rootRef.current?.getBoundingClientRect();
    if (!rect || !rect.width) return 0;
    return Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
  };

  useEffect(() => {
    if (!interactive) return;
    const onMove = (event: PointerEvent) => {
      if (!dragRef.current) return;
      const ratio = ratioFromEvent(event.clientX);
      if (dragRef.current === 'start') onTrim?.(ratio, end);
      else if (dragRef.current === 'end') onTrim?.(start, ratio);
      else onSeek?.(ratio);
    };
    const onUp = () => { dragRef.current = null; };
    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
    return () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };
  }, [interactive, start, end, onSeek, onTrim]);

  return (
    <div
      ref={rootRef}
      className="relative flex-1 h-8 flex items-center min-w-0 select-none"
      onPointerDown={(event) => {
        if (!interactive) return;
        event.preventDefault();
        const ratio = ratioFromEvent(event.clientX);
        const nearStart = Math.abs(ratio - start) < 0.035;
        const nearEnd = Math.abs(ratio - end) < 0.035;
        if (onTrim && (nearStart || nearEnd)) {
          dragRef.current = nearEnd && (!nearStart || Math.abs(ratio - end) <= Math.abs(ratio - start)) ? 'end' : 'start';
          if (dragRef.current === 'start') onTrim(ratio, end);
          else onTrim(start, ratio);
          return;
        }
        dragRef.current = 'seek';
        onSeek?.(ratio);
      }}
    >
      <div className="absolute inset-0 flex items-center justify-between gap-px">
        {bars.map((value, i) => {
          const pos = bars.length > 1 ? i / (bars.length - 1) : 0;
          const inRange = pos >= start && pos <= end;
          const played = (progress ?? -1) >= pos && inRange;
          const h = 4 + value * 22;
          return (
            <span
              key={i}
              className={`rounded-full ${played ? 'bg-[#5C4B7A]' : inRange ? 'bg-[#5C4B7A]/70' : 'bg-[#5C4B7A]/20'}`}
              style={{ width: 2, height: `${h}px`, flex: '1 1 0', maxWidth: 3, minWidth: 1 }}
            />
          );
        })}
      </div>
      {interactive && (
        <>
          <span className="absolute top-0.5 bottom-0.5 w-[3px] rounded-full bg-[#5C4B7A] pointer-events-none" style={{ left: `${start * 100}%` }} />
          <span className="absolute top-0.5 bottom-0.5 w-[3px] rounded-full bg-[#5C4B7A] pointer-events-none" style={{ left: `calc(${end * 100}% - 3px)` }} />
        </>
      )}
    </div>
  );
}

export function ChatVoiceRecordingBar({
  mode,
  elapsedMs,
  peaks,
  previewDuration,
  previewPlaying,
  previewProgress,
  trimStart,
  trimEnd,
  busy,
  onCancel,
  onStop,
  onSend,
  onTogglePreview,
  onSeekPreview,
  onTrim,
}: {
  mode: 'recording' | 'preview';
  elapsedMs: number;
  peaks: number[];
  previewDuration?: number;
  previewPlaying?: boolean;
  previewProgress?: number;
  trimStart?: number;
  trimEnd?: number;
  busy?: boolean;
  onCancel: () => void;
  onStop: () => void;
  onSend: () => void;
  onTogglePreview: () => void;
  onSeekPreview: (ratio: number) => void;
  onTrim: (start: number, end: number) => void;
}) {
  const start = trimStart ?? 0;
  const end = trimEnd ?? 1;
  const full = previewDuration || elapsedMs / 1000;
  const shown = mode === 'preview' ? Math.max(1, (end - start) * full) : elapsedMs / 1000;

  return (
    <div className="flex items-center gap-2 w-full">
      <button
        type="button"
        onClick={onCancel}
        disabled={busy}
        className="p-2 rounded-full text-rose-500 hover:bg-rose-50 disabled:opacity-40"
        title="Удалить"
      >
        <Trash2 className="w-4 h-4" />
      </button>
      <div className="flex-1 flex items-center gap-2.5 rounded-full px-3 py-2 bg-[#E8E0F2] min-w-0">
        {mode === 'recording' ? (
          <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-pulse shrink-0" />
        ) : (
          <button
            type="button"
            onClick={onTogglePreview}
            className="w-8 h-8 rounded-full bg-[#5C4B7A] text-white flex items-center justify-center shrink-0"
            title="Прослушать"
          >
            {previewPlaying ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5 ml-0.5" />}
          </button>
        )}
        <VoiceWave
          peaks={peaks}
          trimStart={mode === 'preview' ? start : 0}
          trimEnd={mode === 'preview' ? end : 1}
          progress={mode === 'preview' ? previewProgress : undefined}
          interactive={mode === 'preview'}
          onSeek={onSeekPreview}
          onTrim={onTrim}
        />
        <span className={`text-xs font-semibold tabular-nums shrink-0 w-10 ${mode === 'recording' ? 'text-rose-600' : 'text-[#5C4B7A]'}`}>
          {formatVoiceTime(shown)}
        </span>
      </div>
      {mode === 'recording' ? (
        <button
          type="button"
          onClick={onStop}
          className="w-10 h-10 rounded-full bg-rose-500 text-white hover:bg-rose-600 flex items-center justify-center"
          title="Стоп"
        >
          <Square className="w-3.5 h-3.5 fill-current" />
        </button>
      ) : (
        <button
          type="button"
          onClick={onSend}
          disabled={busy}
          className="w-10 h-10 rounded-full bg-[#5C4B7A] text-white hover:bg-[#4c3d68] disabled:opacity-40 flex items-center justify-center"
          title="Отправить"
        >
          <Send className="w-4 h-4 ml-0.5" />
        </button>
      )}
    </div>
  );
}

export function ChatMicButton({
  disabled,
  onClick,
}: {
  disabled?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="w-10 h-10 rounded-full flex items-center justify-center text-slate-500 hover:bg-slate-100 hover:text-[#5C4B7A] transition disabled:opacity-40 shrink-0"
      title="Голосовое сообщение"
    >
      <Mic className="w-5 h-5" />
    </button>
  );
}
