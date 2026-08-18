import { showAppConfirm, showAppInfoToast } from '@/shared/utils/appToast';

const PWA_CHOICE_KEY = 'myraion.pwa.choice.v2';

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

function isIos(): boolean {
  return /iphone|ipad|ipod/i.test(navigator.userAgent);
}

function isPhoneDevice(): boolean {
  return /iphone|ipod|windows phone/i.test(navigator.userAgent)
    || /android.+mobile/i.test(navigator.userAgent)
    || (/ipad|android/i.test(navigator.userAgent) && navigator.maxTouchPoints > 1);
}

export async function maybeOfferAddToHome(): Promise<void> {
  if (isStandaloneDisplay()) return;
  try {
    if (localStorage.getItem(PWA_CHOICE_KEY)) return;
  } catch {
    return;
  }
  const phone = isPhoneDevice();
  const title = phone ? '«На районе» на телефон' : '«На районе» на компьютер';
  const message = phone ? 'Добавить на экран "Домой"' : 'Добавить ярлык на рабочий стол';
  const ok = await showAppConfirm({
    title,
    message,
    confirmText: 'Как добавить',
    cancelText: 'Позже',
  });
  try {
    localStorage.setItem(PWA_CHOICE_KEY, ok ? 'shown' : 'later');
  } catch {
    /* ignore */
  }
  if (!ok) return;
  if (deferredPrompt) {
    try {
      await deferredPrompt.prompt();
      await deferredPrompt.userChoice;
    } catch {
      /* ignore */
    }
    deferredPrompt = null;
    return;
  }
  const howTo = phone
    ? (isIos()
      ? 'Поделитесь страницей → «На экран Домой».'
      : 'В меню браузера нажмите «Установить приложение» или «На экран Домой».')
    : 'В меню браузера нажмите «Установить приложение».';
  showAppInfoToast(title, howTo);
}
