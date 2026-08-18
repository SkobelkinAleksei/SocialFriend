import React from 'react';
import { openNeighborProfile } from '@/shared/utils/navigation';
import { renderMentionParts } from '@/features/chat/chatMentions';

export default function ChatMessageText({ text, className, mine }: { text?: string | null; className?: string; mine?: boolean }) {
  const parts = renderMentionParts(text);
  return (
    <div className={`min-w-0 w-full max-w-full break-words [overflow-wrap:anywhere] [word-break:normal] ${className || ''}`}>
      {parts.map((part, index) => (
        part.type === 'mention' && part.userId ? (
          <button
            key={`${part.userId}-${index}`}
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              openNeighborProfile(part.userId!, part.value.replace(/^@/, ''));
            }}
            className={`font-semibold hover:underline break-words [overflow-wrap:anywhere] ${mine ? 'text-[#EDE6F5]' : 'text-[#5C4B7A]'}`}
          >
            {part.value}
          </button>
        ) : (
          <span key={index}>{part.value}</span>
        )
      ))}
    </div>
  );
}
