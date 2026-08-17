import React from 'react';
import { theme } from '@/shared/ui/theme';

type Tone = 'accent' | 'success' | 'neutral' | 'warning';

interface BadgeProps {
  children: React.ReactNode;
  tone?: Tone;
  className?: string;
}

export default function Badge({ children, tone = 'accent', className = '' }: BadgeProps) {
  const tones: Record<Tone, string> = {
    accent: `${theme.accent.bgSoft} ${theme.accent.textStrong}`,
    success: 'bg-[#D4E8DC] text-[#3D6B56]',
    neutral: 'bg-[#EDE6F5] text-[#5C4B7A]',
    warning: 'bg-[#F3D4D0] text-[#7A3A3A]',
  };
  return (
    <span
      className={`inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium ${theme.radius.pill} ${tones[tone]} ${className}`}
    >
      {children}
    </span>
  );
}