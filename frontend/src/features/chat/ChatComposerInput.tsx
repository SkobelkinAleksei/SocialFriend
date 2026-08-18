import React, { useEffect, useLayoutEffect } from 'react';
import { isCompactViewport } from '@/shared/utils/navigation';

const MIN_COMPOSER_PX = 40;
const MAX_COMPOSER_PX = 120;

function resizeComposer(el: HTMLTextAreaElement | null) {
    if (!el) return;
    el.style.height = 'auto';
    const next = Math.min(el.scrollHeight, MAX_COMPOSER_PX);
    el.style.height = `${Math.max(next, MIN_COMPOSER_PX)}px`;
    el.style.overflowY = el.scrollHeight > MAX_COMPOSER_PX ? 'auto' : 'hidden';
}

export default function ChatComposerInput({
    inputRef,
    value,
    onChange,
    onSend,
    placeholder,
    onFocus,
    onHeightChange,
}: {
    inputRef: React.MutableRefObject<HTMLTextAreaElement | null>;
    value: string;
    onChange: (next: string) => void;
    onSend: () => void;
    placeholder: string;
    onFocus?: () => void;
    onHeightChange?: () => void;
}) {
    useLayoutEffect(() => {
        resizeComposer(inputRef.current);
        onHeightChange?.();
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
            onFocus={() => onFocus?.()}
            data-chat-composer
            className="w-full h-10 max-h-[120px] px-3.5 py-2 text-[16px] md:text-[15px] leading-[1.25] bg-slate-100 border-0 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/25 focus:bg-white transition resize-none overflow-y-hidden whitespace-pre-wrap break-words [overflow-wrap:anywhere]"
        />
    );
}
