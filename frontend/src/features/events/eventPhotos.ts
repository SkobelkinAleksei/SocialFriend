import { mediaUrl, toRelativeApiPath } from '@/shared/lib/runtime';

export function resolveEventPhotoUrl(src?: string | null): string {
  if (!src) return '';
  if (src.includes('picsum.photos')) return '';
  return mediaUrl(src);
}

export function toStoredEventPhoto(src?: string | null): string {
  return toRelativeApiPath(src);
}

export function eventGalleryUrls(event?: { photos?: string[]; image?: string } | null): string[] {
  if (!event) return [];
  const fromPhotos = (event.photos || [])
    .map((item) => resolveEventPhotoUrl(item))
    .filter(Boolean);
  if (fromPhotos.length > 0) {
    return Array.from(new Set(fromPhotos));
  }
  const cover = resolveEventPhotoUrl(event.image);
  return cover ? [cover] : [];
}

export function collectEventPhotos(draft?: { photos?: string[]; image?: string } | null): (string | null)[] {
  const fromList = (draft?.photos || [])
    .map((item) => resolveEventPhotoUrl(item))
    .filter(Boolean);
  const slots: (string | null)[] = [null, null, null, null, null];
  fromList.slice(0, 5).forEach((url, index) => {
    slots[index] = url;
  });
  return slots;
}
