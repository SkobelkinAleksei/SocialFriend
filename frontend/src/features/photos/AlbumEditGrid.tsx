import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
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

function AlbumFace({ album }: { album: PhotoAlbum }) {
  return (
    <>
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
    </>
  );
}

type FloatState = {
  x: number;
  y: number;
  width: number;
  height: number;
  offsetX: number;
  offsetY: number;
};

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
  const [floatPos, setFloatPos] = useState<FloatState | null>(null);
  const onReorderRef = useRef(onReorder);
  onReorderRef.current = onReorder;
  const itemsRef = useRef(items);
  const slotsRef = useRef(new Map<number, HTMLElement>());
  const prevRectsRef = useRef(new Map<number, DOMRect>());
  const floatElRef = useRef<HTMLDivElement | null>(null);
  const dragRef = useRef<{
    id: number;
    pointerId: number;
    startX: number;
    startY: number;
    grabbed: boolean;
    timer: number | null;
    target: HTMLElement | null;
    offsetX: number;
    offsetY: number;
    width: number;
    height: number;
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
      setFloatPos(null);
      dragRef.current = null;
    }
  }, [editing]);

  useLayoutEffect(() => {
    const nextRects = new Map<number, DOMRect>();
    items.forEach((album) => {
      const node = slotsRef.current.get(album.id);
      if (!node) return;
      const rect = node.getBoundingClientRect();
      const prev = prevRectsRef.current.get(album.id);
      if (prev && album.id !== draggingId) {
        const dx = prev.left - rect.left;
        const dy = prev.top - rect.top;
        if (dx !== 0 || dy !== 0) {
          node.style.transition = 'none';
          node.style.transform = `translate(${dx}px, ${dy}px)`;
          void node.offsetWidth;
          node.style.transition = 'transform 0.22s ease';
          node.style.transform = '';
        }
      }
      nextRects.set(album.id, rect);
    });
    prevRectsRef.current = nextRects;
  }, [items, draggingId]);

  useEffect(() => {
    if (!editing) return;

    const clearSelection = () => {
      const selection = window.getSelection();
      selection?.removeAllRanges();
    };

    const grab = (id: number, clientX: number, clientY: number) => {
      const drag = dragRef.current;
      if (!drag || drag.id !== id) return;
      drag.grabbed = true;
      drag.target?.setPointerCapture?.(drag.pointerId);
      clearSelection();
      try {
        navigator.vibrate?.(12);
      } catch {
        // ignore
      }
      setDraggingId(id);
      setFloatPos({
        x: clientX,
        y: clientY,
        width: drag.width,
        height: drag.height,
        offsetX: drag.offsetX,
        offsetY: drag.offsetY,
      });
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
        if (Math.hypot(dx, dy) > 6) grab(drag.id, event.clientX, event.clientY);
        if (!dragRef.current?.grabbed) return;
      }
      event.preventDefault();
      clearSelection();
      if (floatElRef.current) {
        floatElRef.current.style.left = `${event.clientX - drag.offsetX}px`;
        floatElRef.current.style.top = `${event.clientY - drag.offsetY}px`;
      }
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
      slotsRef.current.forEach((node) => {
        node.style.transition = '';
        node.style.transform = '';
      });
      setDraggingId(null);
      setFloatPos(null);
      if (grabbed) onReorderRef.current(itemsRef.current.map((item) => item.id));
    };

    const onSelectStart = (event: Event) => {
      if (dragRef.current?.grabbed) event.preventDefault();
    };

    window.addEventListener('pointermove', onMove, { passive: false });
    window.addEventListener('pointerup', onUp);
    window.addEventListener('pointercancel', onUp);
    window.addEventListener('selectstart', onSelectStart);
    return () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      window.removeEventListener('pointercancel', onUp);
      window.removeEventListener('selectstart', onSelectStart);
    };
  }, [editing]);

  const onPointerDown = (event: React.PointerEvent, album: PhotoAlbum) => {
    if (!editing) return;
    if ((event.target as Element).closest('[data-album-delete]')) return;
    if (event.pointerType !== 'touch') event.preventDefault();
    window.getSelection()?.removeAllRanges();
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const timer = event.pointerType === 'touch'
      ? window.setTimeout(() => {
          const drag = dragRef.current;
          if (!drag || drag.id !== album.id) return;
          drag.grabbed = true;
          drag.target?.setPointerCapture?.(drag.pointerId);
          window.getSelection()?.removeAllRanges();
          try {
            navigator.vibrate?.(12);
          } catch {
            // ignore
          }
          setDraggingId(album.id);
          setFloatPos({
            x: drag.startX,
            y: drag.startY,
            width: drag.width,
            height: drag.height,
            offsetX: drag.offsetX,
            offsetY: drag.offsetY,
          });
        }, 380)
      : null;
    dragRef.current = {
      id: album.id,
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      grabbed: false,
      timer,
      target,
      offsetX: event.clientX - rect.left,
      offsetY: event.clientY - rect.top,
      width: rect.width,
      height: rect.height,
    };
  };

  const draggingAlbum = draggingId ? items.find((item) => item.id === draggingId) : null;

  return (
    <div
      data-album-edit-root
      className="myraion-album-edit-root grid grid-cols-2 sm:grid-cols-3 gap-3"
      onContextMenu={editing ? (event) => event.preventDefault() : undefined}
    >
      {items.map((album) => {
        const canDelete = album.kind !== 'SAVED';
        const isDragging = draggingId === album.id;
        const jiggling = editing && !draggingId;
        return (
          <div
            key={album.id}
            ref={(node) => {
              if (node) slotsRef.current.set(album.id, node);
              else slotsRef.current.delete(album.id);
            }}
            data-album-id={album.id}
            className={`myraion-album-slot relative bg-[#FFFCFA] rounded-[24px] overflow-hidden ${jiggling ? 'myraion-album-jiggle' : ''} ${isDragging ? 'opacity-0' : ''}`}
            onPointerDown={(e) => onPointerDown(e, album)}
            style={editing ? { touchAction: draggingId ? 'none' : 'pan-y' } : undefined}
          >
            {editing && canDelete && !isDragging && (
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
              <AlbumFace album={album} />
            </button>
          </div>
        );
      })}
      {items.length === 0 && (
        <div className="col-span-full text-center py-12 text-[#8A8494] text-sm">Групп пока нет</div>
      )}
      {draggingAlbum && floatPos && (
        <div
          ref={floatElRef}
          className="pointer-events-none fixed z-50 bg-[#FFFCFA] rounded-[24px] overflow-hidden shadow-2xl"
          style={{
            left: floatPos.x - floatPos.offsetX,
            top: floatPos.y - floatPos.offsetY,
            width: floatPos.width,
            height: floatPos.height,
            transform: 'scale(1.06) rotate(1.5deg)',
            transformOrigin: 'center',
          }}
        >
          <AlbumFace album={draggingAlbum} />
        </div>
      )}
    </div>
  );
}
