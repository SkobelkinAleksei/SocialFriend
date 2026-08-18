import { useEffect, useRef } from 'react';
import type { useChatMediaAttach } from '@/features/chat/useChatMediaAttach';

function filesFromClipboard(event: ClipboardEvent): File[] {
  const out: File[] = [];
  const seen = new Set<string>();
  const add = (file: File | null) => {
    if (!file) return;
    const image = file.type.startsWith('image/') || /\.(png|jpe?g|gif|webp)$/i.test(file.name);
    if (!image) return;
    const key = `${file.name}:${file.size}:${file.type}:${file.lastModified}`;
    if (seen.has(key)) return;
    seen.add(key);
    out.push(file);
  };
  const data = event.clipboardData;
  if (!data) return out;
  if (data.items) {
    for (const item of Array.from(data.items)) {
      if (item.kind === 'file') add(item.getAsFile());
    }
  }
  if (data.files) {
    for (const file of Array.from(data.files)) add(file);
  }
  return out;
}

export function useChatPasteImages(
  addPhotos: ReturnType<typeof useChatMediaAttach>['addPhotos'],
  enabled: boolean,
) {
  const addRef = useRef(addPhotos);
  addRef.current = addPhotos;

  useEffect(() => {
    if (!enabled) return;
    const onPaste = (event: ClipboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (target?.closest?.('input:not([data-chat-composer]), textarea:not([data-chat-composer]), [contenteditable="true"]')) {
        return;
      }
      const files = filesFromClipboard(event);
      if (!files.length) return;
      event.preventDefault();
      void addRef.current(files);
    };
    window.addEventListener('paste', onPaste);
    return () => window.removeEventListener('paste', onPaste);
  }, [enabled]);
}
