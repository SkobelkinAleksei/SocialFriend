import api from '@/shared/lib/api';
import { resolveChatPhotoUrl, toStoredChatPhoto } from '@/features/chat/chatPhotos';

export const WARM_COVER_COLORS = [
  '#C9896A', '#D4A373', '#E07A5F', '#C38E70', '#DDA15E',
  '#BC6C25', '#A98467', '#E5989B', '#D08C60', '#B76E79',
  '#C9A227', '#E07A3D',
];

export const DEFAULT_COVER_COLOR = '#8A76B0';

export type PhotoLikeKind = 'GALLERY' | 'POST' | 'AVATAR';

export type PhotoAlbum = {
  id: number;
  title: string;
  kind: string;
  coverMode: string;
  coverUrl?: string | null;
  photoCount: number;
  sortOrder?: number;
};

export type GalleryPhoto = {
  id: number;
  ownerId: number;
  albumId: number;
  albumTitle?: string;
  url: string;
  createdAt: string;
  likesCount: number;
  liked: boolean;
};

export type GalleryPhotoPage = {
  items: GalleryPhoto[];
  page: number;
  size: number;
  total: number;
  hasMore: boolean;
  hidden: boolean;
};

export function randomWarmCoverColor(): string {
  return WARM_COVER_COLORS[Math.floor(Math.random() * WARM_COVER_COLORS.length)];
}

export function resolvePhotoUrl(src?: string | null): string {
  return resolveChatPhotoUrl(src);
}

export function toStoredPhoto(src?: string | null): string {
  return toStoredChatPhoto(src);
}

export async function downloadPhoto(src?: string | null): Promise<void> {
  const url = resolvePhotoUrl(src);
  if (!url) return;
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error('download failed');
    const blob = await response.blob();
    const href = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = href;
    link.download = `photo-${Date.now()}.jpg`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(href);
  } catch {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
}

export async function fetchAlbums(userId: number): Promise<PhotoAlbum[]> {
  const res = await api.get(`/api/v1/social/users/${userId}/albums`);
  return res.data || [];
}

export async function fetchPhotos(userId: number, params?: { albumId?: number; page?: number; size?: number }): Promise<GalleryPhotoPage> {
  const res = await api.get(`/api/v1/social/users/${userId}/photos`, { params });
  return res.data;
}

export async function fetchGalleryPhoto(photoId: number): Promise<GalleryPhoto> {
  const res = await api.get(`/api/v1/social/users/photos/${photoId}`);
  return res.data;
}

export async function createAlbum(title: string): Promise<PhotoAlbum> {
  const res = await api.post('/api/v1/social/users/me/albums', { title });
  return res.data;
}

export async function deleteAlbum(albumId: number): Promise<void> {
  await api.delete(`/api/v1/social/users/me/albums/${albumId}`);
}

export async function reorderAlbums(albumIds: number[]): Promise<PhotoAlbum[]> {
  const res = await api.put('/api/v1/social/users/me/albums/order', { albumIds });
  return res.data || [];
}

export async function uploadGalleryPhoto(file: File, albumId: number): Promise<GalleryPhoto> {
  const form = new FormData();
  form.append('file', file);
  form.append('albumId', String(albumId));
  const res = await api.post('/api/v1/social/users/me/photos', form);
  return res.data;
}

export async function savePhotoToAlbum(sourceUrl: string, options?: { albumId?: number; newAlbumTitle?: string }): Promise<GalleryPhoto> {
  const res = await api.post('/api/v1/social/users/me/photos/save', {
    sourceUrl: toStoredPhoto(sourceUrl) || sourceUrl,
    albumId: options?.albumId,
    newAlbumTitle: options?.newAlbumTitle,
  });
  return res.data;
}

export type AvatarHistoryItem = {
  id: number;
  url: string;
  viewUrl?: string | null;
  current: boolean;
  createdAt: string;
};

export async function setAvatarFromSource(sourceUrl?: string | null, photoId?: number): Promise<string> {
  const res = await api.post('/api/v1/social/users/me/avatar', {
    sourceUrl: sourceUrl ? (toStoredPhoto(sourceUrl) || sourceUrl) : undefined,
    photoId,
  });
  return res.data?.url;
}

export async function uploadAvatarFile(file: Blob, original?: Blob): Promise<string> {
  const form = new FormData();
  form.append('file', file, 'avatar.jpg');
  if (original) form.append('original', original, 'avatar-original.jpg');
  const res = await api.post('/api/v1/social/users/me/avatar/upload', form);
  return res.data?.url;
}

export async function fetchAvatarHistory(userId: number): Promise<AvatarHistoryItem[]> {
  const res = await api.get(`/api/v1/social/users/${userId}/avatars`);
  return res.data || [];
}

export async function restoreAvatar(historyId: number): Promise<string> {
  const res = await api.post(`/api/v1/social/users/me/avatar/${historyId}/restore`);
  return res.data?.url;
}

export async function deleteAvatarHistory(historyId: number): Promise<string> {
  const res = await api.delete(`/api/v1/social/users/me/avatar/${historyId}`);
  return res.data?.url;
}

export async function deleteGalleryPhoto(photoId: number): Promise<void> {
  await api.delete(`/api/v1/social/users/me/photos/${photoId}`);
}

export async function updateCover(mode: 'COLOR' | 'TRANSPARENT' | 'PHOTO', color?: string, coverUrl?: string): Promise<void> {
  await api.put('/api/v1/social/users/me/cover', { mode, color, coverUrl });
}

export async function uploadCoverFile(file: Blob): Promise<string> {
  const form = new FormData();
  form.append('file', file, 'cover.jpg');
  const res = await api.post('/api/v1/social/users/me/cover/upload', form);
  return res.data?.url;
}

export async function clearCoverPhoto(): Promise<void> {
  await api.delete('/api/v1/social/users/me/cover');
}

export async function togglePhotoLike(payload: {
  kind: PhotoLikeKind;
  targetKey: string;
  ownerId?: number;
  postId?: number;
}): Promise<{ liked: boolean; likesCount: number }> {
  const res = await api.post('/api/v1/social/users/photos/likes', payload);
  return res.data;
}

export async function fetchPhotoLikeStatus(payload: {
  kind: PhotoLikeKind;
  targetKey: string;
  ownerId?: number;
  postId?: number;
}): Promise<{ liked: boolean; likesCount: number }> {
  const res = await api.get('/api/v1/social/users/photos/likes', { params: payload });
  return res.data;
}

export function photoLikeTargetKey(kind: PhotoLikeKind, url: string, photoId?: number, ownerId?: number): string {
  if (kind === 'GALLERY' && photoId) return String(photoId);
  if (kind === 'AVATAR' && photoId) return `h:${photoId}`;
  if (kind === 'AVATAR' && ownerId) return String(ownerId);
  return toStoredPhoto(url) || url;
}

export function parsePhotoContextLabel(label?: string | null): {
  kind: 'GALLERY' | 'POST' | 'AVATAR';
  photoId?: number;
  postId?: number;
  url?: string;
  ownerId?: number;
} | null {
  const raw = String(label || '').trim();
  const gallery = raw.match(/^GALLERY:(\d+)$/i);
  if (gallery) return { kind: 'GALLERY', photoId: Number(gallery[1]) };
  const avatarHist = raw.match(/^AVATAR:(\d+):(\d+)$/i);
  if (avatarHist) return { kind: 'AVATAR', ownerId: Number(avatarHist[1]), photoId: Number(avatarHist[2]) };
  const avatar = raw.match(/^AVATAR:(\d+)$/i);
  if (avatar) return { kind: 'AVATAR', ownerId: Number(avatar[1]) };
  const post = raw.match(/^POST:(\d+):(.*)$/i);
  if (post) return { kind: 'POST', postId: Number(post[1]), url: post[2] };
  return null;
}

export function routeCommentNotification(n: { commentId?: number; targetId?: number; contextLabel?: string }) {
  if (n.commentId) localStorage.setItem('openCommentId', String(n.commentId));
  else localStorage.removeItem('openCommentId');
  const parsed = parsePhotoContextLabel(n.contextLabel);
  if (parsed) {
    window.dispatchEvent(new CustomEvent('forceOpenPhoto', {
      detail: { contextLabel: n.contextLabel, commentId: n.commentId }
    }));
    return 'photo';
  }
  if (n.targetId) {
    window.dispatchEvent(new CustomEvent('forceOpenNotification', {
      detail: { postId: Number(n.targetId) }
    }));
    return 'post';
  }
  return null;
}

export async function fetchPhotoComments(kind: 'GALLERY' | 'AVATAR', targetId: number, page = 0, size = 5) {
  const res = await api.get(`/api/v1/social/comments/public/photo/${kind}/${targetId}`, { params: { page, size } });
  return res.data;
}

export async function createPhotoComment(
  kind: 'GALLERY' | 'AVATAR',
  targetId: number,
  content: string,
  reply?: { userId: number; commentId: number }
) {
  const res = await api.post(`/api/v1/social/comments/public/photo/${kind}/${targetId}`, {
    content,
    replyToUserId: reply?.userId,
    replyToCommentId: reply?.commentId,
  });
  return res.data;
}

export async function countPhotoComments(kind: 'GALLERY' | 'AVATAR', targetId: number): Promise<number> {
  const res = await api.get(`/api/v1/social/comments/public/photo/${kind}/${targetId}/count`);
  return Number(res.data) || 0;
}

export function monthLabel(iso?: string): string {
  if (!iso) return 'Без даты';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return 'Без даты';
  const label = date.toLocaleDateString('ru-RU', { month: 'long', year: 'numeric' });
  return label.charAt(0).toUpperCase() + label.slice(1);
}
