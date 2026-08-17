export type EventCategoryKey = 'SOS' | 'Движ' | 'События' | 'default';

export function eventCategoryKey(category?: string): EventCategoryKey {
  const cat = (category || '').toLowerCase().trim();
  if (cat === 'sos') return 'SOS';
  if (cat === 'движ') return 'Движ';
  if (cat === 'события') return 'События';
  return 'default';
}

/** Лаванда + крем с референсов: цвет живёт в шапке карточки, не в полоске. */
export const EVENT_CATEGORY_VISUAL = {
  SOS: {
    stamp: 'bg-[#B85C5C] text-white',
    wash: 'from-[#F3D4D0] to-[#E8B8B4]',
    filterIdle: 'bg-[#F8E4E1] border-transparent text-[#7A3A3A]',
    filterActive: 'bg-[#B85C5C] border-transparent text-white',
    count: 'text-[#B85C5C]',
    countActive: 'text-white',
  },
  'Движ': {
    stamp: 'bg-[#5C4B7A] text-white',
    wash: 'from-[#DDD4F0] to-[#C9BBE8]',
    filterIdle: 'bg-[#EDE6F5] border-transparent text-[#5C4B7A]',
    filterActive: 'bg-[#5C4B7A] border-transparent text-white',
    count: 'text-[#5C4B7A]',
    countActive: 'text-white',
  },
  'События': {
    stamp: 'bg-[#3D6B56] text-white',
    wash: 'from-[#D4E8DC] to-[#B8D4C4]',
    filterIdle: 'bg-[#D4E8DC] border-transparent text-[#3D6B56]',
    filterActive: 'bg-[#3D6B56] border-transparent text-white',
    count: 'text-[#3D6B56]',
    countActive: 'text-white',
  },
  default: {
    stamp: 'bg-[#1C1824] text-[#FFFCFA]',
    wash: 'from-[#E8E2F0] to-[#D5C8E8]',
    filterIdle: 'bg-[#FFFCFA] border border-[#1C1824]/12 text-[#1C1824]',
    filterActive: 'bg-[#1C1824] border-transparent text-[#FFFCFA]',
    count: 'text-[#5C4B7A]',
    countActive: 'text-[#EDE6F5]',
  },
} as const;

export function toDayKey(value: Date | string | null | undefined): string | null {
  if (!value) return null;
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return null;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function startOfDay(d: Date): Date {
  const next = new Date(d);
  next.setHours(0, 0, 0, 0);
  return next;
}

export function addDays(d: Date, amount: number): Date {
  const next = startOfDay(d);
  next.setDate(next.getDate() + amount);
  return next;
}
