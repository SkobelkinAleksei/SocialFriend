import React from 'react';
import { FileText, Download } from 'lucide-react';
import { resolveChatMediaUrl } from '@/features/chat/chatPhotos';

export type ChatFileItem = {
  url?: string;
  name?: string;
  size?: number;
  mimeType?: string;
};

export function formatFileSize(bytes?: number | null): string {
  const value = Number(bytes) || 0;
  if (value < 1024) return `${value} Б`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(value < 10 * 1024 ? 1 : 0)} КБ`;
  return `${(value / (1024 * 1024)).toFixed(value < 10 * 1024 * 1024 ? 1 : 0)} МБ`;
}

export default function ChatFileBubble({
  files,
  mine,
}: {
  files?: ChatFileItem[] | null;
  mine?: boolean;
}) {
  const items = (files || []).filter((item) => item?.url);
  if (!items.length) return null;

  return (
    <div className="flex flex-col gap-1.5 mb-1.5 max-w-[260px]">
      {items.map((file, index) => {
        const href = resolveChatMediaUrl(file.url);
        return (
          <a
            key={`${file.url}-${index}`}
            href={href}
            download={file.name || true}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()}
            className={`flex items-center gap-2.5 rounded-xl px-2.5 py-2 border ${mine ? 'bg-white/10 border-white/15 hover:bg-white/15' : 'bg-white border-[#1A1916]/10 hover:bg-[#F7F1EA]'}`}
          >
            <div className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${mine ? 'bg-white/20 text-white' : 'bg-[#EDE6F5] text-[#5C4B7A]'}`}>
              <FileText className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className={`text-[12px] font-semibold truncate ${mine ? 'text-white' : 'text-[#1A1916]'}`}>
                {file.name || 'Файл'}
              </div>
              <div className={`text-[10px] ${mine ? 'text-white/70' : 'text-slate-500'}`}>
                {formatFileSize(file.size)}
              </div>
            </div>
            <Download className={`w-4 h-4 shrink-0 ${mine ? 'text-white/80' : 'text-slate-400'}`} />
          </a>
        );
      })}
    </div>
  );
}
