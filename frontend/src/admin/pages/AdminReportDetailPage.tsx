import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Card from '@/shared/ui/Card';
import Button from '@/shared/ui/Button';
import { resolvePhotoUrl } from '@/shared/utils/photoGallery';
import { adminApi, adminErrorMessage, categoryLabel, reasonLabel, statusLabel, type AdminReport } from '../adminApi';

export default function AdminReportDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const reportId = Number(id);
    const [report, setReport] = useState<AdminReport | null>(null);
    const [preview, setPreview] = useState<any>(null);
    const [busy, setBusy] = useState('');
    const [error, setError] = useState('');

    const load = async () => {
        const res = await adminApi.report(reportId);
        setReport(res.data);
        try {
            if (res.data.category === 'POST') {
                setPreview({ kind: 'POST', data: (await adminApi.previewPost(res.data.targetId)).data });
            } else if (res.data.category === 'EVENT') {
                setPreview({ kind: 'EVENT', data: (await adminApi.previewEvent(res.data.targetId)).data });
            } else if (res.data.category === 'COMMENT') {
                setPreview({ kind: 'COMMENT', data: (await adminApi.previewComment(res.data.targetId)).data });
            } else if (res.data.category === 'MESSAGE') {
                setPreview({ kind: 'MESSAGE', data: (await adminApi.previewChat(res.data.targetId)).data });
            } else {
                setPreview({ kind: res.data.category, snapshot: true });
            }
        } catch {
            setPreview({ kind: res.data.category, missing: true });
        }
    };

    useEffect(() => {
        if (!reportId) return;
        load().catch(() => setError('Не удалось открыть жалобу'));
    }, [reportId]);

    const act = async (kind: 'dismiss' | 'delete' | 'ban') => {
        if (kind === 'delete' && !window.confirm('Удалить этот контент у соседей?')) return;
        if (kind === 'ban' && !window.confirm('Забанить автора на районе?')) return;
        setBusy(kind);
        setError('');
        try {
            if (kind === 'dismiss') await adminApi.dismiss(reportId);
            if (kind === 'delete') await adminApi.deleteTarget(reportId);
            if (kind === 'ban') await adminApi.banFromReport(reportId);
            navigate('/moderation');
        } catch (e: unknown) {
            setError(adminErrorMessage(e, 'Не удалось выполнить действие'));
        } finally {
            setBusy('');
        }
    };

    if (!report) {
        return <div className="p-8 text-[#8A8494]">{error || 'Загружаем жалобу…'}</div>;
    }

    const secondStrike = report.accusedReportsUpheld >= 1 && report.accusedStatus !== 'BANNED';
    const deleteLabel = report.category === 'POST'
        ? 'Удалить пост'
        : report.category === 'EVENT'
            ? 'Удалить событие'
            : report.category === 'COMMENT'
                ? 'Удалить комментарий'
                : report.category === 'PHOTO'
                    ? 'Удалить фото'
                    : report.category === 'CHAT'
                        ? 'Удалить чат'
                        : 'Удалить это сообщение';
    const canDeleteTarget = report.category !== 'CHAT';

    return (
        <div className="max-w-4xl mx-auto px-4 py-8 space-y-5">
            <button type="button" className="text-sm text-[#5C4B7A] font-semibold" onClick={() => navigate('/moderation')}>
                К списку
            </button>
            <div className="flex flex-wrap items-center gap-2">
                <h1 className="myraion-display text-3xl text-[#1C1824]">Разбор</h1>
                <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#EDE6F5] text-[#5C4B7A]">{categoryLabel(report.category)}</span>
                <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#FFFCFA] border border-[#1C1824]/10">{reasonLabel(report.reason)}</span>
            </div>
            {report.details && (
                <Card>
                    <div className="text-xs uppercase tracking-wide text-[#8A8494] mb-2">Описание</div>
                    <p className="text-sm whitespace-pre-wrap">{report.details}</p>
                </Card>
            )}
            {secondStrike && (
                <Card className="border border-amber-200 bg-amber-50">
                    <div className="font-semibold text-amber-800">Повторная верная жалоба</div>
                    <p className="text-sm text-amber-900/80 mt-1">На этого человека уже есть удовлетворённая жалоба. Удаление контента без бана больше не предлагается.</p>
                </Card>
            )}
            {preview?.kind === 'MESSAGE' && preview.data?.messages && (
                <Card>
                    <div className="font-semibold mb-3">{preview.data.title} · сразу к сообщению {preview.data.highlightedId}</div>
                    <div className="space-y-2">
                        {preview.data.messages.map((line: any) => (
                            <div
                                key={line.id}
                                className={`rounded-2xl px-3 py-2 text-sm ${line.highlighted ? 'bg-[#EDE6F5] border border-[#5C4B7A]/30' : 'bg-[#F7F4FA]'}`}
                            >
                                <div className="text-[11px] text-[#8A8494] mb-1">
                                    {line.authorName} · id {line.authorId}
                                    {line.highlighted ? ' · жалоба сюда' : ''}
                                </div>
                                {line.deleted ? <span className="italic text-[#8A8494]">удалено</span> : line.text}
                            </div>
                        ))}
                    </div>
                </Card>
            )}
            {preview?.kind === 'POST' && preview.data && (
                <Card>
                    <div className="font-semibold mb-2">Пост · id {preview.data.id}</div>
                    <p className="text-sm whitespace-pre-wrap">{preview.data.content}</p>
                    {Array.isArray(preview.data.photos) && preview.data.photos.length > 0 && (
                        <div className="mt-3 grid grid-cols-2 gap-2">
                            {preview.data.photos.map((photo: string) => (
                                <img
                                    key={photo}
                                    src={resolvePhotoUrl(photo)}
                                    alt=""
                                    className="w-full h-40 object-cover rounded-2xl"
                                />
                            ))}
                        </div>
                    )}
                </Card>
            )}
            {preview?.kind === 'EVENT' && preview.data && (
                <Card>
                    <div className="font-semibold mb-2">Событие · id {preview.data.id}</div>
                    <p className="text-lg">{preview.data.title}</p>
                    <p className="text-sm text-[#8A8494] mt-1">{preview.data.description}</p>
                    {preview.data.isPrivate ? <p className="text-xs mt-2 text-[#5C4B7A]">Закрытая встреча — вам всё равно видно.</p> : null}
                    {Array.isArray(preview.data.photos) && preview.data.photos.length > 0 && (
                        <div className="mt-3 grid grid-cols-2 gap-2">
                            {preview.data.photos.map((photo: string) => (
                                <img
                                    key={photo}
                                    src={resolvePhotoUrl(photo)}
                                    alt=""
                                    className="w-full h-40 object-cover rounded-2xl"
                                />
                            ))}
                        </div>
                    )}
                </Card>
            )}
            {preview?.kind === 'COMMENT' && preview.data && (
                <Card>
                    <div className="font-semibold mb-2">Комментарий · id {preview.data.id}</div>
                    <p className="text-sm whitespace-pre-wrap">{preview.data.content}</p>
                    {preview.data.status === 'REMOVED' && <p className="text-xs mt-2 text-[#8A8494]">Уже снят</p>}
                </Card>
            )}
            {(preview?.kind === 'PHOTO' || preview?.kind === 'CHAT' || preview?.snapshot) && (
                <Card>
                    <div className="font-semibold mb-2">{categoryLabel(report.category)}</div>
                    {report.category === 'PHOTO' && report.snapshotText && !report.snapshotText.includes('\n') ? (
                        <img src={resolvePhotoUrl(report.snapshotText)} alt="" className="w-full max-h-80 object-contain rounded-2xl bg-[#F7F4FA]" />
                    ) : (
                        <p className="text-sm whitespace-pre-wrap">{report.snapshotText || 'Снимок не сохранился'}</p>
                    )}
                </Card>
            )}
            {preview?.missing && (
                <Card>
                    <p className="text-sm text-[#8A8494]">Цель жалобы уже недоступна. Снимок: {report.snapshotText || 'нет'}</p>
                </Card>
            )}
            <div className="grid md:grid-cols-2 gap-4">
                <Card>
                    <div className="text-xs uppercase tracking-wide text-[#8A8494] mb-2">Отправитель</div>
                    <p>{report.reporterName} · id {report.reporterId}</p>
                    <div className="text-xs uppercase tracking-wide text-[#8A8494] mt-4 mb-2">На кого</div>
                    <p className="font-semibold">{report.accusedName} · id {report.accusedId}</p>
                    <p className="text-sm text-[#8A8494]">{statusLabel(report.accusedStatus)}</p>
                    <p className="text-sm mt-2">Жалоб: {report.accusedReportsTotal} · удовлетворено: {report.accusedReportsUpheld}</p>
                    <Button variant="secondary" size="sm" className="mt-3" onClick={() => navigate(`/moderation/people/${report.accusedId}`)}>
                        Карточка
                    </Button>
                </Card>
                <Card>
                    <div className="text-xs uppercase tracking-wide text-[#8A8494] mb-3">Решение</div>
                    <div className="flex flex-wrap gap-2">
                        <Button variant="secondary" disabled={!!busy} onClick={() => act('dismiss')}>Отклонить</Button>
                        <Button variant="secondary" disabled={!!busy || secondStrike || !canDeleteTarget} onClick={() => act('delete')}>{deleteLabel}</Button>
                        <Button disabled={!!busy} onClick={() => act('ban')}>Забанить автора</Button>
                    </div>
                    {error && <p className="text-sm text-rose-600 mt-3">{error}</p>}
                </Card>
            </div>
        </div>
    );
}
