import { mediaUrl, toRelativeApiPath } from '@/shared/lib/runtime';

export function resolveChatPhotoUrl(src?: string | null): string {
  if (!src) return '';
  return mediaUrl(src);
}

export const resolveChatMediaUrl = resolveChatPhotoUrl;

export function toStoredChatPhoto(src?: string | null): string {
  return toRelativeApiPath(src);
}
