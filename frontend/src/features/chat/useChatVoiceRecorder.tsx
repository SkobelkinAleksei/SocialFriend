import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Mic, Send, Square, Trash2 } from 'lucide-react';
import { showAppInfoToast } from '@/shared/utils/appToast';
import ChatVoiceBubble, { formatVoiceTime } from '@/features/chat/ChatVoiceBubble';

type VoiceMode = 'idle' | 'recording' | 'preview';

const MAX_MS = 5 * 60 * 1000;
const MIN_MS = 800;

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

  const cleanupStream = () => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    recorderRef.current = null;
    chunksRef.current = [];
    if (timerRef.current) window.clearInterval(timerRef.current);
    timerRef.current = null;
  };

  const revokePreview = () => {
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current);
    previewUrlRef.current = '';
    setPreviewUrl('');
    blobRef.current = null;
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
      return;
    }
    const url = URL.createObjectURL(blob);
    blobRef.current = blob;
    durationRef.current = Math.max(1, Math.round(durationMs / 1000));
    previewUrlRef.current = url;
    setPreviewUrl(url);
    setPreviewDuration(durationRef.current);
    setElapsedMs(durationMs);
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

  const send = useCallback(async () => {
    const blob = blobRef.current;
    const duration = durationRef.current;
    if (!blob) {
      setMode('idle');
      return;
    }
    setBusy(true);
    try {
      await onRecorded(blob, duration);
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
    recorderRef.current?.stop();
    cleanupStream();
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current);
  }, []);

  return {
    mode,
    elapsedMs,
    busy,
    previewUrl,
    previewDuration,
    recording: mode === 'recording',
    preview: mode === 'preview',
    start,
    stopToPreview,
    cancel,
    send,
  };
}

function TelegramWave({ active }: { active: boolean }) {
  return (
    <>
      <style>{`
        @keyframes myraion-voice-bar {
          0%, 100% { transform: scaleY(0.22); opacity: 0.35; }
          50% { transform: scaleY(1); opacity: 1; }
        }
      `}</style>
      <div className="flex-1 h-6 flex items-center gap-[2px] overflow-hidden">
        {Array.from({ length: 28 }).map((_, i) => (
          <span
            key={i}
            className="w-[2px] h-5 rounded-full bg-[#8A76B0] origin-center"
            style={{
              animation: active ? `myraion-voice-bar ${0.55 + (i % 5) * 0.11}s ease-in-out ${i * 0.045}s infinite` : 'none',
              transform: active ? undefined : 'scaleY(0.28)',
              opacity: active ? undefined : 0.35,
            }}
          />
        ))}
      </div>
    </>
  );
}

export function ChatVoiceRecordingBar({
  mode,
  elapsedMs,
  previewUrl,
  previewDuration,
  busy,
  onCancel,
  onStop,
  onSend,
}: {
  mode: 'recording' | 'preview';
  elapsedMs: number;
  previewUrl?: string;
  previewDuration?: number;
  busy?: boolean;
  onCancel: () => void;
  onStop: () => void;
  onSend: () => void;
}) {
  return (
    <div className="flex items-center gap-2 w-full">
      <button
        type="button"
        onClick={onCancel}
        disabled={busy}
        className="p-2 rounded-lg text-rose-500 hover:bg-rose-50 disabled:opacity-40"
        title="Удалить"
      >
        <Trash2 className="w-4 h-4" />
      </button>
      <div className="flex-1 flex items-center gap-3 rounded-xl px-3 py-2 bg-[#F7F1EA] border border-[#1A1916]/10 min-w-0">
        {mode === 'preview' && previewUrl ? (
          <ChatVoiceBubble url={previewUrl} duration={previewDuration} />
        ) : (
          <>
            <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-pulse shrink-0" />
            <span className="text-sm font-semibold text-rose-600 tabular-nums w-10">{formatVoiceTime(elapsedMs / 1000)}</span>
            <TelegramWave active={mode === 'recording'} />
          </>
        )}
      </div>
      {mode === 'recording' ? (
        <button
          type="button"
          onClick={onStop}
          className="p-2.5 rounded-xl bg-rose-500 text-white hover:bg-rose-600"
          title="Остановить и прослушать"
        >
          <Square className="w-3.5 h-3.5 fill-current" />
        </button>
      ) : (
        <button
          type="button"
          onClick={onSend}
          disabled={busy}
          className="p-2.5 rounded-xl bg-[#5C4B7A] text-white hover:bg-[#4c3d68] disabled:opacity-40"
          title="Отправить"
        >
          <Send className="w-4 h-4" />
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
      className="p-2.5 rounded-lg text-slate-500 hover:bg-[#EDE6F5] hover:text-[#5C4B7A] transition disabled:opacity-40"
      title="Голосовое сообщение"
    >
      <Mic className="w-4 h-4" />
    </button>
  );
}
