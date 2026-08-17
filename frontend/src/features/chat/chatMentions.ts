export function insertMention(draft: string, caret: number, userId: number, name: string): { text: string; caret: number } {
  const before = draft.slice(0, caret);
  const after = draft.slice(caret);
  const at = before.lastIndexOf('@');
  const prefix = at >= 0 ? before.slice(0, at) : before;
  const token = `@${name}[MENTION:${userId}] `;
  const text = `${prefix}${token}${after}`;
  return { text, caret: (prefix + token).length };
}

export function mentionQuery(draft: string, caret: number): string | null {
  const before = draft.slice(0, caret);
  const at = before.lastIndexOf('@');
  if (at < 0) return null;
  const chunk = before.slice(at + 1);
  if (chunk.includes(' ') || chunk.includes('[')) return null;
  return chunk;
}

export function stripMentionMarkers(text?: string | null): string {
  return String(text || '').replace(/\[MENTION:\d+\]/g, '').trim();
}

export function renderMentionParts(text?: string | null): Array<{ type: 'text' | 'mention'; value: string; userId?: number }> {
  const raw = String(text || '');
  const parts: Array<{ type: 'text' | 'mention'; value: string; userId?: number }> = [];
  const re = /@([^[\]]+)\[MENTION:(\d+)\]/g;
  let last = 0;
  let match: RegExpExecArray | null;
  while ((match = re.exec(raw))) {
    if (match.index > last) {
      parts.push({ type: 'text', value: raw.slice(last, match.index) });
    }
    parts.push({ type: 'mention', value: `@${match[1]}`, userId: Number(match[2]) });
    last = match.index + match[0].length;
  }
  if (last < raw.length) parts.push({ type: 'text', value: raw.slice(last) });
  return parts.length ? parts : [{ type: 'text', value: raw }];
}
