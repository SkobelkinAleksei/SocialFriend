import React from 'react';

interface SectionProps {
  children: React.ReactNode;
  title?: string;
  subtitle?: string;
  action?: React.ReactNode;
  className?: string;
}

export default function Section({ children, title, subtitle, action, className = '' }: SectionProps) {
  return (
    <section className={`mb-8 ${className}`}>
      {(title || action) && (
        <div className="flex items-end justify-between mb-4 gap-4">
          <div>
            {title && <h2 className="myraion-display text-xl md:text-2xl text-[#1C1824] tracking-tight">{title}</h2>}
            {subtitle && <p className="text-sm text-[#8A8494] mt-1">{subtitle}</p>}
          </div>
          {action && <div className="shrink-0">{action}</div>}
        </div>
      )}
      {children}
    </section>
  );
}