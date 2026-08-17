import React, { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import type { LucideIcon } from 'lucide-react';
import { MOBILE_SWIPE_BACK_EDGE, isCompactViewport, swipeBackOriginX } from '@/shared/utils/navigation';

export type ChatRowAction = {
  id: string;
  label: string;
  icon: LucideIcon;
  danger?: boolean;
  onSelect: () => void | Promise<void>;
};

const BTN = 68;
const OPEN_AT = 40;
const AXIS = 8;

let closeOpenRow: (() => void) | null = null;

export default function ChatListSwipeRow({
  className = '',
  actions,
  desktopButtons,
  children,
}: {
  className?: string;
  actions: ChatRowAction[];
  desktopButtons?: React.ReactNode;
  children: React.ReactNode;
}) {
  const strip = Math.max(actions.length, 1) * BTN;
  const [tx, setTx] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [menu, setMenu] = useState<{ x: number; y: number } | null>(null);
  const txRef = useRef(0);
  const startRef = useRef<{ x: number; y: number; tx: number } | null>(null);
  const axisRef = useRef<'x' | 'y' | null>(null);
  const skipClickRef = useRef(false);
  const pressTimer = useRef<number | null>(null);
  const pressPoint = useRef({ x: 0, y: 0 });
  const snapRef = useRef<(next: number) => void>(() => undefined);
  const closeSelf = useRef(() => {
    snapRef.current(0);
  }).current;

  const clearPress = () => {
    if (pressTimer.current) {
      window.clearTimeout(pressTimer.current);
      pressTimer.current = null;
    }
  };

  snapRef.current = (next: number) => {
    txRef.current = next;
    setTx(next);
    if (next !== 0) {
      if (closeOpenRow && closeOpenRow !== closeSelf) closeOpenRow();
      closeOpenRow = closeSelf;
    } else if (closeOpenRow === closeSelf) {
      closeOpenRow = null;
    }
  };

  const openMenu = (x: number, y: number) => {
    clearPress();
    skipClickRef.current = true;
    snapRef.current(0);
    try {
      navigator.vibrate?.(12);
    } catch {
      /* ignore */
    }
    const w = 196;
    const h = 12 + actions.length * 44;
    let left = x - w / 2;
    let top = y - h - 12;
    if (left < 10) left = 10;
    if (left + w > window.innerWidth - 10) left = window.innerWidth - w - 10;
    if (top < 10) top = y + 16;
    setMenu({ x: left, y: top });
  };

  useEffect(() => () => {
    clearPress();
    if (closeOpenRow === closeSelf) closeOpenRow = null;
  }, []);

  const onPointerDown = (e: React.PointerEvent) => {
    if (e.pointerType !== 'touch') return;
    if (!isCompactViewport()) return;
    if (e.clientX <= swipeBackOriginX() + MOBILE_SWIPE_BACK_EDGE) return;
    startRef.current = { x: e.clientX, y: e.clientY, tx: txRef.current };
    axisRef.current = null;
    pressPoint.current = { x: e.clientX, y: e.clientY };
    clearPress();
    pressTimer.current = window.setTimeout(() => {
      pressTimer.current = null;
      openMenu(pressPoint.current.x, pressPoint.current.y);
    }, 480);
  };

  const onPointerMove = (e: React.PointerEvent) => {
    const start = startRef.current;
    if (!start || e.pointerType !== 'touch') return;
    const dx = e.clientX - start.x;
    const dy = e.clientY - start.y;
    if (!axisRef.current) {
      if (Math.abs(dx) < AXIS && Math.abs(dy) < AXIS) return;
      axisRef.current = Math.abs(dx) > Math.abs(dy) ? 'x' : 'y';
      if (axisRef.current === 'x') {
        clearPress();
        setDragging(true);
        (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
      } else {
        clearPress();
      }
    }
    if (axisRef.current !== 'x') return;
    const next = Math.max(-strip, Math.min(0, start.tx + dx));
    txRef.current = next;
    setTx(next);
  };

  const onPointerUp = () => {
    const start = startRef.current;
    clearPress();
    startRef.current = null;
    setDragging(false);
    if (!start) {
      axisRef.current = null;
      return;
    }
    if (axisRef.current === 'x') {
      skipClickRef.current = true;
      const current = txRef.current;
      if (current <= -OPEN_AT) snapRef.current(-strip);
      else snapRef.current(0);
    }
    axisRef.current = null;
  };

  const onClickCapture = (e: React.MouseEvent) => {
    if (skipClickRef.current) {
      e.preventDefault();
      e.stopPropagation();
      skipClickRef.current = false;
      return;
    }
    if (txRef.current !== 0) {
      e.preventDefault();
      e.stopPropagation();
      snapRef.current(0);
    }
  };

  const run = async (action: ChatRowAction) => {
    setMenu(null);
    snapRef.current(0);
    await action.onSelect();
  };

  const compactCls = (action: ChatRowAction) => {
    if (action.danger) return 'bg-[#C45C5C] text-white';
    if (action.id === 'pin') return 'bg-[#7A6B94] text-white';
    return 'bg-[#5C4B7A] text-white';
  };
  const menuCls = (action: ChatRowAction) =>
    action.danger ? 'text-[#C45C5C]' : 'text-[#1C1824]';

  return (
    <div className="relative overflow-hidden isolate">
      <div className={`absolute inset-y-0 right-0 flex lg:hidden ${tx < 0 ? 'pointer-events-auto' : 'pointer-events-none'}`}>
        {actions.map((action) => {
          const Icon = action.icon;
          return (
            <button
              key={`r-${action.id}`}
              type="button"
              tabIndex={tx < 0 ? 0 : -1}
              onClick={(e) => {
                e.stopPropagation();
                void run(action);
              }}
              className={`w-[68px] h-full flex flex-col items-center justify-center gap-1 px-1 ${compactCls(action)}`}
            >
              <Icon className="w-4 h-4 shrink-0" />
              <span className="text-[9px] leading-tight text-center break-words w-full">{action.label}</span>
            </button>
          );
        })}
      </div>
      <div
        className="relative z-10 touch-pan-y select-none bg-[#FFFCFA] group"
        style={{ transform: `translateX(${tx}px)`, transition: dragging ? 'none' : 'transform 180ms ease' }}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
        onClickCapture={onClickCapture}
      >
        <div className={className}>
          {children}
        </div>
        <div className="hidden lg:contents">{desktopButtons}</div>
      </div>
      {menu
        ? createPortal(
            <>
              <button type="button" className="fixed inset-0 z-[80] lg:hidden bg-black/20" aria-label="Закрыть меню" onClick={() => setMenu(null)} />
              <div
                className="fixed z-[81] lg:hidden w-[196px] bg-white rounded-2xl shadow-[0_16px_40px_rgba(28,24,36,0.28)] border border-[#1C1824]/10 py-1.5 overflow-hidden"
                style={{ left: menu.x, top: menu.y }}
              >
                {actions.map((action) => {
                  const Icon = action.icon;
                  return (
                    <button
                      key={action.id}
                      type="button"
                      onClick={() => void run(action)}
                      className={`w-full flex items-center gap-2.5 px-3.5 py-2.5 text-sm text-left hover:bg-[#F6F2FA] ${menuCls(action)}`}
                    >
                      <Icon className="w-4 h-4 shrink-0" />
                      {action.label}
                    </button>
                  );
                })}
              </div>
            </>,
            document.body,
          )
        : null}
    </div>
  );
}
