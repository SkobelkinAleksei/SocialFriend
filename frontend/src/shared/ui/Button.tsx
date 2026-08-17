import React from 'react';
import { theme } from '@/shared/ui/theme';

type Variant = 'primary' | 'secondary' | 'ghost' | 'soft';
type Size = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  children: React.ReactNode;
}

export default function Button({
  variant = 'primary',
  size = 'md',
  className = '',
  children,
  ...rest
}: ButtonProps) {
  const base = `inline-flex items-center justify-center gap-2 font-medium ${theme.radius.btn} transition focus:outline-none focus-visible:ring-2 ${theme.accent.ring} disabled:opacity-50 disabled:cursor-not-allowed`;
  const sizes: Record<Size, string> = {
    sm: 'text-sm px-3 py-1.5',
    md: 'text-sm px-4 py-2.5',
    lg: 'text-base px-6 py-3',
  };
  const variants: Record<Variant, string> = {
    primary: `${theme.accent.bg} ${theme.accent.bgHover} ${theme.text.onAccent} shadow-sm`,
    secondary: `bg-[#FFFCFA] ${theme.text.strong} hover:bg-[#EFEAF6]`,
    ghost: `${theme.text.body} hover:bg-[#EDE6F5]`,
    soft: `${theme.accent.bgSoft} ${theme.accent.textStrong} hover:bg-[#DDD4F0]`,
  };
  return (
    <button className={`${base} ${sizes[size]} ${variants[variant]} ${className}`} {...rest}>
      {children}
    </button>
  );
}