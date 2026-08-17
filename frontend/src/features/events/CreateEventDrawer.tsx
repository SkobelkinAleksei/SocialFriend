import React, { useEffect, useState } from 'react';
import { useChat } from '@/features/chat/ChatContext';
import {
  X,
  MapPin,
  Image as ImageIcon,
  Lock,
  AlertTriangle,
  Zap,
  Sparkles,
  ChevronDown,
  Check,
} from 'lucide-react';
import Button from '@/shared/ui/Button';
import api, { uploadEventPhoto } from '@/shared/lib/api';
import { apiUrl } from '@/shared/lib/runtime';
import { collectEventPhotos, resolveEventPhotoUrl, toStoredEventPhoto } from '@/features/events/eventPhotos';
import { showAppInfoToast } from '@/shared/utils/appToast';
import { useAppBackHandler } from '@/shared/hooks/useAppBackHandler';
import {
  EVENT,
  eventDateBounds,
  hasProfanity,
  hasMarketplaceListing,
  MARKETPLACE_MESSAGE,
  validateEventDateTime,
  validateEventDescription,
  validateEventLimit,
  validateEventLocation,
  validateEventTitle,
} from '@/shared/lib/validation';

interface CreateEventDrawerProps {
  open: boolean;
  onClose: () => void;
  // Метод вызывается после успешного сохранения в базу бэкенда
  onSuccess: () => void;
  initialDraft?: any | null;
  mode?: 'create' | 'edit';
  // Передаем координаты клика с карты (если создаем из раздела "На районе")
  clickedCoords?: { lat: number; lng: number } | null;
}

const CATEGORIES = [
  {
    key: 'SOS',
    label: 'SOS',
    icon: AlertTriangle,
    activeCls: 'bg-[#F3D4D0] text-[#7A3A3A]',
    idleCls: 'bg-[#FFFCFA] text-[#8A8494] hover:bg-[#F3D4D0]/50',
  },
  {
    key: 'ДВИЖ',
    label: 'Движ',
    icon: Zap,
    activeCls: 'bg-[#DDD4F0] text-[#5C4B7A]',
    idleCls: 'bg-[#FFFCFA] text-[#8A8494] hover:bg-[#DDD4F0]/50',
  },
  {
    key: 'СОБЫТИЯ',
    label: 'События',
    icon: Sparkles,
    activeCls: 'bg-[#D4E8DC] text-[#3D6B56]',
    idleCls: 'bg-[#FFFCFA] text-[#8A8494] hover:bg-[#D4E8DC]/50',
  },
];

const FIELD =
  'w-full min-w-0 max-w-full box-border px-3 py-2.5 bg-[#FFFCFA] border border-[#1C1824]/10 rounded-2xl text-sm text-[#1C1824] placeholder:text-[#8A8494] focus:outline-none focus:border-[#5C4B7A]/40';
const LABEL = 'block text-xs text-[#8A8494] mb-1.5';

const QUICK_TAGS = ['спорт', 'музыка', 'настолки', 'активность', 'кофе', 'прогулка'];
const PRIVACY_OPTIONS = [
  { value: 'false', label: 'Открытая встреча (все могут вступить)' },
  { value: 'true', label: 'По одобрению заявки (приватная)' },
];

function readDraftCoords(draft: any): { lat: number; lng: number } | null {
  if (!draft) return null;
  const lat = Number(draft.latitude ?? draft.lat);
  const lng = Number(draft.longitude ?? draft.lng);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  return { lat, lng };
}

function isPrivateDraft(draft: any): boolean {
  if (!draft) return false;
  if (draft.isPrivate === true || draft.isPrivate === 'true') return true;
  return draft.privacy === 'approval';
}

export default function CreateEventDrawer({
                                            open,
                                            onClose,
                                            onSuccess,
                                            initialDraft = null,
                                            mode = 'create',
                                            clickedCoords = null,
                                          }: CreateEventDrawerProps) {
  const { refreshEventRooms } = useChat(); // Получаем метод принудительного обновления комнат
  useAppBackHandler(open, onClose);

  const [title, setTitle] = useState('');
  const [category, setCategory] = useState<string>('СОБЫТИЯ');
  const [isPrivate, setIsPrivate] = useState('false');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [location, setLocation] = useState('');
  const [limit, setLimit] = useState(10);
  const [tags, setTags] = useState<string[]>([]);
  const [description, setDescription] = useState('');
  const [photos, setPhotos] = useState<(string | null)[]>([null, null, null, null, null]);
  const [coverIndex, setCoverIndex] = useState(0);
  const [uploadingSlot, setUploadingSlot] = useState<number | null>(null);
  const [showCancelTooltip, setShowCancelTooltip] = useState(false);
  const [formError, setFormError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [tagDraft, setTagDraft] = useState('');
  const [pointFromSuggest, setPointFromSuggest] = useState(false);

  // Координаты для DaData
  const [eventLat, setEventLat] = useState<number>(clickedCoords?.lat || 55.7558);
  const [eventLng, setEventLng] = useState<number>(clickedCoords?.lng || 37.6173);
  const [addressSuggestions, setAddressSuggestions] = useState<any[]>([]);
  const [showAddressDropdown, setShowAddressDropdown] = useState(false);
  const [selectedAddressValue, setSelectedAddressValue] = useState<string | null>(null);
  useEffect(() => {
    if (clickedCoords) {
      setEventLat(clickedCoords.lat);
      setEventLng(clickedCoords.lng);
      setPointFromSuggest(true);
    }
  }, [clickedCoords]);

  useEffect(() => {
    if (!open) {
      setTitle('');
      setCategory('СОБЫТИЯ');
      setIsPrivate('false');
      setDate('');
      setTime('');
      setLocation('');
      setSelectedAddressValue(null);
      setAddressSuggestions([]);
      setShowAddressDropdown(false);
      setLimit(10);
      setTags([]);
      setDescription('');
      setPhotos([null, null, null, null, null]);
      setCoverIndex(0);
      setUploadingSlot(null);
      setFormError('');
      setFieldErrors({});
      setTagDraft('');
      setPointFromSuggest(false);
      setEventLat(clickedCoords?.lat || 55.7558);
      setEventLng(clickedCoords?.lng || 37.6173);
      return;
    }
    if (!initialDraft) return;

    setTitle(initialDraft.title || '');
    setCategory((initialDraft.category || 'СОБЫТИЯ').toUpperCase().trim());
    setIsPrivate(isPrivateDraft(initialDraft) ? 'true' : 'false');
    const draftLocation = String(initialDraft.locationName || initialDraft.location || '').trim();
    const placeholderLocation = draftLocation === 'Адрес уточняется' ? '' : draftLocation;
    setLocation(placeholderLocation);
    setSelectedAddressValue(placeholderLocation || null);
    setLimit(Math.min(300, Math.max(2, initialDraft.participantLimit || initialDraft.limit || 10)));
    setTags(initialDraft.tags || []);
    setDescription(initialDraft.description || '');
    const existingPhotos = collectEventPhotos(initialDraft);
    setPhotos(existingPhotos);
    setCoverIndex(existingPhotos.findIndex(Boolean) >= 0 ? existingPhotos.findIndex(Boolean) : 0);
    const coords = readDraftCoords(initialDraft);
    if (coords) {
      setEventLat(coords.lat);
      setEventLng(coords.lng);
    }
    if (placeholderLocation || coords) {
      setPointFromSuggest(true);
    }
    if (initialDraft.eventDate) {
      const dObj = new Date(initialDraft.eventDate);
      if (!isNaN(dObj.getTime())) {
        setDate(dObj.toISOString().slice(0, 10));
        setTime(dObj.toTimeString().slice(0, 5));
      }
    }
    // Если пришла уже отформатированная строка (как на странице "События")
    else if (initialDraft.date) {
      try {
        const timeMatch = initialDraft.date.match(/(\d{2}):(\d{2})/);
        if (timeMatch) {
          setTime(timeMatch[0]);
        }
        const dayMatch = initialDraft.date.match(/^\d+/);
        const day = dayMatch ? parseInt(dayMatch[0], 10) : new Date().getDate();
        const months = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
        const dateLower = initialDraft.date.toLowerCase();
        let monthIdx = new Date().getMonth();
        months.forEach((m, idx) => {
          if (dateLower.includes(m)) {
            monthIdx = idx;
          }
        });
        const currentYear = new Date().getFullYear();
        const dObj = new Date(currentYear, monthIdx, day);
        const offset = dObj.getTimezoneOffset();
        const localDate = new Date(dObj.getTime() - (offset * 60 * 1000));
        setDate(localDate.toISOString().slice(0, 10));
      } catch (e) {
        console.error("Не удалось распарсить русскую строку даты:", e);
        setDate(new Date().toISOString().slice(0, 10));
        setTime("18:00");
      }
    }

    if (mode !== 'edit' || !initialDraft.id || coords) return;
    let cancelled = false;
    api.get(`/api/v1/social/events/${initialDraft.id}`).then((res) => {
      if (cancelled) return;
      const fetched = readDraftCoords(res.data);
      if (fetched) {
        setEventLat(fetched.lat);
        setEventLng(fetched.lng);
      }
      const fetchedLocation = String(res.data?.locationName || '').trim();
      if (fetchedLocation) {
        setLocation(fetchedLocation);
        setSelectedAddressValue(fetchedLocation);
      }
      setPointFromSuggest(true);
      if (typeof res.data?.isPrivate === 'boolean') {
        setIsPrivate(res.data.isPrivate ? 'true' : 'false');
      }
    }).catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [open, initialDraft, mode]);

  const handleAddressChange = async (text: string) => {
    setLocation(text);
    if (selectedAddressValue && text === selectedAddressValue) {
      setAddressSuggestions([]);
      setShowAddressDropdown(false);
      return;
    }
    if (selectedAddressValue && text !== selectedAddressValue) {
      setSelectedAddressValue(null);
      setPointFromSuggest(Boolean(clickedCoords));
    }
    if (text.trim().length < 3) {
      setAddressSuggestions([]);
      setShowAddressDropdown(false);
      return;
    }
    try {
      const response = await fetch(apiUrl('/api/v1/geo/suggest/address'), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Accept": "application/json"
        },
        body: JSON.stringify({ query: text, count: 5 })
      });
      const data = await response.json();
      setAddressSuggestions(data.suggestions || []);
      setShowAddressDropdown(true);
    } catch (error) {
      console.error("Ошибка автоподбора адреса:", error);
    }
  };
  const handlePublish = async (e: React.FormEvent) => {
    e.preventDefault();
    const next: Record<string, string> = {};
    const titleErr = validateEventTitle(title);
    const descErr = validateEventDescription(description);
    const savedAddressUnchanged = selectedAddressValue != null
      && location.trim() === selectedAddressValue.trim();
    const locErr = validateEventLocation(
      location,
      pointFromSuggest || Boolean(clickedCoords) || (mode === 'edit' && savedAddressUnchanged)
    );
    const dateErr = mode === 'create' ? validateEventDateTime(date, time) : (!date || !time ? 'Укажите дату и время.' : null);
    const limitErr = validateEventLimit(Number(limit));
    if (titleErr) next.title = titleErr;
    if (descErr) next.description = descErr;
    if (locErr) next.location = locErr;
    if (dateErr) next.datetime = dateErr;
    if (limitErr) next.limit = limitErr;
    if (Object.keys(next).length > 0) {
      setFieldErrors(next);
      setFormError('');
      const order = ['title', 'location', 'datetime', 'limit', 'description'];
      const first = order.find((key) => next[key]);
      if (first) {
        document.getElementById(`event-field-${first}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      return;
    }
    setFieldErrors({});
    setFormError('');
    try {
      const fullDateTime = `${date}T${time}:00`;
      const filled = photos
        .map((item) => toStoredEventPhoto(item))
        .filter(Boolean);
      const cover = toStoredEventPhoto(photos[coverIndex]);
      const orderedPhotos = cover
        ? [cover, ...filled.filter((item) => item !== cover)]
        : filled;
      let lat = eventLat;
      let lng = eventLng;
      const originalLocation = String(initialDraft?.locationName || initialDraft?.location || '').trim();
      const addressUnchanged = mode === 'edit'
        && originalLocation
        && originalLocation !== 'Адрес уточняется'
        && location.trim() === originalLocation;
      if (addressUnchanged) {
        const draftCoords = readDraftCoords(initialDraft);
        if (draftCoords) {
          lat = draftCoords.lat;
          lng = draftCoords.lng;
        } else if (initialDraft?.id && !clickedCoords) {
          try {
            const res = await api.get(`/api/v1/social/events/${initialDraft.id}`);
            const fetched = readDraftCoords(res.data);
            if (fetched) {
              lat = fetched.lat;
              lng = fetched.lng;
            }
          } catch (err) {
            console.error('Не удалось подгрузить координаты события:', err);
          }
        }
      }
      const payload = {
        title: title.trim(),
        description: description.trim(),
        category: category,
        isPrivate: isPrivate === 'true',
        latitude: lat,
        longitude: lng,
        locationName: location.trim(),
        eventDate: fullDateTime,
        participantLimit: Number(limit),
        tags: tags,
        photos: orderedPhotos
      };
      if (mode === 'edit' && initialDraft?.id) {
        await api.put(`/api/v1/social/events/${initialDraft.id}`, payload);
        await refreshEventRooms();
      } else {
        await api.post('/api/v1/social/events', payload);
        window.setTimeout(async () => {
          await refreshEventRooms();
        }, 500);
      }
      onSuccess();
    } catch (error: any) {
      console.error("Ошибка при сохранении события на бэкенде:", error);
      const data = error?.response?.data;
      const fieldErrors = data?.fieldErrors;
      let msg = data?.detail || data?.message;
      if (Array.isArray(fieldErrors) && fieldErrors.length > 0) {
        msg = fieldErrors.map((fe: { message?: string } | string) =>
          typeof fe === 'string' ? fe : fe.message
        ).filter(Boolean).join('; ');
      } else if (fieldErrors && typeof fieldErrors === 'object') {
        const mapped: Record<string, string> = {};
        Object.entries(fieldErrors as Record<string, string>).forEach(([key, value]) => {
          if (key.includes('title')) mapped.title = String(value);
          else if (key.includes('description')) mapped.description = String(value);
          else if (key.includes('location')) mapped.location = String(value);
          else if (key.includes('eventDate') || key.includes('Date')) mapped.datetime = String(value);
          else if (key.includes('Limit') || key.includes('limit')) mapped.limit = String(value);
          else if (key.includes('tags')) mapped.tags = String(value);
        });
        if (Object.keys(mapped).length > 0) {
          setFieldErrors(mapped);
          const order = ['title', 'location', 'datetime', 'limit', 'tags', 'description'];
          const firstKey = order.find((key) => mapped[key]);
          if (firstKey) {
            document.getElementById(`event-field-${firstKey}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
          }
          return;
        }
        msg = Object.values(fieldErrors).filter(Boolean).join('; ');
      }
      setFormError(msg || 'Не удалось сохранить событие');
    }
  };

  const toggleTag = (t: string) => {
    setTags((prev) => {
      if (prev.includes(t)) return prev.filter((x) => x !== t);
      if (prev.length >= EVENT.tagsMax) return prev;
      return [...prev, t];
    });
    setFieldErrors((prev) => ({ ...prev, tags: '' }));
  };

  const addCustomTag = () => {
    const t = tagDraft.trim().replace(/^#/, '').toLowerCase();
    if (!t) return;
    if (t.length < EVENT.tagMin || t.length > EVENT.tagMax) {
      setFieldErrors((prev) => ({ ...prev, tags: `Тег — от ${EVENT.tagMin} до ${EVENT.tagMax} символов.` }));
      return;
    }
    if (hasProfanity(t)) {
      setFieldErrors((prev) => ({ ...prev, tags: 'В теге нельзя использовать ненормативную лексику.' }));
      return;
    }
    if (hasMarketplaceListing(t)) {
      setFieldErrors((prev) => ({ ...prev, tags: MARKETPLACE_MESSAGE }));
      return;
    }
    if (tags.includes(t)) {
      setTagDraft('');
      return;
    }
    if (tags.length >= EVENT.tagsMax) {
      setFieldErrors((prev) => ({ ...prev, tags: 'Можно указать не больше 8 тегов.' }));
      return;
    }
    setTags((prev) => [...prev, t]);
    setTagDraft('');
    setFieldErrors((prev) => ({ ...prev, tags: '' }));
  };

  const handlePhotoPick = async (index: number, file?: File | null) => {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      showAppInfoToast('Фото', 'Можно выбрать только изображение');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      showAppInfoToast('Фото', 'Файл больше 5 МБ');
      return;
    }
    const preview = URL.createObjectURL(file);
    setPhotos((prev) => {
      const next = [...prev];
      next[index] = preview;
      return next;
    });
    if (!photos.some(Boolean)) setCoverIndex(index);
    setUploadingSlot(index);
    try {
      const url = await uploadEventPhoto(file);
      setPhotos((prev) => {
        const next = [...prev];
        if (next[index] === preview) next[index] = resolveEventPhotoUrl(url);
        return next;
      });
      URL.revokeObjectURL(preview);
    } catch (error) {
      console.error('Ошибка загрузки фото события:', error);
      showAppInfoToast('Фото', 'Не удалось загрузить изображение');
      setPhotos((prev) => {
        const next = [...prev];
        next[index] = null;
        return next;
      });
      URL.revokeObjectURL(preview);
    } finally {
      setUploadingSlot((current) => (current === index ? null : current));
    }
  };

  const removePhoto = (index: number) => {
    setPhotos((prev) => {
      const next = [...prev];
      next[index] = null;
      return next;
    });
    setCoverIndex((current) => {
      if (current !== index) return current;
      const remaining = photos.findIndex((item, slot) => slot !== index && item);
      return remaining >= 0 ? remaining : 0;
    });
  };

  const dateBounds = eventDateBounds();
  const canPublish = title.trim().length >= EVENT.titleMin && location.trim().length >= EVENT.locationMin && date && time && uploadingSlot === null;
  const tagColor = category === 'SOS' ? 'bg-[#F3D4D0] text-[#7A3A3A]' : category === 'ДВИЖ' ? 'bg-[#DDD4F0] text-[#5C4B7A]' : 'bg-[#D4E8DC] text-[#3D6B56]';

  useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  if (!open) return null;

  return (
      <div className="fixed inset-0 z-50 overflow-x-hidden overflow-y-hidden md:overflow-y-auto">
        <div
            className="fixed inset-0 bg-[#1C1824]/45 backdrop-blur-sm"
            onClick={onClose}
        />
        <div
            className="relative min-h-full w-full min-w-0 max-w-full flex justify-center items-stretch md:items-start py-0 md:py-10 px-0 md:px-4 overflow-x-hidden"
            onClick={onClose}
        >
          <form
              onSubmit={handlePublish}
              onClick={(e) => e.stopPropagation()}
              className="pointer-events-auto w-full min-w-0 max-w-full md:max-w-md bg-[#FFFCFA] rounded-none md:rounded-[28px] px-4 py-5 md:p-6 pt-[max(1.25rem,env(safe-area-inset-top))] pb-[calc(1.25rem+env(safe-area-inset-bottom))] shadow-2xl relative flex flex-col space-y-4 animate-in slide-in-from-bottom-4 md:zoom-in-95 h-[100dvh] max-h-[100dvh] overflow-x-hidden overflow-y-auto md:h-auto md:max-h-none md:overflow-visible box-border"
          >
            <button
                type="button"
                onClick={onClose}
                className="absolute right-4 top-[max(1rem,env(safe-area-inset-top))] md:top-4 w-9 h-9 rounded-full bg-white text-[#1C1824] hover:bg-[#EFEAF6] flex items-center justify-center transition border border-[#1C1824]/10"
                aria-label="Закрыть"
            >
              <X className="w-4 h-4" />
            </button>
            <div className="pr-10">
              <h3 className="myraion-display text-[28px] leading-tight text-[#1C1824]">
                {mode === 'edit' ? 'Редактировать событие' : 'Создать событие'}
              </h3>
              <p className="text-sm text-[#8A8494] mt-1">
                {mode === 'edit' ? 'Обновите информацию о встрече' : 'Расскажите соседям о встрече'}
              </p>
            </div>
            {formError && (
              <div className="inline-flex self-start px-3 py-1.5 rounded-full bg-[#F3D4D0] text-[#7A3A3A] text-[11px] font-medium">
                {formError}
              </div>
            )}
            <div id="event-field-title">
              <label className={LABEL}>Название события <HintMark text="От 3 до 50 символов." /></label>
              <div className="relative">
                <input
                    type="text"
                    value={title}
                    onChange={(e) => {
                      setTitle(e.target.value.slice(0, EVENT.titleMax));
                      setFieldErrors((prev) => ({ ...prev, title: '' }));
                    }}
                    placeholder="Например, «Пробежка в парке»"
                    className={`${FIELD} pr-14`}
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-[12px] text-[#8A8494] tabular-nums">
 {title.length} / {EVENT.titleMax}
 </span>
              </div>
              <FieldError text={fieldErrors.title} />
            </div>
            <div>
              <label className={LABEL}>Категория события</label>
              <div className="grid grid-cols-3 gap-2 min-w-0">
                {CATEGORIES.map((c) => {
                  const Icon = c.icon;
                  const active = category === c.key;
                  return (
                      <button
                          key={c.key}
                          type="button"
                          onClick={() => setCategory(c.key)}
                          className={`min-w-0 flex items-center justify-center gap-1 py-2.5 text-xs rounded-full transition ${
                              active ? c.activeCls : c.idleCls
                          }`}
                      >
                        <Icon className="w-3.5 h-3.5 shrink-0" />
                        <span className="truncate">{c.label}</span>
                      </button>
                  );
                })}
              </div>
            </div>
            <div>
              <label className={LABEL}>Приватность</label>
              <div className="relative">
                <select
                    value={isPrivate}
                    onChange={(e) => setIsPrivate(e.target.value)}
                    className={`${FIELD} appearance-none cursor-pointer`}
                >
                  {PRIVACY_OPTIONS.map((o) => (
                      <option key={o.value} value={o.value}>
                        {o.label}
                      </option>
                  ))}
                </select>
                <ChevronDown className="w-3.5 h-3.5 text-[#8A8494] absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              </div>
            </div>
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="block text-xs text-[#8A8494]">Ограничение по репутации</label>
                <span className="px-2 py-0.5 bg-[#EDE6F5] text-[#5C4B7A] rounded-full text-[9px] uppercase tracking-wider flex items-center gap-1">
 <Lock className="w-2.5 h-2.5" /> Скоро
 </span>
              </div>
              <input
                  type="text"
                  disabled
                  placeholder="Минимальный уровень репутации"
                  className={`${FIELD} opacity-50 cursor-not-allowed`}
              />
            </div>
            <div id="event-field-location" className="relative">
              <label className={LABEL}>Место встречи <HintMark text="Выберите адрес из подсказок — карта сама поставится по нему." /></label>
              <div className="relative">
                <MapPin className="w-3.5 h-3.5 text-[#5C4B7A] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                    type="text"
                    value={location}
                    maxLength={EVENT.locationMax}
                    onChange={(e) => {
                      handleAddressChange(e.target.value);
                      setFieldErrors((prev) => ({ ...prev, location: '' }));
                    }}
                    placeholder="Начните вводить адрес (улица, дом)..."
                    className={`${FIELD} pl-9`}
                />
              </div>
              <FieldError text={fieldErrors.location} />
              {showAddressDropdown && addressSuggestions.length > 0 && (
                  <div className="absolute left-0 right-0 top-full mt-1 bg-[#FFFCFA] border border-[#1C1824]/10 rounded-2xl shadow-lg z-30 max-h-40 overflow-y-auto py-1">
                    {addressSuggestions.map((suggestion, idx) => (
                        <button
                            type="button"
                            key={idx}
                            onClick={() => {
                              setSelectedAddressValue(suggestion.value);
                              setLocation(suggestion.value);
                              if (suggestion.data?.geo_lat && suggestion.data?.geo_lon) {
                                setEventLat(parseFloat(suggestion.data.geo_lat));
                                setEventLng(parseFloat(suggestion.data.geo_lon));
                                setPointFromSuggest(true);
                              }
                              setAddressSuggestions([]);
                              setShowAddressDropdown(false);
                            }}
                            className="w-full text-left px-3 py-2 text-xs text-[#1C1824] hover:bg-[#EFEAF6] transition last:border-0 truncate"
                        >
                          {suggestion.value}
                        </button>
                    ))}
                  </div>
              )}
            </div>
            <div id="event-field-datetime" className="grid grid-cols-1 md:grid-cols-2 gap-3 min-w-0">
              <div className="min-w-0 relative">
                <div className="flex items-center gap-1.5 mb-1.5">
                  <label className="block text-xs text-[#8A8494]">Дата <HintMark text="Не раньше чем через 15 минут и не позже чем через 3 месяца." /></label>
                  <button
                      type="button"
                      onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        setShowCancelTooltip((open) => !open);
                      }}
                      className="w-5 h-5 rounded-full bg-[#EDE6F5] hover:bg-[#DDD4F0] text-[#5C4B7A] flex items-center justify-center text-xs transition shrink-0"
                      title="Посмотреть правила отмены"
                  >
                    ?
                  </button>
                </div>
                {showCancelTooltip && (
                    <>
                      <div className="fixed inset-0 z-30" onClick={() => setShowCancelTooltip(false)} />
                      <div className="absolute left-0 top-8 w-[min(16rem,calc(100vw-2rem))] p-3 bg-[#1C1824] text-[#FFFCFA] rounded-2xl shadow-xl z-40 text-[11px] leading-normal animate-in fade-in slide-in-from-top-1 duration-150">
                        <div className="font-bold text-[#D5C8E8] mb-0.5">Правила платформы:</div>
                        Отмена или удаление этой встречи организатором будут заблокированы менее чем за 12 часов до её фактического начала. Участники могут понизить ваш рейтинг, в случае, если событие не произойдет.
                      </div>
                    </>
                )}
                <input
                    type="date"
                    required
                    min={dateBounds.min}
                    max={dateBounds.max}
                    value={date}
                    onChange={(e) => {
                      setDate(e.target.value);
                      setFieldErrors((prev) => ({ ...prev, datetime: '' }));
                    }}
                    className={FIELD}
                />
              </div>

              <div className="min-w-0">
                <label className={LABEL}>Время <HintMark text="Не раньше чем через 15 минут и не позже чем через 3 месяца." /></label>
                <input
                    type="time"
                    required
                    value={time}
                    onChange={(e) => {
                      setTime(e.target.value);
                      setFieldErrors((prev) => ({ ...prev, datetime: '' }));
                    }}
                    className={FIELD}
                />
              </div>
              <div className="md:col-span-2 min-w-0">
                <FieldError text={fieldErrors.datetime} />
              </div>
            </div>

            <div id="event-field-limit">
              <label className={LABEL}>Лимит участников <HintMark text="От 2 до 300 человек." /></label>
              <div className="flex items-center rounded-full bg-[#EFEAF6] overflow-hidden max-w-[140px]">
                <button
                    type="button"
                    onClick={() => setLimit((v) => Math.max(2, v - 1))}
                    className="px-3 py-2 text-[#5C4B7A] hover:bg-[#DDD4F0]"
                >
                  -
                </button>
                <span className="flex-1 text-center myraion-display text-[18px] text-[#1C1824] tabular-nums">{limit}</span>
                <button
                    type="button"
                    onClick={() => setLimit((v) => Math.min(EVENT.limitMax, v + 1))}
                    className="px-3 py-2 text-[#5C4B7A] hover:bg-[#DDD4F0]"
                >
                  +
                </button>
              </div>
              <FieldError text={fieldErrors.limit} />
            </div>
            <div id="event-field-tags">
              <label className={LABEL}>Теги <HintMark text="До 8 тегов. Можно выбрать готовые или написать свои (2–24 символа)." /></label>
              <div className="flex flex-wrap gap-1.5 mb-2">
                {QUICK_TAGS.map((t) => {
                  const active = tags.includes(t);
                  return (
                      <button
                          key={t}
                          type="button"
                          onClick={() => toggleTag(t)}
                          className={`px-2.5 py-1 rounded-full text-[11px] transition ${
                              active ? tagColor : 'bg-white text-[#8A8494] hover:bg-[#EFEAF6]'
                          }`}
                      >
                        #{t}
                      </button>
                  );
                })}
              </div>
              {tags.filter((t) => !QUICK_TAGS.includes(t)).length > 0 && (
                <div className="flex flex-wrap gap-1.5 mb-2">
                  {tags.filter((t) => !QUICK_TAGS.includes(t)).map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => toggleTag(t)}
                      className={`px-2.5 py-1 rounded-full text-[11px] ${tagColor}`}
                    >
                      #{t} ×
                    </button>
                  ))}
                </div>
              )}
              <div className="flex gap-2 min-w-0">
                <div className="min-w-0 flex-1">
                  <input
                      type="text"
                      value={tagDraft}
                      maxLength={EVENT.tagMax}
                      onChange={(e) => setTagDraft(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          addCustomTag();
                        }
                      }}
                      placeholder="Свой тег, затем Enter"
                      className={FIELD}
                      disabled={tags.length >= EVENT.tagsMax}
                  />
                </div>
                <button
                    type="button"
                    onClick={addCustomTag}
                    disabled={tags.length >= EVENT.tagsMax}
                    className="shrink-0 px-3 rounded-2xl bg-[#EFEAF6] text-[#5C4B7A] text-xs hover:bg-[#DDD4F0] disabled:opacity-40"
                >
                  Добавить
                </button>
              </div>
              <FieldError text={fieldErrors.tags} />
            </div>
            <div id="event-field-description">
              <label className={LABEL}>Описание (необязательно)</label>
              <textarea
                  value={description}
                  onChange={(e) => {
                    setDescription(e.target.value.slice(0, EVENT.descriptionMax));
                    setFieldErrors((prev) => ({ ...prev, description: '' }));
                  }}
                  placeholder="Детали встречи, что взять с собой..."
                  className={`${FIELD} h-16 resize-none`}
              />
              <p className="text-[11px] text-[#8A8494] mt-1 text-right tabular-nums">{description.length} / {EVENT.descriptionMax}</p>
              <FieldError text={fieldErrors.description} />
            </div>
            <div>
              <label className={LABEL}>Фотографии <span className="text-[#8A8494]">(до {EVENT.photosMax}, отметьте обложку)</span></label>
              <div className="grid grid-cols-3 gap-2">
                {photos.map((p, i) => (
                    <div key={i} className="relative aspect-square">
                      {p ? (
                          <>
                            <img src={resolveEventPhotoUrl(p)} alt="" className="w-full h-full object-cover rounded-2xl" />
                            {uploadingSlot === i && (
                                <div className="absolute inset-0 bg-[#1C1824]/40 rounded-2xl flex items-center justify-center">
                                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white" />
                                </div>
                            )}
                            <button
                                type="button"
                                onClick={() => removePhoto(i)}
                                className="absolute top-1 right-1 w-5 h-5 bg-[#FFFCFA] rounded-full flex items-center justify-center text-[#8A8494] hover:text-[#B85C5C] transition"
                            >
                              <X className="w-3 h-3" />
                            </button>
                            <label className={`absolute left-1 bottom-1 flex items-center gap-1 px-2 py-1 rounded-full text-[10px] cursor-pointer ${coverIndex === i ? 'bg-[#5C4B7A] text-white' : 'bg-[#FFFCFA]/95 text-[#1C1824]'}`}>
                              <input
                                  type="checkbox"
                                  checked={coverIndex === i}
                                  onChange={() => setCoverIndex(i)}
                                  className="hidden"
                              />
                              {coverIndex === i && <Check className="w-3 h-3" />}
                              Обложка
                            </label>
                          </>
                      ) : (
                          <label className="w-full h-full flex flex-col items-center justify-center rounded-2xl border border-dashed border-[#1C1824]/15 bg-[#EFEAF6] hover:bg-[#DDD4F0] text-[#8A8494] hover:text-[#5C4B7A] transition cursor-pointer">
                            <input
                                type="file"
                                accept="image/jpeg,image/png,image/webp,image/gif"
                                className="hidden"
                                onChange={(e) => {
                                  const file = e.target.files?.[0];
                                  e.target.value = '';
                                  handlePhotoPick(i, file);
                                }}
                            />
                            <ImageIcon className="w-4 h-4" />
                            <span className="text-[9px] mt-1">Фото {i + 1}</span>
                          </label>
                      )}
                    </div>
                ))}
              </div>
            </div>
            <div className="pt-2 flex gap-2 justify-end">
              <Button type="button" variant="secondary" onClick={onClose} className="text-xs py-2.5 px-4 rounded-full bg-white text-[#1C1824] border-0 hover:bg-[#EFEAF6]">
                Отмена
              </Button>
              <Button
                  type="submit"
                  disabled={!canPublish}
                  className="bg-[#5C4B7A] hover:bg-[#4A3C66] text-white py-2.5 px-5 text-xs rounded-full shadow-none transition disabled:opacity-40"
              >
                {mode === 'edit' ? 'Сохранить изменения' : 'Опубликовать'}
              </Button>
            </div>
          </form>
        </div>
      </div>
  );
}

function HintMark({ text }: { text: string }) {
  return (
    <span className="relative inline-flex items-center group align-middle ml-0.5">
      <span
        tabIndex={0}
        className="text-[#5C4B7A] font-semibold cursor-help select-none outline-none"
        aria-label="Подсказка"
      >
        *
      </span>
      <span className="pointer-events-none absolute left-0 top-full mt-1.5 z-40 w-[min(14rem,calc(100vw-2rem))] p-2.5 rounded-2xl bg-[#1C1824] text-[#FFFCFA] text-[11px] leading-normal opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 transition-opacity shadow-xl">
        {text}
      </span>
    </span>
  );
}

function FieldError({ text }: { text?: string }) {
  if (!text) return null;
  return (
    <div className="mt-1.5 inline-flex max-w-full px-3 py-1.5 rounded-full bg-[#F3D4D0] text-[#7A3A3A] text-[11px] leading-snug">
      {text}
    </div>
  );
}
