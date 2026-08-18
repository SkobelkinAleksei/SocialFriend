import React, { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, Plus, Upload } from 'lucide-react';
import { ChatMediaLightbox } from '@/features/chat/ChatPhotoGrid';
import { useAuth } from '@/shared/context/AuthContext';
import api from '@/shared/lib/api';
import { showAppConfirm, showAppInfoToast } from '@/shared/utils/appToast';
import { useAppBackHandler } from '@/shared/hooks/useAppBackHandler';
import {
  createAlbum,
  deleteAlbum,
  deleteGalleryPhoto,
  fetchAlbums,
  fetchPhotos,
  GalleryPhoto,
  monthLabel,
  PhotoAlbum,
  reorderAlbums,
  resolvePhotoUrl,
  uploadGalleryPhoto,
} from '@/shared/utils/photoGallery';
import AlbumEditGrid from '@/features/photos/AlbumEditGrid';
import UploadAlbumPicker from '@/features/photos/UploadAlbumPicker';

type PhotosProps = {
  mode?: 'owner' | 'guest';
  targetUserId?: number;
  navigate?: (target: string) => void;
};

export default function Photos({ mode = 'owner', targetUserId, navigate }: PhotosProps) {
  const { user } = useAuth();
  const owner = mode === 'owner';
  const userId = owner ? Number(user?.id) : Number(targetUserId);
  const [tab, setTab] = useState<'albums' | 'timeline'>('albums');
  const [albums, setAlbums] = useState<PhotoAlbum[]>([]);
  const [photos, setPhotos] = useState<GalleryPhoto[]>([]);
  const [hidden, setHidden] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [activeAlbum, setActiveAlbum] = useState<PhotoAlbum | null>(null);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const [newAlbumTitle, setNewAlbumTitle] = useState('');
  const [ownerName, setOwnerName] = useState('');
  const [editing, setEditing] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const pendingAlbumIdRef = useRef<number | null>(null);

  useAppBackHandler(!!activeAlbum, () => setActiveAlbum(null));
  useAppBackHandler(editing && !activeAlbum, () => setEditing(false));

  const loadAlbums = async () => {
    if (!userId) return;
    try {
      setAlbums(await fetchAlbums(userId));
    } catch (error: any) {
      if (error?.response?.status === 403) setHidden(true);
    }
  };

  const loadPhotos = async (pageToLoad = 0, albumId?: number) => {
    if (!userId) return;
    setLoading(true);
    try {
      const data = await fetchPhotos(userId, { page: pageToLoad, size: 24, albumId });
      setHidden(Boolean(data.hidden));
      setPhotos((prev) => (pageToLoad === 0 ? data.items || [] : [...prev, ...(data.items || [])]));
      setHasMore(Boolean(data.hasMore));
      setPage(pageToLoad);
    } catch (error: any) {
      if (error?.response?.status === 403) setHidden(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!userId) return;
    setActiveAlbum(null);
    setTab('albums');
    setEditing(false);
    loadAlbums();
    loadPhotos(0);
    if (!owner) {
      api.get(`/api/v1/social/users/${userId}/profile`).then((res) => {
        setOwnerName(`${res.data?.firstName || ''} ${res.data?.lastName || ''}`.trim());
      }).catch(() => setOwnerName(''));
    }
  }, [userId, owner]);

  useEffect(() => {
    if (!owner && hidden) {
      navigate?.('neighbor-profile');
    }
  }, [hidden, owner, navigate]);

  const groups = useMemo(() => {
    const map = new Map<string, GalleryPhoto[]>();
    photos.forEach((photo) => {
      const key = monthLabel(photo.createdAt);
      map.set(key, [...(map.get(key) || []), photo]);
    });
    return Array.from(map.entries());
  }, [photos]);

  const uploadFiles = async (files: File[]) => {
    const albumId = activeAlbum?.id ?? pendingAlbumIdRef.current;
    pendingAlbumIdRef.current = null;
    if (!albumId) {
      showAppInfoToast('Фото', 'Выберите группу, куда загрузить.');
      return;
    }
    const images = files.filter((file) => file.type.startsWith('image/'));
    if (!images.length) return;
    setUploading(true);
    let uploaded = 0;
    let lastError = '';
    try {
      for (const file of images) {
        try {
          await uploadGalleryPhoto(file, albumId);
          uploaded += 1;
        } catch (error: any) {
          lastError = error?.response?.data?.detail || 'Не удалось загрузить фото';
        }
      }
      await loadAlbums();
      await loadPhotos(0, activeAlbum?.id);
      if (uploaded < images.length) {
        showAppInfoToast('Фото', lastError || 'Не все фото удалось загрузить');
      }
    } finally {
      setUploading(false);
    }
  };

  const startUpload = () => {
    if (uploading) return;
    if (activeAlbum?.id) {
      pendingAlbumIdRef.current = activeAlbum.id;
      fileRef.current?.click();
      return;
    }
    setPickerOpen(true);
  };

  const handleCreateAlbum = async () => {
    if (!newAlbumTitle.trim()) return;
    try {
      const album = await createAlbum(newAlbumTitle.trim());
      setNewAlbumTitle('');
      setAlbums((prev) => [album, ...prev]);
    } catch (error: any) {
      showAppInfoToast('Фото', error?.response?.data?.detail || 'Не удалось создать группу');
    }
  };

  const openAlbum = (album: PhotoAlbum) => {
    setEditing(false);
    setActiveAlbum(album);
    loadPhotos(0, album.id);
  };

  const backFromAlbum = () => {
    setActiveAlbum(null);
    loadPhotos(0);
  };

  const handleDeleteAlbum = async (album: PhotoAlbum) => {
    const confirmed = await showAppConfirm({
      title: 'Удалить группу',
      message: 'Группа и все фото внутри будут удалены.',
      confirmText: 'Удалить',
      cancelText: 'Назад',
      danger: true,
    });
    if (!confirmed) return;
    try {
      await deleteAlbum(album.id);
      setAlbums((prev) => prev.filter((item) => item.id !== album.id));
    } catch (error: any) {
      showAppInfoToast('Фото', error?.response?.data?.detail || error?.response?.data?.details || 'Не удалось удалить группу');
    }
  };

  const handleReorder = async (ids: number[]) => {
    const current = albums.map((item) => item.id).join(',');
    if (current === ids.join(',')) return;
    const previous = albums;
    setAlbums((prev) => {
      const map = new Map(prev.map((item) => [item.id, item]));
      return ids.map((id) => map.get(id)).filter(Boolean) as PhotoAlbum[];
    });
    try {
      const next = await reorderAlbums(ids);
      setAlbums(next);
    } catch (error: any) {
      setAlbums(previous);
      showAppInfoToast('Фото', error?.response?.data?.detail || 'Не удалось сохранить порядок');
    }
  };

  useEffect(() => {
    if (!editing) return;
    const onDown = (event: PointerEvent) => {
      const target = event.target as Element | null;
      if (target?.closest?.('[data-album-edit-root], [data-album-edit-toggle], [data-app-confirm]')) return;
      setEditing(false);
    };
    window.addEventListener('pointerdown', onDown);
    return () => window.removeEventListener('pointerdown', onDown);
  }, [editing]);

  if (!userId) return <div className="p-8 text-slate-500">Загружаем фотографии...</div>;
  if (!owner && hidden) return null;

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-8 min-h-screen">
      <div className="flex flex-col gap-3 mb-6 sm:flex-row sm:items-end sm:justify-between">
        <div className="min-w-0">
          <button type="button" onClick={() => navigate?.(owner ? 'my-page' : 'neighbor-profile')} className="text-xs font-semibold text-[#5C4B7A] mb-2 inline-flex items-center gap-1">
            <ArrowLeft className="w-3.5 h-3.5" /> Назад к профилю
          </button>
          <h1 className="myraion-display text-[28px] sm:text-[36px] md:text-[40px] leading-[1.05] text-[#1C1824]">
            {owner ? 'Фотографии' : `Фото · ${ownerName || 'сосед'}`}
          </h1>
        </div>
        {owner && (
          <div className="flex flex-wrap items-center gap-2 shrink-0 self-start sm:self-auto">
            <input
              ref={fileRef}
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              multiple
              className="hidden"
              onChange={(e) => {
                const files = Array.from(e.target.files || []);
                e.target.value = '';
                if (files.length) void uploadFiles(files);
              }}
            />
            <button
              type="button"
              disabled={uploading}
              onClick={startUpload}
              className="h-10 px-4 rounded-full bg-[#5C4B7A] text-white text-xs font-semibold inline-flex items-center justify-center gap-1.5 disabled:opacity-60"
            >
              <Upload className="w-3.5 h-3.5" /> {uploading ? 'Загрузка…' : 'Загрузить'}
            </button>
            {!activeAlbum && tab === 'albums' && (
              <button
                type="button"
                data-album-edit-toggle
                onClick={() => setEditing((v) => !v)}
                className={`h-10 px-4 rounded-full text-xs font-semibold inline-flex items-center justify-center gap-1.5 ${editing ? 'bg-[#5C4B7A] text-white' : 'bg-[#EDE6F5] text-[#5C4B7A]'}`}
              >
                {editing ? 'Готово' : 'Править'}
              </button>
            )}
          </div>
        )}
      </div>

      {hidden ? null : (
        <>
          {!activeAlbum && (
            <div className="flex gap-2 mb-5">
              <button type="button" onClick={() => { setTab('albums'); loadPhotos(0); }} className={`px-4 py-2 rounded-full text-xs font-semibold ${tab === 'albums' ? 'bg-[#5C4B7A] text-white' : 'bg-[#FFFCFA] text-[#5C4B7A]'}`}>Группы</button>
              <button type="button" onClick={() => { setTab('timeline'); setActiveAlbum(null); setEditing(false); loadPhotos(0); }} className={`px-4 py-2 rounded-full text-xs font-semibold ${tab === 'timeline' ? 'bg-[#5C4B7A] text-white' : 'bg-[#FFFCFA] text-[#5C4B7A]'}`}>По дате</button>
            </div>
          )}

          {activeAlbum && (
            <button type="button" onClick={backFromAlbum} className="mb-4 text-sm font-semibold text-[#5C4B7A] inline-flex items-center gap-1">
              <ArrowLeft className="w-4 h-4" /> Все группы
            </button>
          )}

          {owner && !activeAlbum && tab === 'albums' && (
            <div className="flex gap-2 mb-4">
              <input value={newAlbumTitle} onChange={(e) => setNewAlbumTitle(e.target.value)} placeholder="Название новой группы" maxLength={80} className="flex-1 px-4 py-2.5 rounded-2xl bg-[#FFFCFA] border border-[#1C1824]/10 text-sm" />
              <button type="button" onClick={handleCreateAlbum} className="px-4 rounded-2xl bg-[#EDE6F5] text-[#5C4B7A]">
                <Plus className="w-4 h-4" />
              </button>
            </div>
          )}

          {tab === 'albums' && !activeAlbum ? (
            albums.length === 0 && loading ? null : (
              <AlbumEditGrid
                albums={albums}
                editing={owner && editing}
                onOpen={openAlbum}
                onDelete={handleDeleteAlbum}
                onReorder={handleReorder}
              />
            )
          ) : (
            <div className="space-y-6">
              {activeAlbum && <h2 className="myraion-display text-[28px] text-[#1C1824]">{activeAlbum.title}</h2>}
              {(activeAlbum ? [['', photos] as const] : groups).map(([label, items]) => (
                <div key={label || 'album'}>
                  {label ? <div className="text-xs font-bold uppercase tracking-wider text-[#8A8494] mb-3">{label}</div> : null}
                  <div className="grid grid-cols-3 gap-1.5">
                    {items.map((photo, index) => (
                      <button key={photo.id} type="button" onClick={() => setLightboxIndex(photos.findIndex((item) => item.id === photo.id) >= 0 ? photos.findIndex((item) => item.id === photo.id) : index)} className="aspect-square overflow-hidden rounded-2xl bg-[#EDE6F5]">
                        <img src={resolvePhotoUrl(photo.url)} alt="" className="w-full h-full object-cover" />
                      </button>
                    ))}
                  </div>
                </div>
              ))}
              {photos.length === 0 && !loading && (
                <div className="text-center py-12 bg-[#FFFCFA] rounded-[32px] text-[#8A8494] text-sm">Фотографий пока нет</div>
              )}
              {hasMore && (
                <div className="flex justify-center">
                  <button type="button" onClick={() => loadPhotos(page + 1, activeAlbum?.id)} className="px-5 py-2 rounded-full bg-[#FFFCFA] text-[#5C4B7A] text-xs font-semibold">
                    Показать ещё
                  </button>
                </div>
              )}
            </div>
          )}
        </>
      )}

      <UploadAlbumPicker
        isOpen={pickerOpen}
        albums={albums}
        onClose={() => setPickerOpen(false)}
        onCreated={(album) => setAlbums((prev) => [album, ...prev.filter((item) => item.id !== album.id)])}
        onPick={(albumId) => {
          pendingAlbumIdRef.current = albumId;
          setPickerOpen(false);
          fileRef.current?.click();
        }}
      />

      {lightboxIndex != null && photos[lightboxIndex] && (
        <ChatMediaLightbox
          urls={photos.map((photo) => resolvePhotoUrl(photo.url))}
          index={lightboxIndex}
          onClose={() => setLightboxIndex(null)}
          onIndexChange={setLightboxIndex}
          meta={photos.map((photo) => ({ timestamp: photo.createdAt }))}
          likeSpec={{
            allowLike: true,
            allowSave: true,
            allowComment: true,
            kind: 'GALLERY',
            ownerId: userId,
            photoIds: photos.map((photo) => photo.id),
          }}
          onDelete={owner ? async (index) => {
            const photo = photos[index];
            if (!photo) return;
            try {
              await deleteGalleryPhoto(photo.id);
              const next = photos.filter((item) => item.id !== photo.id);
              setPhotos(next);
              await loadAlbums();
              if (!next.length) setLightboxIndex(null);
              else setLightboxIndex(Math.min(index, next.length - 1));
            } catch (error: any) {
              showAppInfoToast('Фото', error?.response?.data?.detail || 'Не удалось удалить');
            }
          } : undefined}
        />
      )}
    </div>
  );
}
