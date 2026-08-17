import React from 'react';
import { FileText } from 'lucide-react';
import { resolveChatPhotoUrl } from '@/features/chat/chatPhotos';
import { openNeighborProfile } from '@/shared/utils/navigation';

export default function SharedPostCard({
  text,
  coverUrl,
  mine,
}: {
  text: string;
  coverUrl?: string;
  mine?: boolean;
}) {
  const match = text.match(/\[SHARE_POST:(\d+)(?::(\d+))?\]/);
  const postId = match ? Number(match[1]) : null;
  const authorId = match?.[2] ? Number(match[2]) : null;
  const after = text.replace(/\[SHARE_POST:\d+(?::\d+)?\]\s*/, '');
  const parts = after.split('\n');
  const headline = (parts[0] || 'Пост').replace(/^Пост\s+/i, '');
  const userComment = parts.slice(1).join('\n').trim();
  const cover = coverUrl ? resolveChatPhotoUrl(coverUrl) : '';
  const nameMatch = headline.match(/^(.+?):\s*«/);
  const authorName = nameMatch?.[1]?.trim() || '';
  const snippet = nameMatch ? headline.slice(nameMatch[0].length - 1) : headline;

  return (
    <div className="space-y-2 my-1 select-none pointer-events-auto flex flex-col items-start w-full">
      <div className="bg-white border border-slate-200/80 rounded-2xl flex flex-col max-w-[260px] shadow-sm overflow-hidden">
        {cover ? (
          <img src={cover} alt="" className="w-full h-28 object-cover" />
        ) : null}
        <div className="p-3.5 flex flex-col gap-2.5">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[#8A76B0] to-[#5C4B7A] flex items-center justify-center text-white shrink-0 shadow-sm">
              <FileText className="w-4 h-4 stroke-[2.5]" />
            </div>
            <div className="min-w-0 flex-1">
              <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider block leading-none mb-1">
                Пост
              </span>
              <span className="text-[12px] font-semibold text-slate-800 line-clamp-2">
                {authorName ? (
                  <>
                    {authorId ? (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          openNeighborProfile(authorId, authorName);
                        }}
                        className="hover:text-[#5C4B7A] cursor-pointer focus:outline-none"
                        title="Открыть страницу автора"
                      >
                        {authorName}
                      </button>
                    ) : (
                      authorName
                    )}
                    {': '}
                    {snippet}
                  </>
                ) : (
                  headline
                )}
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              if (postId) {
                window.dispatchEvent(new CustomEvent('forceOpenNotification', { detail: { postId } }));
              }
            }}
            className="w-full py-2 px-3 rounded-xl bg-white border border-slate-200 hover:border-[#5C4B7A]/40 hover:bg-[#EDE6F5] text-[#5C4B7A] font-bold text-xs transition active:scale-95 cursor-pointer shadow-sm text-center"
          >
            Открыть пост →
          </button>
        </div>
      </div>
      {userComment ? (
        <div className={`text-xs font-medium mt-1 px-1 break-words max-w-sm ${mine ? 'text-white/90' : 'text-slate-800'}`}>
          {userComment}
        </div>
      ) : null}
    </div>
  );
}
