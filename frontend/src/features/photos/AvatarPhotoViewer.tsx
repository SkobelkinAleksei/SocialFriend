import React, { useEffect, useState } from 'react';
import { ChatMediaLightbox } from '@/features/chat/ChatPhotoGrid';
import {
  AvatarHistoryItem,
  deleteAvatarHistory,
  fetchAvatarHistory,
  resolvePhotoUrl,
} from '@/shared/utils/photoGallery';
type AvatarPhotoViewerProps = {
  userId: number;
  owner?: boolean;
  onClose: () => void;
  onEmpty?: () => void;
  onChanged?: () => Promise<void> | void;
};

export default function AvatarPhotoViewer({ userId, owner, onClose, onEmpty, onChanged }: AvatarPhotoViewerProps) {
  const [items, setItems] = useState<AvatarHistoryItem[] | null>(null);
  const [index, setIndex] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetchAvatarHistory(userId)
      .then((list) => {
        if (cancelled) return;
        if (!list.length) {
          onEmpty?.();
          onClose();
          return;
        }
        setItems(list);
        const current = list.findIndex((item) => item.current);
        setIndex(current >= 0 ? current : 0);
      })
      .catch(() => {
        if (!cancelled) onClose();
      });
    return () => {
      cancelled = true;
    };
  }, [userId]);

  if (!items?.length) return null;

  return (
    <ChatMediaLightbox
      urls={items.map((item) => resolvePhotoUrl(item.viewUrl || item.url))}
      index={index}
      onClose={onClose}
      onIndexChange={setIndex}
      meta={items.map((item) => ({ timestamp: item.createdAt }))}
      likeSpec={{
        allowLike: true,
        allowSave: true,
        allowComment: true,
        kind: 'AVATAR',
        ownerId: userId,
        photoIds: items.map((item) => item.id),
      }}
      onDelete={owner ? async (i) => {
        const item = items[i];
        if (!item) return;
        await deleteAvatarHistory(item.id);
        const next = items.filter((_, pos) => pos !== i);
        await onChanged?.();
        if (!next.length) {
          onClose();
          return;
        }
        setItems(next);
        setIndex(Math.min(i, next.length - 1));
      } : undefined}
    />
  );
}
