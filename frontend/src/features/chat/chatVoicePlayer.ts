import { resolveChatMediaUrl } from '@/features/chat/chatPhotos';
import { claimChatAudio, nextVoiceSpeed, releaseChatAudio } from '@/features/chat/chatVoiceAudio';
import { isUnreadMarker } from '@/features/chat/chatUnread';

export type VoiceTrack = {
  id: string;
  url: string;
  duration?: number | null;
  title?: string;
};

export type VoicePlayerSnapshot = {
  track: VoiceTrack | null;
  playing: boolean;
  progress: number;
  currentTime: number;
  speed: number;
};

type Listener = () => void;

const listeners = new Set<Listener>();
let audio: HTMLAudioElement | null = null;
let queue: VoiceTrack[] = [];
let index = -1;
let playing = false;
let progress = 0;
let currentTime = 0;
let speed = 1;
let snapshot: VoicePlayerSnapshot = { track: null, playing: false, progress: 0, currentTime: 0, speed: 1 };

function notify() {
  snapshot = {
    track: queue[index] || null,
    playing,
    progress,
    currentTime,
    speed,
  };
  listeners.forEach((fn) => fn());
}

function ensureAudio() {
  if (audio) return audio;
  audio = new Audio();
  audio.addEventListener('timeupdate', () => {
    if (!audio) return;
    const total = audio.duration || queue[index]?.duration || 1;
    currentTime = audio.currentTime;
    progress = total ? audio.currentTime / total : 0;
    notify();
  });
  audio.addEventListener('ended', () => {
    playNextVoice();
  });
  audio.addEventListener('myraion-voice-pause', () => {
    playing = false;
    notify();
  });
  return audio;
}

function startCurrent() {
  const track = queue[index];
  if (!track) {
    stopVoicePlayer();
    return;
  }
  const el = ensureAudio();
  const src = resolveChatMediaUrl(track.url) || track.url;
  if (el.src !== src) el.src = src;
  el.playbackRate = speed;
  claimChatAudio(el);
  void el.play().then(() => {
    playing = true;
    notify();
  }).catch(() => {
    playing = false;
    notify();
  });
  notify();
}

export function subscribeVoicePlayer(fn: Listener) {
  listeners.add(fn);
  return () => { listeners.delete(fn); };
}

export function getVoicePlayerState(): VoicePlayerSnapshot {
  return snapshot;
}

export function voiceQueueFromMessages(messages: Array<{
  id: unknown;
  voiceUrl?: string | null;
  voiceDuration?: number | null;
  isDeleted?: boolean;
  isSystem?: boolean;
  from?: string;
  senderFirstName?: string;
  senderLastName?: string;
}>): VoiceTrack[] {
  return messages
    .filter((m) => !m.isDeleted && !m.isSystem && m.voiceUrl && !isUnreadMarker(String(m.id)))
    .map((m) => ({
      id: String(m.id),
      url: String(m.voiceUrl),
      duration: m.voiceDuration,
      title: m.from === 'me'
        ? 'Вы'
        : `${m.senderFirstName || ''} ${m.senderLastName || ''}`.trim() || 'Голосовое',
    }));
}

export function playVoiceQueue(tracks: VoiceTrack[], startId: string) {
  const list = tracks.filter((item) => item.url);
  const start = list.findIndex((item) => item.id === startId);
  queue = start >= 0 ? list.slice(start) : list;
  index = queue.length ? 0 : -1;
  progress = 0;
  currentTime = 0;
  startCurrent();
}

export function toggleVoicePlayer() {
  const el = audio;
  const track = queue[index];
  if (!el || !track) return;
  if (!el.paused) {
    el.pause();
    playing = false;
    releaseChatAudio(el);
    notify();
    return;
  }
  claimChatAudio(el);
  void el.play().then(() => {
    playing = true;
    notify();
  }).catch(() => {
    playing = false;
    notify();
  });
}

export function playNextVoice() {
  if (index + 1 < queue.length) {
    index += 1;
    progress = 0;
    currentTime = 0;
    startCurrent();
    return;
  }
  stopVoicePlayer();
}

export function seekVoicePlayer(ratio: number) {
  const el = audio;
  const track = queue[index];
  if (!el || !track) return;
  const total = el.duration || track.duration || 1;
  const next = Math.max(0, Math.min(1, ratio));
  el.currentTime = next * total;
  currentTime = el.currentTime;
  progress = next;
  notify();
}

export function stopVoicePlayer() {
  if (audio) {
    audio.pause();
    releaseChatAudio(audio);
    audio.removeAttribute('src');
    audio.load();
  }
  queue = [];
  index = -1;
  playing = false;
  progress = 0;
  currentTime = 0;
  notify();
}

export function cycleVoiceSpeed() {
  speed = nextVoiceSpeed(speed);
  if (audio) audio.playbackRate = speed;
  notify();
}

export function isVoiceTrackPlaying(id?: string | null) {
  return Boolean(id && snapshot.track?.id === String(id) && snapshot.playing);
}
