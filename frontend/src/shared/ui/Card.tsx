import React from 'react';
import { theme } from '@/shared/ui/theme';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  hover?: boolean;
  padded?: boolean;
}

export default function Card({ children, className = '', hover = false, padded = true, ...rest }: CardProps) {
  return (
    <div
      className={`${theme.surface.card} ${theme.radius.card} shadow-[0_20px_44px_-28px_rgba(92,75,122,0.45)] ${
        hover ? 'hover:shadow-md transition' : ''
      } ${padded ? 'p-5' : ''} ${className}`}
      {...rest}
    >
      {children}
    </div>
  );
}