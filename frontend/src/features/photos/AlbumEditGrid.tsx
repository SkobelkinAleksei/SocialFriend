import React, { useEffect, useRef, useState } from 'react';
import { Image as ImageIcon, X } from 'lucide-react';
import { PhotoAlbum, resolvePhotoUrl } from '@/shared/utils/photoGallery';

function moveAlbum(list: PhotoAlbum[], fromId: number, toId: number): PhotoAlbum[] {
  const from = list.findIndex((item) => item.id === fromId);
  const to = list.findIndex((item) => item.id === toId);
  if (from < 0 || to < 0 || from === to) return list;
  const next = [...list];
  const [item] = next.splice(from, 1);
  next.splice(to, 0, item);
  return next;
}

export default function AlbumEditGrid({
  albums,
  editing,
  onOpen,
  onDelete,
  onReorder,
}: {
  albums: PhotoAlbum[];
  editing: boolean;
  onOpen: (album: PhotoAlbum) => void;
  onDelete: (album: PhotoAlbum) => void;
  onReorder: (ids: number[]) => void;
}) {
  const [items, setItems] = useState(albums);
  const [draggingId, setDraggingId] = useState<number | null>(null);
  const onReorderRef = useRef(onReorder);
  onReorderRef.current = onReorder;
  const itemsRef = useRef(items);
  const dragRef = useRef<{
    id: number;
    pointerId: number;
    startX: number;
    startY: number;
    grabbed: boolean;
    timer: number | null;
    target: HTMLElement | null;
  } | null>(null);

  useEffect(() => {
    setItems(albums);
  }, [albums]);

  useEffect(() => {
    itemsRef.current = items;
  }, [items]);

  useEffect(() => {
    if (!editing) {
      setDraggingId(null);
      dragRef.current = null;
    }
  }, [editing]);

  useEffect(() => {
    if (!editing) return;

    const grab = (id: number) => {
      const drag = dragRef.current;
      if (!drag || drag.id !== id) return;
      drag.grabbed = true;
      drag.target?.setPointerCapture?.(drag.pointerId);
      setDraggingId(id);
    };

    const onMove = (event: PointerEvent) => {
      const drag = dragRef.current;
      if (!drag) return;
      const dx = event.clientX - drag.startX;
      const dy = event.clientY - drag.startY;
      if (!drag.grabbed) {
        if (event.pointerType === 'touch') {
          if (Math.hypot(dx, dy) > 10) {
            if (drag.timer) window.clearTimeout(drag.timer);
            dragRef.current = null;
          }
          return;
        }
        if (Math.hypot(dx, dy) > 6) grab(drag.id);
        if (!dragRef.current?.grabbed) return;
      }
      event.preventDefault();
      const under = document.elementFromPoint(event.clientX, event.clientY)?.closest('[data-album-id]');
      const overId = under ? Number(under.getAttribute('data-album-id')) : NaN;
      if (!overId || overId === drag.id) return;
      setItems((prev) => moveAlbum(prev, drag.id, overId));
    };

    const onUp = () => {
      const drag = dragRef.current;
      if (!drag) return;
      if (drag.timer) window.clearTimeout(drag.timer);
      const grabbed = drag.grabbed;
      try {
        drag.target?.releasePointerCapture?.(drag.pointerId);
      } catch {
        // ignore
      }
      dragRef.current = null;
      setDraggingId(null);
      if (grabbed) onReorderRef.current(itemsRef.current.map((item) => item.id));
    };

    window.addEventListener('pointermove', onMove, { passive: false });
    window.addEventListener('pointerup', onUp);
    window.addEventListener('pointercancel', onUp);
    return () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      window.removeEventListener('pointercancel', onUp);
    };
  }, [editing]);

  const onPointerDown = (event: React.PointerEvent, album: PhotoAlbum) => {
    if (!editing) return;
    if ((event.target as Element).closest('[data-album-delete]')) return;
    const timer = event.pointerType === 'touch'
      ? window.setTimeout(() => {
          const drag = dragRef.current;
          if (!drag || drag.id !== album.id) return;
          drag.grabbed = true;
          drag.target?.setPointerCapture?.(drag.pointerId);
          setDraggingId(album.id);
        }, 380)
      : null;
    dragRef.current = {
      id: album.id,
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      grabbed: false,
      timer,
      target: event.currentTarget as HTMLElement,
    };
  };

  return (
    <div data-album-edit-root className="grid grid-cols-2 sm:grid-cols-3 gap-3">
      {items.map((album) => {
        const canDelete = album.kind !== 'SAVED';
        const jiggling = editing && draggingId !== album.id;
        return (
          <div
            key={album.id}
            data-album-id={album.id}
            className={`relative bg-[#FFFCFA] rounded-[24px] overflow-hidden ${jiggling ? 'myraion-album-jiggle' : ''} ${draggingId === album.id ? 'shadow-lg z-10 scale-[1.03]' : ''}`}
            onPointerDown={(e) => onPointerDown(e, album)}
            style={editing ? { touchAction: draggingId ? 'none' : 'manipulation' } : undefined}
          >
            {editing && canDelete && (
              <button
                type="button"
                data-album-delete
                onClick={(e) => {
                  e.stopPropagation();
                  onDelete(album);
                }}
                className="absolute top-2 right-2 z-20 w-7 h-7 rounded-full bg-white/90 text-[#8A8494] border border-[#1C1824]/10 shadow-sm flex items-center justify-center"
                aria-label="Удалить группу"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
            <button
              type="button"
              disabled={editing}
              onClick={() => onOpen(album)}
              className="w-full text-left hover:shadow-md transition disabled:hover:shadow-none disabled:cursor-grab"
            >
              <div className="h-32 bg-[#EDE6F5] pointer-events-none">
                {album.coverUrl ? (
                  <img src={resolvePhotoUrl(album.coverUrl)} alt="" className="w-full h-full object-cover" draggable={false} />
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
          </div>
        );
      })}
      {items.length === 0 && (
        <div className="col-span-full text-center py-12 text-[#8A8494] text-sm">Групп пока нет</div>
      )}
    </div>
  );
}
