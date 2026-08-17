import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { addDays, startOfDay, toDayKey } from '@/features/events/eventVisuals';

const WEEKDAYS_RU = ['вс', 'пн', 'вт', 'ср', 'чт', 'пт', 'сб'];
const DAYS_BEFORE = 14;
const DAYS_AFTER = 75;

type WeekDateStripProps = {
  selectedDayKey: string | null;
  onSelect: (dayKey: string | null) => void;
  eventDayKeys: string[];
};

function parseDayKey(key: string): Date {
  const [y, m, d] = key.split('-').map(Number);
  return startOfDay(new Date(y, (m || 1) - 1, d || 1));
}

function monthTitle(from: Date, to: Date) {
  const cap = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);
  if (from.getMonth() !== to.getMonth() || from.getFullYear() !== to.getFullYear()) {
    const startMonth = cap(from.toLocaleDateString('ru-RU', { month: 'short' }).replace('.', ''));
    const endMonth = cap(to.toLocaleDateString('ru-RU', { month: 'short' }).replace('.', ''));
    return `${startMonth} — ${endMonth} ${to.getFullYear()}`;
  }
  const month = cap(from.toLocaleDateString('ru-RU', { month: 'long' }));
  return `${month} ${from.getFullYear()}`;
}

export default function WeekDateStrip({ selectedDayKey, onSelect, eventDayKeys }: WeekDateStripProps) {
  const today = useMemo(() => startOfDay(new Date()), []);
  const scrollerRef = useRef<HTMLDivElement>(null);
  const didInitScroll = useRef(false);
  const todayKey = toDayKey(today)!;
  const [title, setTitle] = useState(() =>
    monthTitle(today, addDays(today, 4))
  );

  const days = useMemo(() => {
    let start = addDays(today, -DAYS_BEFORE);
    let end = addDays(today, DAYS_AFTER);
    for (const key of eventDayKeys) {
      if (!key) continue;
      const day = parseDayKey(key);
      if (day < start) start = addDays(day, -3);
      if (day > end) end = addDays(day, 3);
    }
    const count = Math.round((end.getTime() - start.getTime()) / 86400000) + 1;
    return Array.from({ length: count }, (_, i) => addDays(start, i));
  }, [today, eventDayKeys]);

  const marked = new Set(eventDayKeys);

  const visibleRange = useCallback(() => {
    const root = scrollerRef.current;
    if (!root || days.length === 0) return { from: days[0], to: days[Math.min(4, days.length - 1)] };
    const buttons = Array.from(root.querySelectorAll<HTMLElement>('[data-day-key]'));
    const left = root.scrollLeft;
    const right = left + root.clientWidth;
    const visible = buttons.filter((btn) => {
      const mid = btn.offsetLeft + btn.offsetWidth / 2;
      return mid >= left && mid <= right;
    });
    const first = visible[0] || buttons[0];
    const last = visible[visible.length - 1] || buttons[buttons.length - 1];
    const fromKey = first?.dataset.dayKey;
    const toKey = last?.dataset.dayKey;
    return {
      from: fromKey ? parseDayKey(fromKey) : days[0],
      to: toKey ? parseDayKey(toKey) : days[days.length - 1],
    };
  }, [days]);

  const syncTitle = useCallback(() => {
    const { from, to } = visibleRange();
    setTitle(monthTitle(from, to));
  }, [visibleRange]);

  const scrollToKey = useCallback((key: string, inline: ScrollLogicalPosition = 'start') => {
    const root = scrollerRef.current;
    const btn = root?.querySelector<HTMLElement>(`[data-day-key="${key}"]`);
    if (!root || !btn) return;
    const pad = 16;
    if (inline === 'center') {
      root.scrollTo({ left: btn.offsetLeft - root.clientWidth / 2 + btn.offsetWidth / 2, behavior: 'smooth' });
    } else {
      root.scrollTo({ left: Math.max(0, btn.offsetLeft - pad), behavior: 'smooth' });
    }
  }, []);

  const scrollByPage = (dir: -1 | 1) => {
    const root = scrollerRef.current;
    if (!root) return;
    root.scrollBy({ left: dir * root.clientWidth * 0.85, behavior: 'smooth' });
  };

  useEffect(() => {
    if (didInitScroll.current || days.length === 0) return;
    const key = selectedDayKey || todayKey;
    const root = scrollerRef.current;
    const btn = root?.querySelector<HTMLElement>(`[data-day-key="${key}"]`);
    if (!root || !btn) return;
    root.scrollLeft = Math.max(0, btn.offsetLeft - 16);
    didInitScroll.current = true;
    syncTitle();
  }, [days, selectedDayKey, todayKey, syncTitle]);

  useEffect(() => {
    if (!didInitScroll.current || !selectedDayKey) return;
    scrollToKey(selectedDayKey, 'center');
  }, [selectedDayKey, scrollToKey]);

  useEffect(() => {
    const root = scrollerRef.current;
    if (!root) return;
    let frame = 0;
    const onScroll = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(syncTitle);
    };
    root.addEventListener('scroll', onScroll, { passive: true });
    return () => {
      cancelAnimationFrame(frame);
      root.removeEventListener('scroll', onScroll);
    };
  }, [syncTitle]);

  const jumpToKey = (key: string) => {
    onSelect(key);
    requestAnimationFrame(() => scrollToKey(key, 'center'));
  };

  const startKey = toDayKey(days[0])!;
  const endKey = toDayKey(days[days.length - 1])!;
  const sorted = useMemo(() => [...eventDayKeys].filter(Boolean).sort(), [eventDayKeys]);
  const nextOutside = sorted.find((key) => key > endKey) || null;
  const prevOutside = [...sorted].reverse().find((key) => key < startKey) || null;

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between md:gap-3">
        <div className="myraion-display text-[20px] md:text-[28px] leading-tight text-[#1A1916]">{title}</div>
        <div className="flex items-center gap-2 self-start md:self-auto shrink-0">
          <button
            type="button"
            onClick={() => {
              onSelect(null);
              scrollToKey(todayKey, 'start');
            }}
            className="h-11 px-4 rounded-full bg-[#D5C8E8] text-[#1C1824] text-[12px] font-bold"
          >
            Все встречи
          </button>
          <button type="button" onClick={() => scrollByPage(-1)} className="w-11 h-11 rounded-full bg-[#FAF6F0] border border-[#1A1916]/12 text-[#1A1916] flex items-center justify-center hover:border-[#1A1916]/40">
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button type="button" onClick={() => scrollByPage(1)} className="w-11 h-11 rounded-full bg-[#FAF6F0] border border-[#1A1916]/12 text-[#1A1916] flex items-center justify-center hover:border-[#1A1916]/40">
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      </div>
      <p className="text-[11px] md:text-[12px] text-[#6B645C] whitespace-nowrap">Листай даты</p>

      <div
        ref={scrollerRef}
        className="flex gap-2 md:gap-2.5 overflow-x-auto pb-0.5 -mx-1 px-1 scroll-px-3 snap-x snap-mandatory touch-pan-x overscroll-x-contain [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden"
      >
        {days.map((day) => {
          const key = toDayKey(day)!;
          const active = selectedDayKey === key;
          const hasEvents = marked.has(key);
          const isToday = key === todayKey;
          return (
            <button
              key={key}
              type="button"
              data-day-key={key}
              data-today={isToday ? 'true' : undefined}
              onClick={() => onSelect(active ? null : key)}
              className={`snap-start shrink-0 w-[4.5rem] md:w-[6.25rem] h-[88px] md:h-[120px] rounded-[28px] md:rounded-[36px] flex flex-col items-center justify-center gap-1 md:gap-2 px-1 transition-all duration-200 ${
                active
                  ? 'bg-[#DDD4F0] text-[#1C1824] scale-[1.03]'
                  : isToday
                    ? 'bg-[#FFFCFA] text-[#1C1824] ring-1 ring-[#5C4B7A]/35'
                    : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'
              }`}
            >
              <span className={`w-1.5 h-1.5 rounded-full ${active || hasEvents ? 'bg-[#5C4B7A]' : 'bg-[#1C1824]/15'}`} />
              <span className={`text-[9px] md:text-[12px] uppercase tracking-[0.12em] md:tracking-[0.16em] ${active ? 'text-[#5C4B7A]' : 'text-[#8A8494]'}`}>
                {isToday ? 'сегодня' : WEEKDAYS_RU[day.getDay()]}
              </span>
              <span className="myraion-display text-[22px] md:text-[30px] leading-none">{day.getDate()}</span>
            </button>
          );
        })}
      </div>

      {(nextOutside || prevOutside) && (
        <div className="flex flex-wrap gap-4">
          {prevOutside && (
            <button type="button" onClick={() => jumpToKey(prevOutside)} className="text-[12px] text-[#6B645C] hover:text-[#1A1916] transition">
              ← {parseDayKey(prevOutside).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' })}
            </button>
          )}
          {nextOutside && (
            <button type="button" onClick={() => jumpToKey(nextOutside)} className="text-[12px] text-[#6B645C] hover:text-[#1A1916] transition">
              {parseDayKey(nextOutside).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' })} →
            </button>
          )}
        </div>
      )}
    </div>
  );
}
