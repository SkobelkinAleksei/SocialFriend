import { showAppConfirm } from '@/shared/utils/appToast';

const PWA_CHOICE_KEY = 'myraion.pwa.choice.v3';

type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
};

let deferredPrompt: BeforeInstallPromptEvent | null = null;

export function listenForInstallPrompt(): void {
  if (typeof window === 'undefined') return;
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    deferredPrompt = event as BeforeInstallPromptEvent;
  });
}

export function isStandaloneDisplay(): boolean {
  if (typeof window === 'undefined') return false;
  const media = window.matchMedia?.('(display-mode: standalone)').matches;
  const ios = Boolean((window.navigator as Navigator & { standalone?: boolean }).standalone);
  return Boolean(media || ios);
}

function isPhoneDevice(): boolean {
  return /iphone|ipod|windows phone/i.test(navigator.userAgent)
    || /android.+mobile/i.test(navigator.userAgent)
    || (/ipad|android/i.test(navigator.userAgent) && navigator.maxTouchPoints > 1);
}

function waitForInstallPrompt(ms: number): Promise<BeforeInstallPromptEvent | null> {
  if (deferredPrompt) return Promise.resolve(deferredPrompt);
  return new Promise((resolve) => {
    const timer = window.setTimeout(() => {
      window.removeEventListener('beforeinstallprompt', onPrompt);
      resolve(deferredPrompt);
    }, ms);
    const onPrompt = () => {
      window.clearTimeout(timer);
      resolve(deferredPrompt);
    };
    window.addEventListener('beforeinstallprompt', onPrompt, { once: true });
  });
}

export async function maybeOfferAddToHome(): Promise<void> {
  if (isStandaloneDisplay()) return;
  if (isPhoneDevice()) return;
  try {
    if (localStorage.getItem(PWA_CHOICE_KEY)) return;
  } catch {
    return;
  }
  const promptEvent = await waitForInstallPrompt(2000);
  if (!promptEvent) return;

  const ok = await showAppConfirm({
    title: '«На районе» на компьютер',
    message: 'Добавить ярлык на рабочий стол',
    confirmText: 'Добавить',
    cancelText: 'Позже',
  });
  try {
    localStorage.setItem(PWA_CHOICE_KEY, ok ? 'shown' : 'later');
  } catch {
    /* ignore */
  }
  if (!ok) return;
  try {
    await promptEvent.prompt();
    await promptEvent.userChoice;
  } catch {
    /* ignore */
  }
  deferredPrompt = null;
}
