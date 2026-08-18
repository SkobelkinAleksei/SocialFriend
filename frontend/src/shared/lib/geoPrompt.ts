import { showAppConfirm } from '@/shared/utils/appToast';

const GEO_CHOICE_KEY = 'myraion.geo.choice';

let devicePromptsDone = false;

export function markDevicePromptsDone(): void {
  devicePromptsDone = true;
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event('myraion.devicePrompts.done'));
  }
}

export function whenDevicePromptsDone(callback: () => void): void {
  if (devicePromptsDone) {
    callback();
    return;
  }
  if (typeof window === 'undefined') {
    callback();
    return;
  }
  window.addEventListener('myraion.devicePrompts.done', callback, { once: true });
}

export async function maybeAskGeo(): Promise<boolean> {
  if (typeof window === 'undefined' || !navigator.geolocation) return false;
  try {
    if (localStorage.getItem(GEO_CHOICE_KEY) === 'deny') return false;
  } catch {
    /* ignore */
  }
  if (navigator.permissions) {
    try {
      const status = await navigator.permissions.query({ name: 'geolocation' as PermissionName });
      if (status.state === 'granted') return true;
      if (status.state === 'denied') return false;
    } catch {
      /* Safari может не уметь query geolocation */
    }
  }
  try {
    if (localStorage.getItem(GEO_CHOICE_KEY) === 'allow') return true;
  } catch {
    /* ignore */
  }
  const ok = await showAppConfirm({
    title: 'Рядом с вами',
    message: 'Покажем события и район по геолокации.',
    confirmText: 'Разрешить',
    cancelText: 'Отказаться',
  });
  try {
    localStorage.setItem(GEO_CHOICE_KEY, ok ? 'allow' : 'deny');
  } catch {
    /* ignore */
  }
  return ok;
}
