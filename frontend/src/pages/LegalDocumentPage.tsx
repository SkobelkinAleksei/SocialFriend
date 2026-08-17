import React from 'react';
import { ArrowLeft } from 'lucide-react';
import { LEGAL_DOCS, type LegalDocId } from '@/shared/legal/legalDocuments';

interface LegalDocumentPageProps {
  doc: LegalDocId;
  onBack: () => void;
  onOpenDoc?: (id: LegalDocId) => void;
}

export default function LegalDocumentPage({ doc, onBack, onOpenDoc }: LegalDocumentPageProps) {
  const data = LEGAL_DOCS[doc];
  const other: LegalDocId = doc === 'privacy' ? 'rules' : 'privacy';

  return (
    <div className="min-h-full bg-[#EFEAF6] text-[#1C1824]">
      <div className="max-w-2xl mx-auto px-4 py-5 sm:py-8 pb-[max(2rem,env(safe-area-inset-bottom))]">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-1.5 text-sm text-[#5C4B7A] hover:text-[#4A3C66] mb-5 min-h-11 -ml-1 px-1"
        >
          <ArrowLeft className="w-4 h-4" />
          Назад
        </button>
        <h1 className="myraion-display text-[28px] sm:text-[32px] leading-tight">{data.title}</h1>
        <p className="text-xs text-[#8A8494] mt-2">Редакция от {data.updated}</p>
        <p className="text-sm leading-relaxed text-[#1C1824] mt-4">{data.intro}</p>
        {onOpenDoc && (
          <p className="text-sm text-[#8A8494] mt-3">
            См. также:{' '}
            <button
              type="button"
              onClick={() => onOpenDoc(other)}
              className="text-[#5C4B7A] font-semibold underline underline-offset-2 bg-transparent border-none p-0 cursor-pointer"
            >
              {LEGAL_DOCS[other].title}
            </button>
          </p>
        )}
        <div className="mt-6 space-y-6">
          {data.sections.map((section) => (
            <section key={section.heading}>
              <h2 className="text-[15px] font-bold text-[#1C1824] mb-2">{section.heading}</h2>
              {section.paragraphs.map((p, i) => (
                <p key={`${section.heading}-${i}`} className="text-sm leading-relaxed text-[#1C1824]/90 mb-2 last:mb-0">
                  {p}
                </p>
              ))}
            </section>
          ))}
        </div>
        <p className="text-[11px] text-[#8A8494] mt-10">
          «На районе» · {data.title} · {data.updated}
        </p>
      </div>
    </div>
  );
}
