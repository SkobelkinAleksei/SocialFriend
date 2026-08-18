const SPEEDS = [1, 1.5, 2] as const;

let activeAudio: HTMLAudioElement | null = null;

export function claimChatAudio(audio: HTMLAudioElement) {
  if (activeAudio && activeAudio !== audio) {
    activeAudio.pause();
    activeAudio.dispatchEvent(new Event('myraion-voice-pause'));
  }
  activeAudio = audio;
}

export function releaseChatAudio(audio: HTMLAudioElement) {
  if (activeAudio === audio) activeAudio = null;
}

export function nextVoiceSpeed(current: number): number {
  const index = SPEEDS.findIndex((item) => item === current);
  return SPEEDS[(index + 1) % SPEEDS.length];
}

export function formatVoiceSpeed(speed: number): string {
  return speed === 1 ? '1x' : `${speed}x`;
}

export async function sliceVoiceBlob(blob: Blob, startRatio: number, endRatio: number): Promise<Blob> {
  const from = Math.max(0, Math.min(startRatio, 1));
  const to = Math.max(from + 0.02, Math.min(endRatio, 1));
  if (from <= 0.005 && to >= 0.995) return blob;
  const ctx = new AudioContext();
  try {
    const buffer = await ctx.decodeAudioData(await blob.arrayBuffer());
    return encodeWavSlice(buffer, from, to);
  } finally {
    await ctx.close().catch(() => undefined);
  }
}

function encodeWavSlice(buffer: AudioBuffer, startRatio: number, endRatio: number): Blob {
  const start = Math.floor(buffer.length * startRatio);
  const end = Math.max(start + 1, Math.floor(buffer.length * endRatio));
  const sourceRate = buffer.sampleRate;
  const targetRate = 16000;
  const left = buffer.getChannelData(0);
  const right = buffer.numberOfChannels > 1 ? buffer.getChannelData(1) : left;
  const slice = new Float32Array(end - start);
  for (let i = 0; i < slice.length; i++) {
    slice[i] = (left[start + i] + right[start + i]) * 0.5;
  }
  const samples = downsample(slice, sourceRate, targetRate);
  const bytes = new ArrayBuffer(44 + samples.length * 2);
  const view = new DataView(bytes);
  writeAscii(view, 0, 'RIFF');
  view.setUint32(4, 36 + samples.length * 2, true);
  writeAscii(view, 8, 'WAVE');
  writeAscii(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, targetRate, true);
  view.setUint32(28, targetRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeAscii(view, 36, 'data');
  view.setUint32(40, samples.length * 2, true);
  let offset = 44;
  for (let i = 0; i < samples.length; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true);
    offset += 2;
  }
  return new Blob([bytes], { type: 'audio/wav' });
}

function downsample(input: Float32Array, fromRate: number, toRate: number): Float32Array {
  if (fromRate === toRate) return input;
  const ratio = fromRate / toRate;
  const out = new Float32Array(Math.max(1, Math.floor(input.length / ratio)));
  for (let i = 0; i < out.length; i++) {
    out[i] = input[Math.min(input.length - 1, Math.floor(i * ratio))];
  }
  return out;
}

function writeAscii(view: DataView, offset: number, text: string) {
  for (let i = 0; i < text.length; i++) view.setUint8(offset + i, text.charCodeAt(i));
}
