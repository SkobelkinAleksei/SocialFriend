import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import api from '@/shared/lib/api';
import { toRelativeApiPath } from '@/shared/lib/runtime';

type CropVariant = 'avatar' | 'cover';

type ImageCropModalProps = {
  source: string | File;
  variant: CropVariant;
  onCancel: () => void;
  onConfirm: (file: File, original?: File) => Promise<void> | void;
};

const AVATAR_VIEW = 280;
const COVER_VIEW = { w: 360, h: 120 };
const AVATAR_OUT = 400;
const COVER_OUT = { w: 1200, h: 400 };
const ORIGINAL_MAX_EDGE = 1920;

function toApiPath(src: string): string {
  return toRelativeApiPath(src) || src;
}

async function loadObjectUrl(source: string | File): Promise<string> {
  if (source instanceof File) return URL.createObjectURL(source);
  if (/^(blob:|data:)/i.test(source)) return source;
  const path = toApiPath(source);
  const res = await api.get(path, { responseType: 'blob' });
  return URL.createObjectURL(res.data);
}

function loadHtmlImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('Не удалось загрузить фото'));
    img.src = url;
  });
}

function clampOffset(x: number, y: number, scale: number, imgW: number, imgH: number, vw: number, vh: number) {
  const w = imgW * scale;
  const h = imgH * scale;
  return {
    x: Math.min(0, Math.max(vw - w, x)),
    y: Math.min(0, Math.max(vh - h, y)),
  };
}

function canvasToJpeg(canvas: HTMLCanvasElement, quality = 0.92): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('blob'))), 'image/jpeg', quality);
  });
}

export default function ImageCropModal({ source, variant, onCancel, onConfirm }: ImageCropModalProps) {
  const isAvatar = variant === 'avatar';
  const vw = isAvatar ? AVATAR_VIEW : COVER_VIEW.w;
  const vh = isAvatar ? AVATAR_VIEW : COVER_VIEW.h;
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [img, setImg] = useState<HTMLImageElement | null>(null);
  const objectUrlRef = useRef<string | null>(null);
  const [scale, setScale] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const drag = useRef<{ x: number; y: number; ox: number; oy: number } | null>(null);

  const minScale = useMemo(() => {
    if (!img) return 1;
    return Math.max(vw / img.naturalWidth, vh / img.naturalHeight);
  }, [img, vw, vh]);
  const maxScale = minScale * 3;

  useEffect(() => {
    let cancelled = false;
    loadObjectUrl(source)
      .then(async (url) => {
        objectUrlRef.current = url.startsWith('blob:') ? url : null;
        const loaded = await loadHtmlImage(url);
        if (cancelled) return;
        const start = Math.max(vw / loaded.naturalWidth, vh / loaded.naturalHeight);
        setImg(loaded);
        setScale(start);
        setOffset({
          x: (vw - loaded.naturalWidth * start) / 2,
          y: (vh - loaded.naturalHeight * start) / 2,
        });
      })
      .catch(() => {
        if (!cancelled) setError('Не удалось загрузить фото для редактирования');
      });
    return () => {
      cancelled = true;
      if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
    };
  }, [source, vw, vh]);

  const applyScale = (next: number) => {
    if (!img) return;
    const clamped = Math.min(maxScale, Math.max(minScale, next));
    const cx = (vw / 2 - offset.x) / scale;
    const cy = (vh / 2 - offset.y) / scale;
    const nextOffset = clampOffset(
      vw / 2 - cx * clamped,
      vh / 2 - cy * clamped,
      clamped,
      img.naturalWidth,
      img.naturalHeight,
      vw,
      vh
    );
    setScale(clamped);
    setOffset(nextOffset);
  };

  const onPointerDown = (e: React.PointerEvent) => {
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    drag.current = { x: e.clientX, y: e.clientY, ox: offset.x, oy: offset.y };
  };
  const onPointerMove = (e: React.PointerEvent) => {
    if (!drag.current || !img) return;
    const next = clampOffset(
      drag.current.ox + (e.clientX - drag.current.x),
      drag.current.oy + (e.clientY - drag.current.y),
      scale,
      img.naturalWidth,
      img.naturalHeight,
      vw,
      vh
    );
    setOffset(next);
  };
  const onPointerUp = () => {
    drag.current = null;
  };

  const imageStyle = img ? {
    width: img.naturalWidth * scale,
    height: img.naturalHeight * scale,
    left: offset.x,
    top: offset.y,
  } : undefined;

  const renderCrop = (
    target: HTMLCanvasElement,
    srcX: number,
    srcY: number,
    srcW: number,
    srcH: number,
    outW: number,
    outH: number
  ) => {
    if (!img) return;
    target.width = outW;
    target.height = outH;
    const ctx = target.getContext('2d');
    if (!ctx) throw new Error('canvas');
    ctx.fillStyle = '#E8E8E8';
    ctx.fillRect(0, 0, outW, outH);
    ctx.drawImage(img, srcX, srcY, srcW, srcH, 0, 0, outW, outH);
  };

  const exportCrop = async () => {
    if (!img) return;
    setBusy(true);
    try {
      const cropCanvas = document.createElement('canvas');
      const outW = isAvatar ? AVATAR_OUT : COVER_OUT.w;
      const outH = isAvatar ? AVATAR_OUT : COVER_OUT.h;
      renderCrop(
        cropCanvas,
        -offset.x / scale,
        -offset.y / scale,
        vw / scale,
        vh / scale,
        outW,
        outH
      );
      const cropBlob = await canvasToJpeg(cropCanvas);
      const cropFile = new File([cropBlob], isAvatar ? 'avatar.jpg' : 'cover.jpg', { type: 'image/jpeg' });

      let originalFile: File | undefined;
      if (isAvatar) {
        const ratio = Math.min(1, ORIGINAL_MAX_EDGE / Math.max(img.naturalWidth, img.naturalHeight));
        const fullW = Math.max(1, Math.round(img.naturalWidth * ratio));
        const fullH = Math.max(1, Math.round(img.naturalHeight * ratio));
        const fullCanvas = document.createElement('canvas');
        renderCrop(fullCanvas, 0, 0, img.naturalWidth, img.naturalHeight, fullW, fullH);
        const fullBlob = await canvasToJpeg(fullCanvas, 0.93);
        originalFile = new File([fullBlob], 'avatar-original.jpg', { type: 'image/jpeg' });
      }

      await onConfirm(cropFile, originalFile);
    } catch {
      setError('Не удалось сохранить кадрирование');
      setBusy(false);
    }
  };

  const zoomValue = img ? (scale - minScale) / (maxScale - minScale || 1) : 0;

  return createPortal(
    <div className="fixed inset-0 z-[100] bg-[#1A1916]/70 flex items-center justify-center px-4 py-6" onClick={onCancel}>
      <div
        className="w-[min(560px,100%)] bg-[#FFFCFA] rounded-[28px] shadow-2xl p-5 max-h-[92vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="myraion-display text-[24px] text-[#1C1824]">
            {isAvatar ? 'Настройка аватарки' : 'Настройка фона'}
          </h3>
          <button type="button" onClick={onCancel} className="w-9 h-9 rounded-full hover:bg-[#EDE6F5] flex items-center justify-center">
            <X className="w-4 h-4" />
          </button>
        </div>

        {error ? <p className="text-sm text-[#B85C5C] mb-3">{error}</p> : null}

        <div className="flex flex-col sm:flex-row gap-4 items-center">
          <div className="flex-1 w-full">
            <p className="text-[11px] text-[#8A8494] mb-2">
              {isAvatar ? 'Положение в кружке — перетащите и масштабируйте' : 'Положение на фоне — перетащите и масштабируйте'}
            </p>
            <div
              className={`relative mx-auto overflow-hidden bg-[#D8D8D8] touch-none ${isAvatar ? 'rounded-full' : 'rounded-2xl'}`}
              style={{ width: vw, height: vh, cursor: 'grab' }}
              onPointerDown={onPointerDown}
              onPointerMove={onPointerMove}
              onPointerUp={onPointerUp}
              onPointerCancel={onPointerUp}
              onWheel={(e) => {
                e.preventDefault();
                applyScale(scale * (e.deltaY > 0 ? 0.94 : 1.06));
              }}
            >
              {img && (
                <img
                  src={img.src}
                  alt=""
                  draggable={false}
                  className="absolute max-w-none select-none pointer-events-none"
                  style={imageStyle}
                />
              )}
            </div>
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={zoomValue}
              onChange={(e) => applyScale(minScale + Number(e.target.value) * (maxScale - minScale))}
              className="w-full mt-3"
            />
          </div>
          <div className="shrink-0 text-center">
            <p className="text-[11px] text-[#8A8494] mb-2">
              {isAvatar ? 'Так будет в кружке' : 'Так будет на фоне'}
            </p>
            {isAvatar ? (
              <div className="w-24 h-24 rounded-full overflow-hidden mx-auto border-4 border-white shadow-md bg-[#D8D8D8] relative">
                {img && (
                  <img
                    src={img.src}
                    alt=""
                    className="absolute max-w-none"
                    style={{
                      width: (img.naturalWidth * scale * 96) / vw,
                      height: (img.naturalHeight * scale * 96) / vh,
                      left: (offset.x * 96) / vw,
                      top: (offset.y * 96) / vh,
                    }}
                  />
                )}
              </div>
            ) : (
              <div className="w-[180px] h-[60px] rounded-xl overflow-hidden mx-auto border border-[#1C1824]/10 bg-[#D8D8D8] relative">
                {img && (
                  <img
                    src={img.src}
                    alt=""
                    className="absolute max-w-none"
                    style={{
                      width: (img.naturalWidth * scale * 180) / vw,
                      height: (img.naturalHeight * scale * 60) / vh,
                      left: (offset.x * 180) / vw,
                      top: (offset.y * 60) / vh,
                    }}
                  />
                )}
              </div>
            )}
          </div>
        </div>

        <div className="flex justify-end gap-2 mt-5">
          <button type="button" onClick={onCancel} className="px-4 py-2 rounded-full text-sm text-[#8A8494]">
            Отмена
          </button>
          <button
            type="button"
            disabled={busy || !img}
            onClick={exportCrop}
            className="h-10 px-5 rounded-full bg-[#5C4B7A] text-white text-sm font-semibold disabled:opacity-50"
          >
            {busy ? 'Сохранение...' : 'Готово'}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
