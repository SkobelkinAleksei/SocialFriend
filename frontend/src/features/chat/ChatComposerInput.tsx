import React, { useEffect, useLayoutEffect } from 'react';
import { isCompactViewport } from '@/shared/utils/navigation';

const MAX_COMPOSER_PX = 128;

function resizeComposer(el: HTMLTextAreaElement | null) {
    if (!el) return;
    el.style.height = 'auto';
    const next = Math.min(el.scrollHeight, MAX_COMPOSER_PX);
    el.style.height = `${Math.max(next, 40)}px`;
    el.style.overflowY = el.scrollHeight > MAX_COMPOSER_PX ? 'auto' : 'hidden';
}

export default function ChatComposerInput({
    inputRef,
    value,
    onChange,
    onSend,
    placeholder,
}: {
    inputRef: React.MutableRefObject<HTMLTextAreaElement | null>;
    value: string;
    onChange: (next: string) => void;
    onSend: () => void;
    placeholder: string;
}) {
    useLayoutEffect(() => {
        resizeComposer(inputRef.current);
    }, [value, inputRef]);

    useEffect(() => {
        const onResize = () => resizeComposer(inputRef.current);
        window.addEventListener('resize', onResize);
        window.visualViewport?.addEventListener('resize', onResize);
        return () => {
            window.removeEventListener('resize', onResize);
            window.visualViewport?.removeEventListener('resize', onResize);
        };
    }, [inputRef]);

    return (
        <textarea
            ref={inputRef}
            rows={1}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={(e) => {
                if (e.key !== 'Enter') return;
                if (e.nativeEvent.isComposing) return;
                if (isCompactViewport()) return;
                if (e.shiftKey) return;
                e.preventDefault();
                onSend();
            }}
            placeholder={placeholder}
            enterKeyHint="enter"
            autoComplete="off"
            className="w-full max-h-32 px-4 py-2.5 text-[16px] md:text-sm leading-snug bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/30 focus:bg-white transition resize-none overflow-y-hidden whitespace-pre-wrap break-words [overflow-wrap:anywhere]"
        />
    );
}
