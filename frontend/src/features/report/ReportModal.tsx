import React, { createContext, useCallback, useContext, useState } from 'react';
import { createPortal } from 'react-dom';
import { AlertCircle, X } from 'lucide-react';
import api from '@/shared/lib/api';
import Button from '@/shared/ui/Button';
import { useAppBackHandler } from '@/shared/hooks/useAppBackHandler';
import { showAppInfoToast } from '@/shared/utils/appToast';
import { useAuth } from '@/shared/context/AuthContext';

export type ReportCategory = 'POST' | 'COMMENT' | 'PHOTO' | 'EVENT' | 'MESSAGE' | 'CHAT';
export type ReportReason = 'SPAM' | 'ABUSE' | 'FAKE' | 'OTHER';

export type ReportTarget = {
  category: ReportCategory;
  accusedId: number;
  accusedName?: string;
  targetId: number;
  roomId?: number | null;
  targetTitle?: string;
  snapshotText?: string;
};

const REASONS: { id: ReportReason; title: string; hint: string }[] = [
  { id: 'SPAM', title: 'Спам', hint: 'Реклама, рассылка, не про район' },
  { id: 'ABUSE', title: 'Оскорбления', hint: 'Грубость, травля, угрозы' },
  { id: 'FAKE', title: 'Фейк', hint: 'Обман, чужие фото, ложный профиль' },
  { id: 'OTHER', title: 'Другое', hint: 'Коротко напишите, в чём дело' },
];

const KIND_LABEL: Record<ReportCategory, string> = {
  POST: 'пост',
  COMMENT: 'комментарий',
  PHOTO: 'фото',
  EVENT: 'событие',
  MESSAGE: 'сообщение',
  CHAT: 'чат',
};

function isMediaSnapshot(text?: string) {
  const value = String(text || '').trim();
  if (!value) return false;
  return value.includes('/media/')
    || value.startsWith('/api/')
    || /^https?:\/\//i.test(value);
}

function neighborSnapshot(text?: string) {
  if (!text || isMediaSnapshot(text)) return '';
  return text
    .replace(/\[SHARE_EVENT:\d+\]\s*/g, '')
    .replace(/\[SHARE_POST:\d+\]\s*/g, '')
    .trim();
}

type ReportContextValue = {
  openReport: (target: ReportTarget) => void;
};

const ReportContext = createContext<ReportContextValue | null>(null);

export function useReport() {
  const ctx = useContext(ReportContext);
  if (!ctx) {
    return {
      openReport: () => {
        showAppInfoToast('Жалоба', 'Не удалось открыть окно');
      },
    };
  }
  return ctx;
}

export function ReportProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [target, setTarget] = useState<ReportTarget | null>(null);
  const openReport = useCallback((next: ReportTarget) => {
    if (!next?.accusedId || !next?.targetId) return;
    if (user?.id && Number(next.accusedId) === Number(user.id)) return;
    setTarget(next);
  }, [user?.id]);
  return (
    <ReportContext.Provider value={{ openReport }}>
      {children}
      {target && <ReportModal target={target} onClose={() => setTarget(null)} />}
    </ReportContext.Provider>
  );
}

function ReportModal({ target, onClose }: { target: ReportTarget; onClose: () => void }) {
  const [reason, setReason] = useState<ReportReason | null>(null);
  const [details, setDetails] = useState('');
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);

  useAppBackHandler(true, onClose);

  const canSend = reason != null && (reason !== 'OTHER' || details.trim().length > 0) && !busy;

  const submit = async () => {
    if (!reason || !canSend) return;
    setBusy(true);
    try {
      await api.post('/api/v1/social/reports', {
        category: target.category,
        reason,
        accusedId: target.accusedId,
        targetId: target.targetId,
        roomId: target.roomId || undefined,
        targetTitle: target.targetTitle || undefined,
        snapshotText: target.snapshotText || undefined,
        details: reason === 'OTHER' ? details.trim() : undefined,
      });
      setSent(true);
    } catch (error: any) {
      showAppInfoToast('Жалоба', error?.response?.data?.detail || error?.response?.data?.details || 'Не удалось отправить');
    } finally {
      setBusy(false);
    }
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[110] bg-[#1A1916]/45 backdrop-blur-[2px] flex items-end md:items-center justify-center px-0 md:px-4"
      onClick={onClose}
    >
      <div
        className="w-full md:max-w-[440px] bg-[#FFFCFA] rounded-t-[24px] md:rounded-[28px] shadow-[0_20px_44px_-28px_rgba(92,75,122,0.45)] border border-[#1C1824]/10 p-5 pb-[max(1.25rem,env(safe-area-inset-bottom))] animate-in slide-in-from-bottom-4 md:zoom-in-95 fade-in duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3 mb-3">
          <div>
            <div className="myraion-display text-[22px] text-[#1C1824] leading-tight">Жалоба</div>
            <p className="text-[12px] text-[#8A8494] mt-1">
              {sent ? 'Жалоба успешно отправлена' : `На ${KIND_LABEL[target.category]}`}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-9 h-9 rounded-full text-[#8A8494] hover:bg-[#EDE6F5] flex items-center justify-center shrink-0"
            aria-label="Закрыть"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {sent ? (
          <div className="space-y-4">
            <Button className="w-full" onClick={onClose}>Понятно</Button>
          </div>
        ) : (
          <>
            {(target.accusedName || neighborSnapshot(target.snapshotText)) && (
              <div className="rounded-2xl bg-[#EFEAF6] px-3.5 py-2.5 mb-3">
                {target.accusedName && (
                  <div className="text-[12px] font-semibold text-[#5C4B7A]">{target.accusedName}</div>
                )}
                {neighborSnapshot(target.snapshotText) && (
                  <div className="text-[12px] text-[#1C1824]/80 mt-0.5 line-clamp-3 whitespace-pre-wrap">
                    {neighborSnapshot(target.snapshotText)}
                  </div>
                )}
              </div>
            )}
            <div className="space-y-1.5 mb-3">
              {REASONS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setReason(item.id)}
                  className={`w-full text-left rounded-2xl border px-3.5 py-2.5 transition ${
                    reason === item.id
                      ? 'border-[#5C4B7A] bg-[#EDE6F5]'
                      : 'border-[#1C1824]/10 bg-white hover:bg-[#EFEAF6]'
                  }`}
                >
                  <div className="text-sm font-semibold text-[#1C1824]">{item.title}</div>
                  <div className="text-[11px] text-[#8A8494] mt-0.5">{item.hint}</div>
                </button>
              ))}
            </div>
            {reason === 'OTHER' && (
              <textarea
                value={details}
                onChange={(e) => setDetails(e.target.value.slice(0, 500))}
                rows={3}
                placeholder="Коротко, в чём дело"
                className="w-full mb-3 p-3 text-sm bg-white border border-[#1C1824]/10 rounded-2xl resize-none focus:outline-none focus:border-[#5C4B7A]/40 text-[#1C1824]"
              />
            )}
            <div className="flex gap-2">
              <Button variant="secondary" className="flex-1" onClick={onClose}>Отмена</Button>
              <Button className="flex-1" disabled={!canSend} onClick={submit}>
                {busy ? 'Отправляем…' : 'Отправить'}
              </Button>
            </div>
          </>
        )}
      </div>
    </div>,
    document.body
  );
}

export function ReportIconButton({
  className,
  iconClassName = 'w-4 h-4',
  onClick,
  title = 'Жалоба',
}: {
  className?: string;
  iconClassName?: string;
  onClick: (e: React.MouseEvent<HTMLButtonElement>) => void;
  title?: string;
}) {
  return (
    <button
      type="button"
      title={title}
      aria-label={title}
      onClick={onClick}
      className={className}
    >
      <AlertCircle className={iconClassName} />
    </button>
  );
}
