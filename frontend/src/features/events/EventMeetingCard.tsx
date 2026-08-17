import React from 'react';
import { Calendar, Clock, Lock, MapPin, Users } from 'lucide-react';
import { OrganizerNameModal } from '@/features/events/EventDetailsModal';
import { openNeighborProfile } from '@/shared/utils/navigation';
import { EVENT_CATEGORY_VISUAL, eventCategoryKey } from '@/features/events/eventVisuals';
import { resolveEventPhotoUrl } from '@/features/events/eventPhotos';

export type EventPreviewPerson = {
  firstName?: string;
  lastName?: string;
};

type EventMeetingCardProps = {
  title: string;
  date: string;
  location: string;
  participants: number;
  limit?: number;
  image?: string;
  category?: string;
  privacy?: 'open' | 'approval';
  locked?: boolean;
  organizerId?: number;
  currentUserId?: number;
  people?: EventPreviewPerson[];
  onClick?: () => void;
  footer?: React.ReactNode;
};

function initials(person?: EventPreviewPerson): string {
  const a = (person?.firstName || '').trim().charAt(0);
  const b = (person?.lastName || '').trim().charAt(0);
  return (a + b).toUpperCase() || '?';
}

export default function EventMeetingCard({
  title,
  date,
  location,
  participants,
  limit,
  image,
  category,
  privacy,
  locked,
  organizerId,
  currentUserId,
  people = [],
  onClick,
  footer,
}: EventMeetingCardProps) {
  const skin = EVENT_CATEGORY_VISUAL[eventCategoryKey(category)];
  const extra = Math.max(0, participants - Math.min(people.length, 3));
  const shown = people.slice(0, 3);

  return (
    <article
      onClick={onClick}
      className="myraion-event-card relative overflow-hidden bg-[#FFFCFA] rounded-[32px] flex flex-col h-full cursor-pointer"
    >
      <div className={`relative h-48 overflow-hidden ${locked ? 'bg-slate-100' : `bg-gradient-to-br ${skin.wash}`}`}>
        {locked ? (
          <div className="w-full h-full bg-slate-200/80 flex flex-col items-center justify-center text-slate-400 gap-2">
            <div className="w-10 h-10 rounded-full bg-white flex items-center justify-center shadow-md text-slate-500">
              <Lock className="w-4 h-4" />
            </div>
            <span className="text-[11px] font-bold text-slate-500/90 tracking-wide mt-1">
              Фото откроется после одобрения
            </span>
          </div>
        ) : resolveEventPhotoUrl(image) ? (
          <img src={resolveEventPhotoUrl(image)} alt="" className="w-full h-full object-cover" />
        ) : null}

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            if (organizerId) openNeighborProfile(organizerId);
          }}
          className="absolute top-3 left-3 px-3 py-1 bg-[#FFFCFA]/90 text-[#1C1824] text-[11px] rounded-full max-w-[160px] truncate cursor-pointer hover:underline z-20"
        >
          <OrganizerNameModal organizerId={Number(organizerId || 0)} currentUserId={currentUserId} />
        </button>

        <span className={`absolute top-3 right-3 px-2.5 py-1 rounded-full text-[10px] uppercase tracking-[0.16em] ${skin.stamp}`}>
          {category || 'Встреча'}
        </span>
      </div>

      <div className="px-5 pt-3 pb-5 flex flex-col flex-1">
        {privacy === 'approval' ? (
          <div className="inline-flex items-center gap-1 text-[10px] uppercase tracking-[0.14em] leading-none text-[#5C4B7A] mb-0.5">
            <Clock className="w-3 h-3" /> По заявкам
          </div>
        ) : null}

        <h3 className="myraion-display text-[22px] md:text-[24px] leading-[1.05] text-[#1C1824] line-clamp-2">
          {title}
        </h3>

        <div className={`mt-2.5 space-y-2 ${locked ? 'text-xs font-semibold text-slate-500' : 'text-[13px] text-[#5A5566]'}`}>
          <div className="flex items-center gap-2.5 min-h-[20px]">
            <Calendar className={`w-4 h-4 shrink-0 ${locked ? 'text-slate-400' : 'text-[#5C4B7A]'}`} />
            <span className={`truncate ${locked ? 'text-slate-700 font-bold' : ''}`}>{date || 'Дата уточняется'}</span>
          </div>
          <div className="flex items-center gap-2.5 min-h-[20px]">
            <MapPin className={`w-4 h-4 shrink-0 ${locked ? 'text-slate-400' : 'text-[#5C4B7A]'}`} />
            <span className={`truncate ${locked ? 'text-slate-400 font-medium italic' : ''}`}>
              {locked ? 'Адрес скрыт' : (location || 'Адрес уточняется')}
            </span>
          </div>
          {locked ? (
            <div className="flex items-center gap-2.5 min-h-[20px]">
              <Users className="w-4 h-4 shrink-0 text-slate-400" />
              <span className="text-slate-400 font-medium italic">Список скрыт</span>
            </div>
          ) : null}
        </div>

        <div className="mt-auto pt-5">
          {locked ? null : (
          <div className="flex items-center min-h-[32px]">
              <div className="flex items-center">
                {shown.map((person, idx) => (
                  <div
                    key={`${person.firstName}-${idx}`}
                    className="w-8 h-8 rounded-full border-2 border-[#FFFCFA] bg-[#EFEAF6] text-[10px] text-[#1C1824] flex items-center justify-center"
                    style={{ marginLeft: idx === 0 ? 0 : -8, zIndex: 10 - idx }}
                  >
                    {initials(person)}
                  </div>
                ))}
                {shown.length === 0 && (
                  <div className="w-8 h-8 rounded-full border-2 border-[#FFFCFA] bg-[#EFEAF6] text-[10px] text-[#8A8494] flex items-center justify-center">
                    +
                  </div>
                )}
                {extra > 0 && (
                  <div
                    className="w-8 h-8 rounded-full border-2 border-[#FFFCFA] bg-[#5C4B7A] text-[10px] text-white flex items-center justify-center"
                    style={{ marginLeft: -8 }}
                  >
                    +{extra}
                  </div>
                )}
                <span className="ml-2.5 leading-none">
                  <span className="myraion-display text-[20px] text-[#1C1824] tabular-nums">{participants}</span>
                  {limit ? <span className="text-[13px] text-[#8A8494] tabular-nums"> / {limit}</span> : null}
                </span>
              </div>
          </div>
          )}

          {footer && (
            <div className={`${locked ? 'mt-5 pt-3 border-t border-slate-100' : 'mt-4 pt-4 border-t border-[#1C1824]/10'}`} onClick={(e) => e.stopPropagation()}>
              {footer}
            </div>
          )}
        </div>
      </div>
    </article>
  );
}
