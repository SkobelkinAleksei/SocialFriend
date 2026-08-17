import React, { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, Image as ImageIcon, Plus, Upload } from 'lucide-react';
import { ChatMediaLightbox } from '@/features/chat/ChatPhotoGrid';
import { useAuth } from '@/shared/context/AuthContext';
import api from '@/shared/lib/api';
import { showAppInfoToast } from '@/shared/utils/appToast';
import { useAppBackHandler } from '@/shared/hooks/useAppBackHandler';
import {
  createAlbum,
  deleteGalleryPhoto,
  fetchAlbums,
  fetchPhotos,
  GalleryPhoto,
  monthLabel,
  PhotoAlbum,
  resolvePhotoUrl,
} from '@/shared/utils/photoGallery';

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
  const fileRef = useRef<HTMLInputElement>(null);

  useAppBackHandler(!!activeAlbum, () => setActiveAlbum(null));

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

  const upload = async (file: File) => {
    const form = new FormData();
    form.append('file', file);
    if (activeAlbum?.id) form.append('albumId', String(activeAlbum.id));
    try {
      await api.post('/api/v1/social/users/me/photos', form);
      await loadAlbums();
      await loadPhotos(0, activeAlbum?.id);
    } catch (error: any) {
      showAppInfoToast('Фото', error?.response?.data?.detail || 'Не удалось загрузить фото');
    }
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
    setActiveAlbum(album);
    loadPhotos(0, album.id);
  };

  const backFromAlbum = () => {
    setActiveAlbum(null);
    loadPhotos(0);
  };

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
          <div className="shrink-0 self-start sm:self-auto">
            <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp,image/gif" className="hidden" onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) upload(file);
              e.target.value = '';
            }} />
            <button type="button" onClick={() => fileRef.current?.click()} className="h-10 px-4 rounded-full bg-[#5C4B7A] text-white text-xs font-semibold inline-flex items-center gap-1.5">
              <Upload className="w-3.5 h-3.5" /> Загрузить
            </button>
          </div>
        )}
      </div>

      {hidden ? null : (
        <>
          {!activeAlbum && (
            <div className="flex gap-2 mb-5">
              <button type="button" onClick={() => { setTab('albums'); loadPhotos(0); }} className={`px-4 py-2 rounded-full text-xs font-semibold ${tab === 'albums' ? 'bg-[#5C4B7A] text-white' : 'bg-[#FFFCFA] text-[#5C4B7A]'}`}>Группы</button>
              <button type="button" onClick={() => { setTab('timeline'); setActiveAlbum(null); loadPhotos(0); }} className={`px-4 py-2 rounded-full text-xs font-semibold ${tab === 'timeline' ? 'bg-[#5C4B7A] text-white' : 'bg-[#FFFCFA] text-[#5C4B7A]'}`}>По дате</button>
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
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {albums.map((album) => (
                <button key={album.id} type="button" onClick={() => openAlbum(album)} className="text-left bg-[#FFFCFA] rounded-[24px] overflow-hidden hover:shadow-md transition">
                  <div className="h-32 bg-[#EDE6F5]">
                    {album.coverUrl ? (
                      <img src={resolvePhotoUrl(album.coverUrl)} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-[#8A8494]">
                        <ImageIcon className="w-8 h-8" />
                      </div>
                    )}
                  </div>
                  <div className="p-3">
                    <div className="text-sm font-semibold text-[#1C1824] truncate">{album.title}</div>
                    <div className="text-[11px] text-[#8A8494] mt-0.5">{album.photoCount} фото</div>
                  </div>
                </button>
              ))}
              {albums.length === 0 && !loading && (
                <div className="col-span-full text-center py-12 text-[#8A8494] text-sm">Групп пока нет</div>
              )}
            </div>
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
