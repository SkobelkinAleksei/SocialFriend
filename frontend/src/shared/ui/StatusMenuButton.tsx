import React, { useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown } from 'lucide-react';

export type StatusMenuItem = {
  label: string;
  onClick: () => void;
  tone?: 'default' | 'danger' | 'success';
};

const VARIANTS = {
  accent: 'bg-[#5C4B7A] text-white border border-[#5C4B7A]',
  amber: 'bg-amber-50 border border-amber-300 text-amber-950',
  muted: 'bg-white border border-slate-300 text-slate-800',
  success: 'bg-[#4A8B6F] text-white border border-[#4A8B6F]',
};

export default function StatusMenuButton({
  label,
  items,
  variant = 'accent',
  className = '',
  buttonClassName = '',
}: {
  label: string;
  items: StatusMenuItem[];
  variant?: keyof typeof VARIANTS;
  className?: string;
  buttonClassName?: string;
  desktop?: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState<{ left: number; width: number; top: number; openUp: boolean } | null>(null);
  const ref = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const closedByOverlay = useRef(false);

  useLayoutEffect(() => {
    if (!open) {
      setPos(null);
      return;
    }
    const place = () => {
      const el = ref.current;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      const width = Math.max(rect.width, 196);
      const left = Math.min(Math.max(8, rect.left), Math.max(8, window.innerWidth - width - 8));
      const estimatedH = items.length * 40 + 12;
      const spaceBelow = window.innerHeight - rect.bottom;
      const openUp = spaceBelow < estimatedH + 12;
      setPos({
        left,
        width,
        top: openUp ? rect.top - 6 : rect.bottom + 6,
        openUp,
      });
    };
    place();
    window.addEventListener('resize', place);
    window.addEventListener('scroll', place, true);
    return () => {
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', place, true);
    };
  }, [open, items.length]);

  const menuButton = (
    <button
      type="button"
      onPointerDown={(e) => e.stopPropagation()}
      onClick={(e) => {
        e.stopPropagation();
        if (closedByOverlay.current) {
          closedByOverlay.current = false;
          return;
        }
        setOpen((v) => !v);
      }}
      className={`w-full h-9 px-3 text-xs font-semibold rounded-xl shadow-sm inline-flex items-center justify-center gap-1.5 ${VARIANTS[variant]} ${buttonClassName}`}
    >
      <span className="truncate">{label}</span>
      <ChevronDown className={`w-3.5 h-3.5 shrink-0 transition ${open ? 'rotate-180' : ''}`} />
    </button>
  );

  const menu =
    open && pos
      ? createPortal(
          <>
            <button
              type="button"
              className="fixed inset-0 z-[80] bg-transparent"
              aria-label="Закрыть меню"
              onPointerDown={(e) => {
                e.preventDefault();
                e.stopPropagation();
                closedByOverlay.current = true;
                setOpen(false);
                window.setTimeout(() => {
                  closedByOverlay.current = false;
                }, 400);
              }}
            />
            <div
              ref={menuRef}
              className="fixed z-[81] bg-white rounded-2xl shadow-[0_16px_40px_rgba(28,24,36,0.22)] border border-[#1C1824]/10 p-1"
              style={{
                left: pos.left,
                width: pos.width,
                top: pos.top,
                transform: pos.openUp ? 'translateY(-100%)' : undefined,
              }}
            >
              {items.map((item) => (
                <button
                  key={item.label}
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setOpen(false);
                    item.onClick();
                  }}
                  className={`w-full text-left px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                    item.tone === 'danger'
                      ? 'hover:bg-red-50 text-red-600'
                      : item.tone === 'success'
                        ? 'hover:bg-emerald-50 text-emerald-700'
                        : 'hover:bg-[#EDE6F5] text-slate-700'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </>,
          document.body,
        )
      : null;

  return (
    <div ref={ref} className={`relative ${className}`}>
      {menuButton}
      {menu}
    </div>
  );
}
