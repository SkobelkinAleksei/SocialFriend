import React from 'react';
import ChatVoiceBubble from '@/features/chat/ChatVoiceBubble';
import ChatPhotoGrid from '@/features/chat/ChatPhotoGrid';
import ChatFileBubble from '@/features/chat/ChatFileBubble';
import SharedPostCard from '@/features/feed/SharedPostCard';
import SharedEventCard from '@/features/events/SharedEventCard';
import { formatSidebarMessage, isForwardQuoteCaption, mediaHintFromMessage, resolveMessageMedia } from '@/features/chat/ChatContext';
import { isShareEventText, isSharePostText } from '@/shared/utils/shareToChat';

function senderName(fw: any): string {
  const name = `${fw?.senderFirstName || ''} ${fw?.senderLastName || ''}`.trim();
  return name || (fw?.senderId ? `Пользователь #${fw.senderId}` : 'Участник');
}

function jumpToQuoted(id: any) {
  if (id == null || id === '') return;
  const targetId = String(id).replace(/\s+/g, '');
  const node = document.getElementById(`msg-container-${targetId}`) || document.querySelector(`[data-msg-id="${targetId}"]`);
  if (!node) return;
  node.scrollIntoView({ behavior: 'smooth', block: 'center' });
  const bubbleEl = node.querySelector('.message-bubble');
  if (bubbleEl) {
    bubbleEl.classList.add('brightness-90', 'scale-[0.99]');
    setTimeout(() => bubbleEl.classList.remove('brightness-90', 'scale-[0.99]'), 1000);
  }
}

export default function ChatForwardedPack({
  forwards,
  mine,
}: {
  forwards: any[] | null | undefined;
  mine?: boolean;
}) {
  if (!Array.isArray(forwards) || forwards.length === 0) return null;

  return (
    <div className="space-y-2 mb-1.5">
      {forwards.map((fw, idx) => {
        const media = resolveMessageMedia(fw);
        const raw = String(fw?.content || fw?.text || '').trim();
        if (isSharePostText(raw) || isShareEventText(raw)) {
          return (
            <div key={fw?.id || idx} className="space-y-1">
              <button
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  jumpToQuoted(fw?.id);
                }}
                className={`block text-[11px] font-semibold ${mine ? 'text-white/90' : 'text-[#5C4B7A]'}`}
              >
                Переслано от: {senderName(fw)}
              </button>
              {isSharePostText(raw) ? (
                <SharedPostCard text={raw} coverUrl={media.photos?.[0]} mine={mine} />
              ) : (
                <SharedEventCard text={raw} mine={mine} />
              )}
            </div>
          );
        }
        const caption = raw && !isForwardQuoteCaption(raw)
          ? formatSidebarMessage(raw, mediaHintFromMessage(fw))
          : '';
        return (
          <div
            key={fw?.id || idx}
            className="rounded-xl px-2.5 py-2 border-l-2"
            style={{
              backgroundColor: mine ? 'rgba(255,255,255,0.14)' : 'rgba(237,230,245,0.7)',
              borderColor: mine ? 'rgba(255,255,255,0.65)' : '#5C4B7A',
              color: mine ? 'rgba(255,255,255,0.95)' : '#1A1916',
            }}
          >
            <button
              type="button"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                jumpToQuoted(fw?.id);
              }}
              className="block w-full text-left"
            >
              <div className={`text-[11px] font-semibold ${mine ? 'text-white/90' : 'text-[#5C4B7A]'}`}>
                {senderName(fw)}
              </div>
              {caption ? (
                <div className={`text-[12px] mt-0.5 min-w-0 whitespace-pre-wrap break-words [overflow-wrap:anywhere] [word-break:normal] ${mine ? 'text-white/80' : 'text-slate-600'}`}>
                  {caption}
                </div>
              ) : null}
            </button>
            <div className="mt-1.5">
              <ChatVoiceBubble url={media.voiceUrl} duration={media.voiceDuration} mine={mine} />
              <ChatPhotoGrid photos={media.photos} addedAt={fw?.timestamp || fw?.createdAt} />
              <ChatFileBubble files={media.files} mine={mine} />
            </div>
          </div>
        );
      })}
    </div>
  );
}
