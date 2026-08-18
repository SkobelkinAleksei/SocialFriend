import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { FolderPlus, Image as ImageIcon, X } from 'lucide-react';
import { useAppBackHandler } from '@/shared/hooks/useAppBackHandler';
import { createAlbum, PhotoAlbum, resolvePhotoUrl } from '@/shared/utils/photoGallery';
import { showAppInfoToast } from '@/shared/utils/appToast';

export default function UploadAlbumPicker({
  isOpen,
  albums,
  onClose,
  onPick,
  onCreated,
}: {
  isOpen: boolean;
  albums: PhotoAlbum[];
  onClose: () => void;
  onPick: (albumId: number) => void;
  onCreated: (album: PhotoAlbum) => void;
}) {
  const [newTitle, setNewTitle] = useState('');
  const [busy, setBusy] = useState(false);

  useAppBackHandler(isOpen, onClose);

  if (!isOpen) return null;

  return createPortal(
    <div className="fixed inset-0 z-[90] bg-[#1A1916]/45 flex items-end sm:items-center justify-center px-4 py-6" onClick={onClose}>
      <div
        className="w-[min(440px,100%)] bg-[#FFFCFA] rounded-[28px] shadow-2xl p-5"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="myraion-display text-[24px] text-[#1C1824]">Куда загрузить?</h3>
          <button type="button" onClick={onClose} className="w-9 h-9 rounded-full hover:bg-[#EDE6F5] flex items-center justify-center">
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="max-h-64 overflow-y-auto space-y-1">
          {albums.map((album) => (
            <button
              key={album.id}
              type="button"
              disabled={busy}
              onClick={() => onPick(album.id)}
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-[#EDE6F5] text-left"
            >
              <div className="w-9 h-9 rounded-lg overflow-hidden bg-[#EDE6F5] shrink-0">
                {album.coverUrl ? (
                  <img src={resolvePhotoUrl(album.coverUrl)} alt="" className="w-full h-full object-cover" />
                ) : (
                  <ImageIcon className="w-4 h-4 m-2.5 text-[#8A8494]" />
                )}
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
            disabled={busy || !newTitle.trim()}
            onClick={async () => {
              setBusy(true);
              try {
                const album = await createAlbum(newTitle.trim());
                setNewTitle('');
                onCreated(album);
                onPick(album.id);
              } catch (error: any) {
                showAppInfoToast('Фото', error?.response?.data?.detail || 'Не удалось создать группу');
              } finally {
                setBusy(false);
              }
            }}
            className="px-3 rounded-xl bg-[#EDE6F5] text-[#5C4B7A] disabled:opacity-40"
          >
            <FolderPlus className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
