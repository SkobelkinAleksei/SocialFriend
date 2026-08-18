import { useCallback, useRef, useState } from 'react';
import { uploadChatFile, uploadChatPhoto } from '@/shared/lib/api';
import { resolveChatPhotoUrl, toStoredChatPhoto } from '@/features/chat/chatPhotos';
import { showAppInfoToast } from '@/shared/utils/appToast';

export type PendingChatPhoto = {
  localId: string;
  preview: string;
  url: string;
  uploading: boolean;
};

export type PendingChatFile = {
  localId: string;
  name: string;
  size: number;
  mimeType: string;
  url: string;
  uploading: boolean;
};

export type StoredChatFile = {
  url: string;
  name: string;
  size: number;
  mimeType: string;
};

export function useChatMediaAttach(maxPhotos = 10, maxFiles = 5) {
  const [pendingPhotos, setPendingPhotos] = useState<PendingChatPhoto[]>([]);
  const [pendingFiles, setPendingFiles] = useState<PendingChatFile[]>([]);
  const photoInputRef = useRef<HTMLInputElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const photosRef = useRef<PendingChatPhoto[]>([]);
  photosRef.current = pendingPhotos;

  const openPhotoPicker = () => photoInputRef.current?.click();
  const openFilePicker = () => fileInputRef.current?.click();

  const addPhotos = useCallback(async (fileList: FileList | File[] | null) => {
    if (!fileList || fileList.length === 0) return;
    const files = Array.from(fileList);
    const remaining = maxPhotos - photosRef.current.length;
    if (remaining <= 0) {
      showAppInfoToast('Фото', `Можно прикрепить не больше ${maxPhotos} фото`);
      return;
    }
    if (files.length > remaining) {
      showAppInfoToast('Фото', `Можно прикрепить не больше ${maxPhotos} фото`);
    }
    const slice = files.slice(0, remaining);
    for (const file of slice) {
      if (!file.type.startsWith('image/') && file.type !== '') {
        showAppInfoToast('Фото', 'Можно выбрать только изображение');
        continue;
      }
      if (file.size > 5 * 1024 * 1024) {
        showAppInfoToast('Фото', 'Файл больше 5 МБ');
        continue;
      }
      const localId = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      const preview = URL.createObjectURL(file);
      setPendingPhotos((prev) => [...prev, { localId, preview, url: '', uploading: true }]);
      try {
        const stored = await uploadChatPhoto(file);
        const resolved = resolveChatPhotoUrl(stored);
        setPendingPhotos((prev) => prev.map((item) => (
          item.localId === localId ? { ...item, url: resolved, uploading: false } : item
        )));
      } catch {
        showAppInfoToast('Фото', 'Не удалось загрузить фото');
        setPendingPhotos((prev) => prev.filter((item) => item.localId !== localId));
        URL.revokeObjectURL(preview);
      }
    }
  }, [maxPhotos]);

  const addFiles = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return;
    const files = Array.from(fileList);
    const remaining = maxFiles - pendingFiles.length;
    if (remaining <= 0) {
      showAppInfoToast('Файлы', `Можно прикрепить не больше ${maxFiles} файлов`);
      return;
    }
    if (files.length > remaining) {
      showAppInfoToast('Файлы', `Можно прикрепить не больше ${maxFiles} файлов`);
    }
    const slice = files.slice(0, remaining);
    for (const file of slice) {
      if (file.size > 20 * 1024 * 1024) {
        showAppInfoToast('Файлы', `${file.name}: больше 20 МБ`);
        continue;
      }
      const localId = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      setPendingFiles((prev) => [...prev, {
        localId,
        name: file.name,
        size: file.size,
        mimeType: file.type,
        url: '',
        uploading: true,
      }]);
      try {
        const stored = await uploadChatFile(file);
        setPendingFiles((prev) => prev.map((item) => (
          item.localId === localId
            ? { ...item, url: stored.url, name: stored.name, size: stored.size, mimeType: stored.mimeType, uploading: false }
            : item
        )));
      } catch (err: any) {
        const details = err?.response?.data?.details || err?.response?.data?.error;
        showAppInfoToast('Файлы', details || `Не удалось загрузить ${file.name}`);
        setPendingFiles((prev) => prev.filter((item) => item.localId !== localId));
      }
    }
  };

  const removePhoto = (localId: string) => {
    setPendingPhotos((prev) => {
      const target = prev.find((item) => item.localId === localId);
      if (target?.preview?.startsWith('blob:')) URL.revokeObjectURL(target.preview);
      return prev.filter((item) => item.localId !== localId);
    });
  };

  const removeFile = (localId: string) => {
    setPendingFiles((prev) => prev.filter((item) => item.localId !== localId));
  };

  const clear = () => {
    setPendingPhotos((prev) => {
      prev.forEach((item) => {
        if (item.preview?.startsWith('blob:')) URL.revokeObjectURL(item.preview);
      });
      return [];
    });
    setPendingFiles([]);
  };

  const storedPhotoUrls = pendingPhotos
    .map((item) => toStoredChatPhoto(item.url))
    .filter(Boolean);
  const storedFiles: StoredChatFile[] = pendingFiles
    .filter((item) => item.url)
    .map((item) => ({
      url: toStoredChatPhoto(item.url) || item.url,
      name: item.name,
      size: item.size,
      mimeType: item.mimeType,
    }));
  const allReady = pendingPhotos.every((item) => !!item.url && !item.uploading)
    && pendingFiles.every((item) => !!item.url && !item.uploading);
  const uploading = pendingPhotos.some((item) => item.uploading) || pendingFiles.some((item) => item.uploading);

  return {
    pendingPhotos,
    pendingFiles,
    photoInputRef,
    fileInputRef,
    openPhotoPicker,
    openFilePicker,
    addPhotos,
    addFiles,
    removePhoto,
    removeFile,
    clear,
    storedPhotoUrls,
    storedFiles,
    allReady,
    uploading,
    photoCount: pendingPhotos.length,
    fileCount: pendingFiles.length,
    count: pendingPhotos.length + pendingFiles.length,
    maxPhotos,
    maxFiles,
  };
}
