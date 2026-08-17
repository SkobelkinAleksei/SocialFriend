import React, { useState, useEffect, useRef } from 'react';
import api from '@/shared/lib/api';
import { showAppInfoToast } from '@/shared/utils/appToast';
import { openNeighborProfile } from '@/shared/utils/navigation';
import { useAuth } from '@/shared/context/AuthContext';
import Card from '@/shared/ui/Card';
import Button from '@/shared/ui/Button';
import StatusMenuButton from '@/shared/ui/StatusMenuButton';
import DistrictMapSection from '@/features/map/DistrictMapSection';
import CreateEventDrawer from '@/features/events/CreateEventDrawer';
import { EVENT_CATEGORY_VISUAL, eventCategoryKey } from '@/features/events/eventVisuals';
import { resolveEventPhotoUrl } from '@/features/events/eventPhotos';
import {
  ShieldAlert,
  Flame,
  Sparkles,
  MapPin,
  Plus,
  Clock,
  Lock,
  Send,
  ThumbsUp,
  X, CheckCircle2, Users, Calendar
} from 'lucide-react';

export type EventCategory = 'SOS' | 'ДВИЖ' | 'СОБЫТИЯ';

export interface EventDto {
  id: number;
  title: string;
  description: string;
  category: EventCategory;
  isPrivate: boolean;
  latitude: number;
  longitude: number;
  locationName: string;
  organizerId: number;
  eventDate: string;
  isPast?: boolean;
  canVoteReputation?: boolean;
  reputationOpensAt?: string;
  reputationClosesAt?: string;
  participantLimit: number;
  currentParticipants: number;
  tags: string[];
  photos: string[];
  userStatus?: 'PENDING' | 'RE_PENDING' | 'JOINED' | 'NOT_PARTICIPATING' | 'REJECTED' | 'BANNED' | 'KICKED';
}

export default function District({ active = true }: { active?: boolean }) {
  const { user } = useAuth();
  const [activeCategory, setActiveCategory] = useState<EventCategory | 'ALL'>('ALL');
  const categoryChipRefs = useRef<Record<string, HTMLButtonElement | null>>({});

  const selectCategory = (key: EventCategory | 'ALL') => {
    const resolved = activeCategory === key && key !== 'ALL' ? 'ALL' : key;
    setActiveCategory(resolved);
    requestAnimationFrame(() => {
      categoryChipRefs.current[resolved]?.scrollIntoView({ inline: 'center', block: 'nearest', behavior: 'smooth' });
    });
  };
  const [events, setEvents] = useState<EventDto[]>([]);
  const [counts, setCounts] = useState({ all: 0, sos: 0, dvizh: 0, events: 0 });
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<any | null>(null);

  // Ссылка для удаленного управления модальным окном внутри карты
  const mapSectionRef = useRef<any>(null);

  const handleUpdateCounts = (newCounts: { all: number; sos: number; dvizh: number; events: number }) => {
    setCounts(newCounts);
  };

  // ИСПРАВЛЕНО: Самостоятельный обработчик кнопок ленты отправляет запросы на бэкенд напрямую
  const handleCardParticipantAction = async (p: any, forcedAction?: 'JOIN' | 'APPLY' | 'LEAVE') => {
    try {
      const isEventPrivate = !!p.isPrivate;
      let actionType = forcedAction;
      if (!actionType) {
        if (p.userStatus === 'JOINED') {
          actionType = 'LEAVE';
        } else {
          actionType = isEventPrivate ? 'APPLY' : 'JOIN';
        }
      }

      let endpoint = `/api/v1/social/events/${p.id}/participants/join`;
      if (actionType === 'APPLY') {
        endpoint = `/api/v1/social/events/${p.id}/participants/apply`;
      } else if (actionType === 'LEAVE') {
        endpoint = `/api/v1/social/events/${p.id}/participants/leave`;
      }

      console.log(`[Лента] Отправка запроса. Событие: ${p.id}, Действие: ${actionType}`);
      const response = await api.post(endpoint);

      if (response.data) {
        const statusResponse = await api.get(`/api/v1/social/events/${p.id}/participants/status`).catch(() => ({ data: { status: '' } }));

        const serverStatusStr = statusResponse.data?.status;
        const realStatus = serverStatusStr || (actionType === 'JOIN' ? 'JOINED' : actionType === 'APPLY' ? 'PENDING' : 'NOT_PARTICIPATING');
        const upperStatus = realStatus.toUpperCase();

        if (actionType === 'APPLY') {
          const wasRejected = String(p.userStatus || '').toUpperCase() === 'REJECTED';
          const title = p.title || 'встречу';
          showAppInfoToast(
            wasRejected ? 'Повторная заявка' : 'Заявка отправлена',
            wasRejected
              ? `Повторная заявка на «${title}» отправлена организатору`
              : `Заявка на приватную встречу «${title}» отправлена организатору`
          );
        }

        setEvents((prevEvents) =>
            prevEvents.map((item) => {
              if (String(item.id) === String(p.id)) {
                return {
                  ...item,
                  ...response.data,
                  userStatus: upperStatus,
                };
              }
              return item;
            })
        );

        // Синхронизируем слой маркеров на самой Leaflet-карте, чтобы они не рассинхронизировались
        mapSectionRef.current?.setEvents?.((prev: any[]) =>
            prev.map((item) => (String(item.id) === String(p.id) ? { ...item, ...response.data, userStatus: upperStatus } : item))
        );

        // Извещаем глобальный слой, что данные обновились (счётчики, вкладки заявок и т.д.)
        window.dispatchEvent(new CustomEvent('refreshApplicationsData'));
      }
    } catch (error) {
      console.error('Ошибка при обработке участия в событии:', error);
    }
  };

  const nearbyEvents = events.filter((p) => (p.eventDate ? new Date(p.eventDate).getTime() >= Date.now() : true));

  return (
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 md:py-8 min-h-screen">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-6 md:mb-8">
          <div>
            <p className="text-[11px] uppercase tracking-[0.22em] text-[#8A8494] mb-2">Карта · рядом</p>
            <h1 className="myraion-display text-[32px] sm:text-[44px] md:text-[52px] leading-[0.92] text-[#1C1824]">На районе</h1>
            <p className="text-sm text-[#8A8494] mt-3 max-w-xl">Что происходит рядом — события, активности и места в пешей доступности</p>
          </div>
          <Button
              onClick={() => setIsCreateOpen(true)}
              className="bg-[#5C4B7A] hover:bg-[#4A3C66] text-white min-h-11 py-3 px-5 rounded-full self-stretch sm:self-start flex items-center justify-center gap-2 text-sm"
          >
            <Plus className="w-4 h-4" /> Создать событие
          </Button>
        </div>

        <div className="flex flex-nowrap md:flex-wrap gap-2 mb-7 overflow-x-auto md:overflow-visible pb-0.5 -mx-4 px-4 md:mx-0 md:px-0 scroll-px-4 [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
          {([
            { key: 'ALL' as const, label: 'Все', count: counts.all, visual: EVENT_CATEGORY_VISUAL.default },
            { key: 'SOS' as const, label: 'SOS', count: counts.sos, visual: EVENT_CATEGORY_VISUAL.SOS },
            { key: 'ДВИЖ' as const, label: 'Движ', count: counts.dvizh, visual: EVENT_CATEGORY_VISUAL['Движ'] },
            { key: 'СОБЫТИЯ' as const, label: 'События', count: counts.events, visual: EVENT_CATEGORY_VISUAL['События'] },
          ]).map((item) => {
            const isActive = activeCategory === item.key;
            return (
              <button
                  key={item.key}
                  ref={(el) => { categoryChipRefs.current[item.key] = el; }}
                  onClick={() => selectCategory(item.key)}
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-full text-xs font-bold border transition duration-150 shrink-0 ${isActive ? item.visual.filterActive : item.visual.filterIdle}`}
              >
                {item.key === 'SOS' && <ShieldAlert className="w-4 h-4" />}
                {item.key === 'ДВИЖ' && <Flame className="w-4 h-4" />}
                {item.key === 'СОБЫТИЯ' && <Sparkles className="w-4 h-4" />}
                <span>{item.label}</span>
                <span className={`text-[15px] font-bold tabular-nums leading-none ${isActive ? item.visual.countActive : item.visual.count}`}>{item.count}</span>
              </button>
            );
          })}
          <span className="w-3 shrink-0 md:hidden" aria-hidden />
        </div>
        <div className="space-y-3">
            <DistrictMapSection
                ref={mapSectionRef}
                active={active}
                activeCategory={activeCategory}
                onUpdateCounts={handleUpdateCounts}
                onEventsChange={(visibleEvents: any[]) => setEvents(visibleEvents as EventDto[])}
            />
            <div className="flex flex-nowrap gap-2 select-none overflow-x-auto [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
              <span className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#F8E4E1] text-[#7A3A3A] text-[11px] font-bold tracking-[0.08em] uppercase shrink-0">
                <span className="w-2.5 h-2.5 rounded-full bg-[#B85C5C]" />
                SOS
              </span>
              <span className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#EDE6F5] text-[#5C4B7A] text-[11px] font-bold tracking-[0.08em] uppercase shrink-0">
                <span className="w-2.5 h-2.5 rounded-full bg-[#5C4B7A]" />
                Движ
              </span>
              <span className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#D4E8DC] text-[#3D6B56] text-[11px] font-bold tracking-[0.08em] uppercase shrink-0">
                <span className="w-2.5 h-2.5 rounded-full bg-[#3D6B56]" />
                События
              </span>
              <span className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#1C1824] text-[#FFFCFA] text-[11px] font-bold tracking-[0.08em] uppercase shrink-0">
                <span className="relative w-3.5 h-3.5 shrink-0">
                  <span className="absolute left-[33%] top-0 w-2.5 h-2.5 rounded-full bg-[#3D6B56] ring-1 ring-[#FFFCFA]" />
                  <span className="absolute left-0 top-0 w-2.5 h-2.5 rounded-full bg-[#5C4B7A] ring-1 ring-[#FFFCFA]" />
                  <span className="absolute left-[16.5%] top-[33%] w-2.5 h-2.5 rounded-full bg-[#FFFCFA]" />
                </span>
                Несколько событий
              </span>
            </div>
        </div>

        {/* Нижняя сетка точек поблизости */}
        <div className="mt-10">
          <h2 className="myraion-display text-2xl text-[#1C1824] tracking-tight">Все точки поблизости</h2>
          <p className="text-xs text-[#8A8494] mt-1 mb-5">Найдено {nearbyEvents.length} · отсортировано по дате</p>
          {nearbyEvents.length === 0 ? (
            <p className="text-sm text-[#8A8494]">Рядом пока нет встреч. Кликните по карте, чтобы создать первую.</p>
          ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {nearbyEvents
                .map((p) => {
                  const isEventPrivate = !!p.isPrivate;
                  const isHidden = isEventPrivate && p.organizerId !== user?.id && p.userStatus !== 'JOINED';


                  // Цвета категорий для маленьких баблов
                  const currentCatColor = EVENT_CATEGORY_VISUAL[eventCategoryKey(p.category)].stamp;
                  const cover = resolveEventPhotoUrl(p.photos?.[0]);
                  const wash = EVENT_CATEGORY_VISUAL[eventCategoryKey(p.category)].wash;

                  return (
                      <Card
                          key={p.id}
                          padded={false}
                          className="myraion-event-card overflow-hidden bg-[#FFFCFA] rounded-[32px] flex flex-col h-full cursor-pointer relative"
                          onClick={() => mapSectionRef.current?.openEvent(p)}
                      >
                        {/* ВЕРХНЯЯ ЧАСТЬ: ИЗОБРАЖЕНИЕ И БАБЛЫ СВЕРХУ */}
                        <div className="relative h-48 bg-slate-100 shrink-0 overflow-hidden group">
                          {isHidden ? (
                              // Эталонная приватная карточка из ТЗ: размытый фон, замочек и текст
                              <div className="w-full h-full bg-slate-200/80 flex flex-col items-center justify-center text-slate-400 gap-2 relative backdrop-blur-sm">
                                <div className="w-10 h-10 rounded-full bg-white flex items-center justify-center shadow-md text-slate-500">
                                  <Lock className="w-4 h-4" />
                                </div>
                                <span className="text-[11px] font-bold text-slate-500/90 tracking-wide mt-1">
            Фото откроется после одобрения
          </span>
                              </div>
                          ) : cover ? (
                              <>
                                <img
                                    src={cover}
                                    alt=""
                                    className="w-full h-full object-cover group-hover:scale-103 transition duration-500"
                                />
                                <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent" />
                              </>
                          ) : (
                              <div className={`w-full h-full bg-gradient-to-br ${wash}`} />
                          )}

                          {/* АБСОЛЮТНЫЕ БАБЛЫ ПОВЕРХ КАРТИНКИ */}
                          <div className="absolute top-3 left-3 right-3 flex items-center justify-between gap-2 z-20">
                            <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  if (p.organizerId) openNeighborProfile(p.organizerId);
                                }}
                                className="px-3 py-1 bg-white/95 backdrop-blur hover:bg-white text-slate-800 text-[11px] font-bold rounded-full shadow-sm transition border border-slate-100 flex items-center max-w-[150px] cursor-pointer hover:underline"
                            >
                              {/* Динамически вытягиваем имя создателя по его organizerId из UserController */}
                              <OrganizerName organizerId={p.organizerId} currentUserId={user?.id} />
                            </button>

                            <span className={`px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${currentCatColor}`}>
          {p.category}
        </span>
                          </div>
                        </div>

                        {/* НИЖНЯЯ ЧАСТЬ: ИНФО-ПОЛЯ И ПАСТЕЛЬНЫЕ КНОПКИ */}
                        <div className="px-5 pt-3 pb-5 flex flex-col justify-between flex-1 min-w-0">
                          <div>
                            {isEventPrivate ? (
                              <div className="inline-flex items-center gap-1 text-[10px] uppercase tracking-[0.14em] leading-none text-[#5C4B7A] mb-0.5">
                                <Clock className="w-3 h-3" /> По заявкам
                              </div>
                            ) : null}
                            <h4 className="myraion-display text-lg text-[#1C1824] leading-[1.05] tracking-tight line-clamp-2">
                              {p.title}
                            </h4>

                            {/* Список иконок и параметров с жестким сокрытием по эталону */}
                            <div className="mt-2 space-y-2 text-xs font-semibold text-slate-500">
                              {/* Дата и Время (Две строчки: сверху дата, снизу только время) */}
                              <div className="flex items-center gap-2.5 text-xs text-slate-700 font-bold">
                                <Calendar className="w-4 h-4 text-slate-400 shrink-0" />
                                <span>
                                    {(() => {
                                      if (!p.eventDate) return 'Дата уточняется';
                                      const dateObj = new Date(p.eventDate);
                                      const dayAndMonth = dateObj.toLocaleDateString('ru-RU', { month: 'long', day: 'numeric' });
                                      const hoursAndMins = dateObj.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
                                      return `${dayAndMonth} в ${hoursAndMins}`;
                                    })()}
                                  </span>
                              </div>

                              {/* Адрес */}
                              <div className="flex items-center gap-2.5">
                                <MapPin className="w-4 h-4 text-slate-400 shrink-0" />
                                <span className={`truncate ${isHidden ? "text-slate-400 font-medium italic" : "text-slate-700"}`}>
      {isHidden ? 'Адрес скрыт' : p.locationName}
    </span>
                              </div>

                              {/* Участники */}
                              <div className="flex items-center gap-2.5">
                                <Users className="w-4 h-4 text-slate-400 shrink-0" />
                                <span className={isHidden ? "text-slate-400 font-medium italic" : "text-slate-700"}>
      {isHidden ? 'Список скрыт' : `Участники: ${p.currentParticipants || 0}${p.participantLimit ? ` / ${p.participantLimit}` : ''}`}
    </span>
                              </div>
                            </div>

                          </div>

                          {/* СТИЛЬНЫЕ ПАСТЕЛЬНЫЕ ПЛАШКИ-КНОПКИ НА ВСЮ ШИРИНУ КАРТОЧКИ */}
                          <div className="mt-5 pt-3 border-t border-slate-100"
                               onClick={(e) => e.stopPropagation()}>
                            {p.organizerId === user?.id ? (
                                <div className="w-full bg-[#EDE6F5] text-[#5C4B7A] text-xs h-10 rounded-full flex items-center justify-center gap-1.5 select-none">
                                  Вы организатор встречи
                                </div>
                            ) : p.userStatus === 'JOINED' ? (
                                <StatusMenuButton
                                    className="w-full"
                                    variant="success"
                                    label="Вы идёте"
                                    buttonClassName="!h-10 !rounded-full"
                                    items={[{ label: 'Отказаться от участия', onClick: () => handleCardParticipantAction(p, 'LEAVE'), tone: 'danger' }]}
                                    desktop={
                                      <button onClick={() => handleCardParticipantAction(p, 'LEAVE')} className="w-full bg-[#D4E8DC] hover:bg-[#F3D4D0] text-[#3D6B56] hover:text-[#7A3A3A] text-xs font-bold h-10 rounded-full flex items-center justify-center gap-1.5 transition-all duration-200 group/btn">
                                        <CheckCircle2 className="w-4 h-4 text-[#3D6B56] group-hover/btn:hidden" />
                                        <X className="w-4 h-4 text-red-500 hidden group-hover/btn:inline" />
                                        <span className="group-hover/btn:hidden">Вы идёте · Подробнее</span>
                                        <span className="hidden group-hover/btn:inline">Отказаться от участия</span>
                                      </button>
                                    }
                                />
                            ) : p.userStatus === 'PENDING' ? (
                                <StatusMenuButton
                                    className="w-full"
                                    variant="amber"
                                    label="Заявка отправлена"
                                    buttonClassName="!h-10 !rounded-full"
                                    items={[{ label: 'Отменить заявку', onClick: () => handleCardParticipantAction(p, 'LEAVE'), tone: 'danger' }]}
                                    desktop={
                                      <button onClick={() => handleCardParticipantAction(p, 'LEAVE')} className="w-full bg-[#EDE6F5] hover:bg-[#F3D4D0] text-[#5C4B7A] hover:text-[#7A3A3A] text-xs font-bold h-10 rounded-full flex items-center justify-center gap-1.5 transition-all duration-200 group/pending">
                                        <Clock className="w-4 h-4 text-[#8A76B0] animate-pulse group-hover/pending:hidden" />
                                        <span className="group-hover/pending:hidden">Заявка отправлена</span>
                                        <X className="w-4 h-4 text-red-500 hidden group-hover/pending:inline" />
                                        <span className="hidden group-hover/pending:inline">Отменить заявку</span>
                                      </button>
                                    }
                                />
                            ) : p.userStatus === 'RE_PENDING' ? (
                                <StatusMenuButton
                                    className="w-full"
                                    variant="amber"
                                    label="Повторная заявка отправлена"
                                    buttonClassName="!h-10 !rounded-full"
                                    items={[{ label: 'Отменить повторную заявку', onClick: () => handleCardParticipantAction(p, 'LEAVE'), tone: 'danger' }]}
                                    desktop={
                                      <button onClick={() => handleCardParticipantAction(p, 'LEAVE')} className="w-full bg-[#F3D4D0] hover:bg-[#E8B8B4] text-[#7A3A3A] text-xs font-bold h-10 rounded-full flex items-center justify-center gap-1.5 transition-all duration-200 group/listRePending cursor-pointer">
                                        <Clock className="w-4 h-4 text-amber-500 animate-pulse group-hover/listRePending:hidden" />
                                        <span className="group-hover/listRePending:hidden">Повторная заявка отправлена</span>
                                        <X className="w-4 h-4 text-red-500 hidden group-hover/listRePending:inline" />
                                        <span className="hidden group-hover/listRePending:inline">Отменить повторную заявку</span>
                                      </button>
                                    }
                                />
                            ) : p.userStatus === 'REJECTED' ? (
                                <button
                                    onClick={() => handleCardParticipantAction(p, 'APPLY')}
                                    className="w-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white text-xs font-bold h-10 rounded-full transition flex items-center justify-center gap-1.5 active:scale-95"
                                >
                                  <Plus className="w-4 h-4" />
                                  <span>Подать заявку повторно</span>
                                </button>
                            ) : p.userStatus === 'KICKED' ? (
                                <div className="w-full bg-[#F3D4D0] text-[#7A3A3A] text-xs font-black h-10 rounded-full flex items-center justify-center gap-1.5 select-none">
                                  <X className="w-4 h-4 text-[#B85C5C]" />
                                  <span>Вас исключили из встречи</span>
                                </div>
                            ) : p.userStatus === 'BANNED' ? (
                                <div className="w-full bg-[#F3D4D0] text-[#7A3A3A] text-xs font-black h-10 rounded-full flex items-center justify-center gap-1.5 select-none">
                                  <X className="w-4 h-4 text-rose-500" />
                                  <span>Вам отказано организатором</span>
                                </div>
                            ) : isEventPrivate ? (
                                <button
                                    onClick={() => handleCardParticipantAction(p, 'APPLY')}
                                    className="w-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white text-xs font-bold h-10 rounded-full transition flex items-center justify-center gap-1.5 active:scale-98"
                                >
                                  <Send className="w-4 h-4" />
                                  <span>Оставить заявку</span>
                                </button>
                            ) : (
                                <button
                                    onClick={() => handleCardParticipantAction(p, 'JOIN')}
                                    className="w-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white text-xs font-bold h-10 rounded-full transition flex items-center justify-center gap-1.5 active:scale-98"
                                >
                                  <ThumbsUp className="w-4 h-4" />
                                  <span>Вступить в событие</span>
                                </button>
                            )}
                          </div>
                        </div>
                      </Card>
                  );
                })}
          </div>
          )}
        </div>

        <CreateEventDrawer
            open={isCreateOpen}
            initialDraft={editingEvent}
            onClose={() => {
              setIsCreateOpen(false);
              setEditingEvent(null);
            }}
            onSuccess={() => {
              setIsCreateOpen(false);
              setEditingEvent(null);

              if (mapSectionRef.current?.fetchEvents) {
                mapSectionRef.current.fetchEvents();
              } else {
                window.location.reload();
              }
            }}
        />
      </div>
  );
}

const organizerCache: Record<number, string> = {};

interface OrganizerNameProps {
  organizerId: number;
  currentUserId?: number;
}

export function OrganizerName({ organizerId, currentUserId }: OrganizerNameProps) {
  const [name, setName] = useState<string>(`Сосед #${organizerId}`);

  useEffect(() => {
    if (currentUserId && Number(organizerId) === Number(currentUserId)) {
      setName('Вы');
      return;
    }

    if (organizerCache[organizerId]) {
      setName(organizerCache[organizerId]);
      return;
    }

    // Делаем запрос к твоему реальному эндпоинту UserController
    api.get(`/api/v1/social/users/${organizerId}`)
        .then((res) => {
          // Вытаскиваем firstName и lastName согласно твоей структуре UserDto
          const { firstName, lastName } = res.data;
          if (firstName) {
            // Форматируем как "Имя Ф." (например, Артём С.)
            const formattedName = `${firstName} ${lastName ? lastName.charAt(0) + '.' : ''}`.trim();
            organizerCache[organizerId] = formattedName;
            setName(formattedName);
          }
        })
        .catch(() => {
          // Если профиль не успел загрузиться, оставляем дефолт
          setName(`Сосед #${organizerId}`);
        });
  }, [organizerId, currentUserId]);

  return <span className="truncate">{name}</span>;
}
