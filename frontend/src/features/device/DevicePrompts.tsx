import { useEffect } from 'react';
import { showAppConfirm } from '@/shared/utils/appToast';
import { maybeOfferAddToHome } from '@/shared/lib/pwa';
import { markDevicePromptsDone } from '@/shared/lib/geoPrompt';
import { enableWebPush, getVapidPublicKey, isWebPushGranted } from '@/shared/lib/webPush';

const PUSH_CHOICE_KEY = 'myraion.push.choice';

async function maybeOfferPush(): Promise<void> {
  if (typeof window === 'undefined' || !('Notification' in window)) return;
  const key = await getVapidPublicKey();
  if (!key) return;
  if (isWebPushGranted()) {
    await enableWebPush().catch(() => false);
    return;
  }
  if (Notification.permission === 'denied') return;
  try {
    if (localStorage.getItem(PUSH_CHOICE_KEY)) return;
  } catch {
    return;
  }
  const ok = await showAppConfirm({
    title: 'Сообщения от соседей',
    message: 'Разрешить уведомления, чтобы не пропустить чат и события, даже когда сайт закрыт.',
    confirmText: 'Разрешить',
    cancelText: 'Не сейчас',
  });
  try {
    localStorage.setItem(PUSH_CHOICE_KEY, ok ? 'allow' : 'later');
  } catch {
    /* ignore */
  }
  if (ok) {
    await enableWebPush();
  }
}

export default function DevicePrompts() {
  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      await new Promise((resolve) => window.setTimeout(resolve, 1200));
      if (cancelled) return;
      await maybeOfferAddToHome();
      if (cancelled) return;
      await maybeOfferPush();
    };
    void run().finally(() => {
      markDevicePromptsDone();
    });
    return () => {
      cancelled = true;
    };
  }, []);
  return null;
}
