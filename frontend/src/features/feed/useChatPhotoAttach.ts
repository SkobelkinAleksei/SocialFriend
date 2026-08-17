import { useRef, useState } from 'react';
import { uploadChatPhoto } from '@/shared/lib/api';
import { resolveChatPhotoUrl, toStoredChatPhoto } from '@/features/chat/chatPhotos';
import { showAppInfoToast } from '@/shared/utils/appToast';

export type PendingChatPhoto = {
  localId: string;
  preview: string;
  url: string;
  uploading: boolean;
};

export function useChatPhotoAttach(max = 10, upload: (file: File) => Promise<string> = uploadChatPhoto) {
  const [pending, setPending] = useState<PendingChatPhoto[]>([]);
  const inputRef = useRef<HTMLInputElement | null>(null);

  const openPicker = () => inputRef.current?.click();

  const addFiles = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return;
    const files = Array.from(fileList);
    const remaining = max - pending.length;
    if (remaining <= 0) {
      showAppInfoToast('Фото', `Можно прикрепить не больше ${max} фото`);
      return;
    }
    if (files.length > remaining) {
      showAppInfoToast('Фото', `Можно прикрепить не больше ${max} фото`);
    }
    const slice = files.slice(0, remaining);
    for (const file of slice) {
      if (!file.type.startsWith('image/')) {
        showAppInfoToast('Фото', 'Можно выбрать только изображение');
        continue;
      }
      if (file.size > 5 * 1024 * 1024) {
        showAppInfoToast('Фото', 'Файл больше 5 МБ');
        continue;
      }
      const localId = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      const preview = URL.createObjectURL(file);
      setPending((prev) => [...prev, { localId, preview, url: '', uploading: true }]);
      try {
        const stored = await upload(file);
        const resolved = resolveChatPhotoUrl(stored);
        setPending((prev) => prev.map((item) => (
          item.localId === localId ? { ...item, url: resolved, uploading: false } : item
        )));
      } catch {
        showAppInfoToast('Фото', 'Не удалось загрузить фото');
        setPending((prev) => prev.filter((item) => item.localId !== localId));
        URL.revokeObjectURL(preview);
      }
    }
  };

  const remove = (localId: string) => {
    setPending((prev) => {
      const target = prev.find((item) => item.localId === localId);
      if (target?.preview?.startsWith('blob:')) URL.revokeObjectURL(target.preview);
      return prev.filter((item) => item.localId !== localId);
    });
  };

  const hydrate = (urls: string[]) => {
    setPending((prev) => {
      prev.forEach((item) => {
        if (item.preview?.startsWith('blob:')) URL.revokeObjectURL(item.preview);
      });
      return (urls || []).filter(Boolean).map((url) => {
        const stored = toStoredChatPhoto(url) || url;
        return {
          localId: stored,
          preview: resolveChatPhotoUrl(stored),
          url: stored,
          uploading: false,
        };
      });
    });
  };

  const clear = () => {
    setPending((prev) => {
      prev.forEach((item) => {
        if (item.preview?.startsWith('blob:')) URL.revokeObjectURL(item.preview);
      });
      return [];
    });
  };

  const storedUrls = pending
    .map((item) => toStoredChatPhoto(item.url))
    .filter(Boolean);
  const allReady = pending.every((item) => !!item.url && !item.uploading);
  const uploading = pending.some((item) => item.uploading);

  return {
    pending,
    inputRef,
    openPicker,
    addFiles,
    remove,
    hydrate,
    clear,
    storedUrls,
    allReady,
    uploading,
    count: pending.length,
    max,
  };
}
