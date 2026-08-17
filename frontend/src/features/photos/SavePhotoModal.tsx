import React, { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Bookmark, Download, FolderPlus, Image as ImageIcon, User, X } from 'lucide-react';
import { useAuth } from '@/shared/context/AuthContext';
import { showAppInfoToast } from '@/shared/utils/appToast';
import { useAppBackHandler } from '@/shared/hooks/useAppBackHandler';
import ImageCropModal from '@/features/photos/ImageCropModal';
import { mediaUrl } from '@/shared/lib/runtime';
import {
  createAlbum,
  downloadPhoto,
  fetchAlbums,
  PhotoAlbum,
  savePhotoToAlbum,
  uploadAvatarFile,
} from '@/shared/utils/photoGallery';

type SavePhotoModalProps = {
  isOpen: boolean;
  sourceUrl: string;
  photoId?: number;
  onClose: () => void;
};

export default function SavePhotoModal({ isOpen, sourceUrl, onClose }: SavePhotoModalProps) {
  const { user, refreshUser } = useAuth();
  const [albums, setAlbums] = useState<PhotoAlbum[]>([]);
  const [newTitle, setNewTitle] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const [cropOpen, setCropOpen] = useState(false);

  useEffect(() => {
    if (!isOpen || !user?.id) return;
    fetchAlbums(Number(user.id)).then(setAlbums).catch(() => setAlbums([]));
    setNewTitle('');
  }, [isOpen, user?.id]);

  useAppBackHandler(isOpen || cropOpen, () => {
    if (cropOpen) setCropOpen(false);
    else onClose();
  });

  if (!isOpen && !cropOpen) return null;

  const run = async (key: string, task: () => Promise<void>) => {
    setBusy(key);
    try {
      await task();
      onClose();
    } catch (error: any) {
      showAppInfoToast('Фото', error?.response?.data?.detail || 'Не удалось выполнить действие');
    } finally {
      setBusy(null);
    }
  };

  return (
    <>
      {isOpen && createPortal(
        <div className="fixed inset-0 z-[90] bg-[#1A1916]/45 flex items-end sm:items-center justify-center px-4 py-6" onClick={onClose}>
          <div
            className="w-[min(440px,100%)] bg-[#FFFCFA] rounded-[28px] shadow-2xl p-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="myraion-display text-[24px] text-[#1C1824]">Сохранить фото</h3>
              <button type="button" onClick={onClose} className="w-9 h-9 rounded-full hover:bg-[#EDE6F5] flex items-center justify-center">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2">
              <button
                type="button"
                disabled={!!busy}
                onClick={() => run('download', () => downloadPhoto(sourceUrl))}
                className="w-full flex items-center gap-3 px-3 py-3 rounded-2xl hover:bg-[#EDE6F5] text-left"
              >
                <Download className="w-4 h-4 text-[#5C4B7A]" />
                <span className="text-sm font-semibold text-[#1C1824]">Скачать</span>
              </button>
              <button
                type="button"
                disabled={!!busy}
                onClick={() => run('saved', () => savePhotoToAlbum(sourceUrl).then(() => undefined))}
                className="w-full flex items-center gap-3 px-3 py-3 rounded-2xl hover:bg-[#EDE6F5] text-left"
              >
                <Bookmark className="w-4 h-4 text-[#5C4B7A]" />
                <span className="text-sm font-semibold text-[#1C1824]">В альбом «Сохранённые»</span>
              </button>
              <button
                type="button"
                disabled={!!busy}
                onClick={() => setCropOpen(true)}
                className="w-full flex items-center gap-3 px-3 py-3 rounded-2xl hover:bg-[#EDE6F5] text-left"
              >
                <User className="w-4 h-4 text-[#5C4B7A]" />
                <span className="text-sm font-semibold text-[#1C1824]">Поставить на аватар</span>
              </button>
            </div>

            <div className="mt-4 pt-4 border-t border-[#1C1824]/8">
              <div className="text-[11px] uppercase tracking-wider text-[#8A8494] mb-2">Группы</div>
              <div className="max-h-44 overflow-y-auto space-y-1">
                {albums.filter((album) => album.kind !== 'SAVED').map((album) => (
                  <button
                    key={album.id}
                    type="button"
                    disabled={!!busy}
                    onClick={() => run(`album-${album.id}`, () => savePhotoToAlbum(sourceUrl, { albumId: album.id }).then(() => undefined))}
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-[#EDE6F5] text-left"
                  >
                    <div className="w-9 h-9 rounded-lg overflow-hidden bg-[#EDE6F5] shrink-0">
                      {album.coverUrl ? <img src={resolveChatSafe(album.coverUrl)} alt="" className="w-full h-full object-cover" /> : <ImageIcon className="w-4 h-4 m-2.5 text-[#8A8494]" />}
                    </div>
                    <div className="min-w-0">
                      <div className="text-sm font-semibold text-[#1C1824] truncate">{album.title}</div>
                      <div className="text-[11px] text-[#8A8494]">{album.photoCount} фото</div>
                    </div>
                  </button>
                ))}
              </div>
              <div className="mt-3 flex gap-2">
                <input
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="Новая группа"
                  maxLength={80}
                  className="flex-1 px-3 py-2 rounded-xl bg-white border border-[#1C1824]/10 text-sm"
                />
                <button
                  type="button"
                  disabled={!!busy || !newTitle.trim()}
                  onClick={() => run('create', async () => {
                    const album = await createAlbum(newTitle.trim());
                    await savePhotoToAlbum(sourceUrl, { albumId: album.id });
                  })}
                  className="px-3 rounded-xl bg-[#EDE6F5] text-[#5C4B7A] disabled:opacity-40"
                >
                  <FolderPlus className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>,
        document.body
      )}
      {cropOpen && (
        <ImageCropModal
          source={sourceUrl}
          variant="avatar"
          onCancel={() => setCropOpen(false)}
          onConfirm={async (file, original) => {
            await uploadAvatarFile(file, original);
            await refreshUser();
            setCropOpen(false);
            onClose();
          }}
        />
      )}
    </>
  );
}

function resolveChatSafe(url: string): string {
  if (/^(https?:|blob:|data:)/i.test(url)) return url;
  return mediaUrl(url);
}
