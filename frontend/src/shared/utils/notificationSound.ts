const SOUND_PREF_KEY = 'myraion.notificationSound';

let audioCtx: AudioContext | null = null;
let lastPlayedAt = 0;

function getAudioContext(): AudioContext | null {
  const AC = window.AudioContext || (window as any).webkitAudioContext;
  if (!AC) return null;
  if (!audioCtx) audioCtx = new AC();
  return audioCtx;
}

export function isNotificationSoundEnabled(): boolean {
  try {
    return localStorage.getItem(SOUND_PREF_KEY) !== 'off';
  } catch {
    return true;
  }
}

export function setNotificationSoundEnabled(enabled: boolean): void {
  try {
    localStorage.setItem(SOUND_PREF_KEY, enabled ? 'on' : 'off');
  } catch {
    // private mode
  }
}

export function unlockNotificationSound(): void {
  const ctx = getAudioContext();
  if (!ctx) return;
  if (ctx.state === 'suspended') {
    void ctx.resume();
  }
}

function playChime(ctx: AudioContext): void {
  const t = ctx.currentTime;
  const master = ctx.createGain();
  master.gain.value = 0.16;
  master.connect(ctx.destination);

  const note = (freq: number, start: number, duration: number, peak: number) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, start);
    gain.gain.setValueAtTime(0.0001, start);
    gain.gain.exponentialRampToValueAtTime(peak, start + 0.018);
    gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
    osc.connect(gain);
    gain.connect(master);
    osc.start(start);
    osc.stop(start + duration + 0.02);
  };

  note(659.25, t, 0.16, 0.55);
  note(830.61, t + 0.11, 0.28, 0.62);
}

export function playNotificationSound(): void {
  if (!isNotificationSoundEnabled()) return;
  const now = Date.now();
  if (now - lastPlayedAt < 450) return;
  lastPlayedAt = now;

  const ctx = getAudioContext();
  if (!ctx) return;

  const play = () => playChime(ctx);
  if (ctx.state === 'suspended') {
    void ctx.resume().then(play).catch(() => undefined);
    return;
  }
  play();
}

export function bindNotificationSoundUnlock(): () => void {
  const unlock = () => unlockNotificationSound();
  window.addEventListener('pointerdown', unlock, { passive: true });
  window.addEventListener('keydown', unlock);
  return () => {
    window.removeEventListener('pointerdown', unlock);
    window.removeEventListener('keydown', unlock);
  };
}
