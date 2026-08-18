import React, { useEffect, useRef, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { isCompactViewport } from '@/shared/utils/navigation';

const COMMIT_PX = 76;

function isScrollAtTop(target: EventTarget | null): boolean {
  let el: Element | null = target instanceof Element ? target : null;
  while (el && el !== document.body) {
    const style = window.getComputedStyle(el);
    const scrollable = /(auto|scroll)/.test(style.overflowY + style.overflow) && el.scrollHeight > el.clientHeight + 2;
    if (scrollable && el.scrollTop > 4) return false;
    el = el.parentElement;
  }
  const root = document.scrollingElement || document.documentElement;
  return root.scrollTop <= 4;
}

export default function MobilePullToRefresh() {
  const startRef = useRef<{ x: number; y: number } | null>(null);
  const trackingRef = useRef(false);
  const pullRef = useRef(0);
  const [pull, setPull] = useState(0);
  const [armed, setArmed] = useState(false);

  useEffect(() => {
    const reset = () => {
      startRef.current = null;
      trackingRef.current = false;
      pullRef.current = 0;
      setPull(0);
      setArmed(false);
    };

    const onStart = (e: TouchEvent) => {
      if (!isCompactViewport() || e.touches.length !== 1) return;
      if (document.querySelector('[data-photo-swipe], [data-post-modal], [data-event-modal]')) return;
      if ((e.target as Element | null)?.closest?.('[data-photo-swipe], [data-post-modal], [data-event-modal], [data-district-map], .leaflet-container')) return;
      const touch = e.touches[0];
      if (!isScrollAtTop(e.target)) return;
      startRef.current = { x: touch.clientX, y: touch.clientY };
      trackingRef.current = false;
      pullRef.current = 0;
    };

    const onMove = (e: TouchEvent) => {
      if (document.querySelector('[data-photo-swipe], [data-post-modal], [data-event-modal]')) {
        reset();
        return;
      }
      if ((e.target as Element | null)?.closest?.('[data-district-map], .leaflet-container')) {
        reset();
        return;
      }
      const start = startRef.current;
      if (!start || e.touches.length !== 1) return;
      const touch = e.touches[0];
      const dx = touch.clientX - start.x;
      const dy = touch.clientY - start.y;
      if (!trackingRef.current) {
        if (Math.abs(dx) < 8 && Math.abs(dy) < 8) return;
        if (dy < 10 || Math.abs(dx) >= Math.abs(dy) || !isScrollAtTop(e.target)) {
          startRef.current = null;
          return;
        }
        trackingRef.current = true;
      }
      if (!trackingRef.current) return;
      if (e.cancelable) e.preventDefault();
      const next = Math.max(0, Math.min(120, dy * 0.55));
      pullRef.current = next;
      setPull(next);
      setArmed(next >= COMMIT_PX);
    };

    const onEnd = () => {
      const shouldReload = trackingRef.current && pullRef.current >= COMMIT_PX;
      const hadGesture = Boolean(startRef.current || trackingRef.current || pullRef.current > 0);
      if (!hadGesture) return;
      reset();
      if (shouldReload) window.location.reload();
    };

    window.addEventListener('touchstart', onStart, { capture: true, passive: true });
    window.addEventListener('touchmove', onMove, { capture: true, passive: false });
    window.addEventListener('touchend', onEnd, { capture: true });
    window.addEventListener('touchcancel', onEnd, { capture: true });
    window.addEventListener('pointerup', onEnd, { capture: true });
    window.addEventListener('pointercancel', onEnd, { capture: true });
    return () => {
      window.removeEventListener('touchstart', onStart, true);
      window.removeEventListener('touchmove', onMove, true);
      window.removeEventListener('touchend', onEnd, true);
      window.removeEventListener('touchcancel', onEnd, true);
      window.removeEventListener('pointerup', onEnd, true);
      window.removeEventListener('pointercancel', onEnd, true);
    };
  }, []);

  if (pull <= 0) return null;

  const phoneTopBar = window.matchMedia('(max-width: 767px)').matches;

  return (
    <div
      className="lg:hidden pointer-events-none fixed z-[96] left-1/2 md:left-[calc(16rem+(100%-16rem)/2)] -translate-x-1/2"
      style={{
        top: phoneTopBar
          ? `calc(3.25rem + env(safe-area-inset-top) + ${Math.max(4, pull * 0.35)}px)`
          : `calc(env(safe-area-inset-top) + ${Math.max(12, pull * 0.35)}px)`,
      }}
      aria-hidden
    >
      <div
        className={`w-9 h-9 rounded-full bg-[#FFFCFA] border border-[#1C1824]/10 shadow-md flex items-center justify-center text-[#5C4B7A] transition-transform ${
          armed ? 'scale-110' : 'scale-100'
        }`}
      >
        <RefreshCw
          className="w-4 h-4"
          style={{ transform: `rotate(${pull * 3}deg)` }}
          strokeWidth={2.4}
        />
      </div>
    </div>
  );
}
