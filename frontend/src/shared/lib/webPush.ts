import api from '@/shared/lib/api';

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(base64);
  const output = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i += 1) {
    output[i] = raw.charCodeAt(i);
  }
  return output;
}

export async function getVapidPublicKey(): Promise<string | null> {
  try {
    const res = await api.get('/api/v1/social/notifications/push/vapid-public-key', {
      validateStatus: (status) => status === 200 || status === 204,
    });
    const key = res.data?.publicKey;
    return typeof key === 'string' && key.length > 0 ? key : null;
  } catch {
    return null;
  }
}

export async function enableWebPush(): Promise<boolean> {
  if (typeof window === 'undefined' || !('Notification' in window) || !('serviceWorker' in navigator)) {
    return false;
  }
  const key = await getVapidPublicKey();
  if (!key) return false;
  const permission = await Notification.requestPermission();
  if (permission !== 'granted') return false;
  const registration = await navigator.serviceWorker.ready;
  const existing = await registration.pushManager.getSubscription();
  const subscription = existing || await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(key) as BufferSource,
  });
  const json = subscription.toJSON();
  await api.post('/api/v1/social/notifications/push/subscribe', {
    endpoint: json.endpoint,
    p256dh: json.keys?.p256dh,
    auth: json.keys?.auth,
  });
  return true;
}

export async function disableWebPush(): Promise<void> {
  if (typeof window === 'undefined' || !('serviceWorker' in navigator)) return;
  try {
    const registration = await navigator.serviceWorker.ready;
    const subscription = await registration.pushManager.getSubscription();
    if (subscription) {
      await api.post('/api/v1/social/notifications/push/unsubscribe', { endpoint: subscription.endpoint }).catch(() => undefined);
      await subscription.unsubscribe();
    }
  } catch {
    /* ignore */
  }
}

export function isWebPushGranted(): boolean {
  return typeof Notification !== 'undefined' && Notification.permission === 'granted';
}
