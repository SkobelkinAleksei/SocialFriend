import React, { useState } from 'react';
import api, { uploadPostPhoto } from '@/shared/lib/api';
import { useAuth } from '@/shared/context/AuthContext';
import Card from '@/shared/ui/Card';
import Button from '@/shared/ui/Button';
import { getAvatarUrl } from '@/shared/utils/navigation';
import { ImageIcon, X } from 'lucide-react';
import { useChatPhotoAttach } from '@/features/feed/useChatPhotoAttach';
import { showAppInfoToast } from '@/shared/utils/appToast';

interface MyPostSectionProps {
    onPostCreated: () => void;
}

const POST_MIN = 5;
const POST_MAX = 3000;

export default function MyPostSection({ onPostCreated }: MyPostSectionProps) {
    const { user } = useAuth();
    const [newPostContent, setNewPostContent] = useState('');
    const [commentsAllowed, setCommentsAllowed] = useState(true);
    const photos = useChatPhotoAttach(10, uploadPostPhoto);
    const postLength = newPostContent.length;
    const canPublish = newPostContent.trim().length >= POST_MIN && (photos.count === 0 || photos.allReady);

    const handleCreatePost = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!user?.id || !canPublish) {
            if (newPostContent.trim().length > 0 && newPostContent.trim().length < POST_MIN) {
                showAppInfoToast('Пост', `Напишите хотя бы ${POST_MIN} символов`);
            }
            return;
        }
        try {
            await api.post('/api/v1/social/posts', {
                content: newPostContent,
                commentsAllowed: commentsAllowed,
                photos: photos.storedUrls,
            });
            setNewPostContent('');
            photos.clear();
            onPostCreated();
        } catch (error: any) {
            console.error("Ошибка при создании поста:", error);
            const detail = error?.response?.data?.detail
                || error?.response?.data?.fieldErrors?.[0]?.message
                || 'Не удалось опубликовать пост';
            showAppInfoToast('Пост', String(detail).includes('CONTENT') || String(detail).includes('символ')
                ? `Напишите хотя бы ${POST_MIN} символов`
                : detail);
        }
    };

    return (
        <div className="mt-5 md:mt-6 lg:mt-8 mb-4">
            <Card className="p-4 md:p-[18px] lg:p-5">
                <form onSubmit={handleCreatePost} className="space-y-3">
                    <div className="flex gap-3 items-start">
                        <img src={getAvatarUrl(user?.id, user?.avatarUrl)} alt="" className="w-10 h-10 rounded-full object-cover border border-slate-100" />
                        <div className="relative flex-1 min-w-0">
                        <textarea
                            value={newPostContent}
                            onChange={(e) => setNewPostContent(e.target.value.slice(0, POST_MAX))}
                            placeholder="Написать пост на районе..."
                            maxLength={POST_MAX}
                            className="w-full min-h-[80px] p-3 pb-7 bg-[#EFEAF6] border border-[#1C1824]/10 rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/20 focus:border-[#5C4B7A]/40 transition resize-none placeholder:text-[#8A8494] text-[#1C1824]"
                        />
                        {newPostContent.trim().length > 0 && newPostContent.trim().length < POST_MIN && (
                            <span className="pointer-events-none absolute bottom-2.5 left-3 text-[11px] text-[#8A8494]">
                                Минимум {POST_MIN} символов
                            </span>
                        )}
                        <span className="pointer-events-none absolute bottom-2.5 right-3 text-[11px] text-slate-400 tabular-nums">
                            {postLength}/{POST_MAX}
                        </span>
                        </div>
                    </div>
                    {photos.count > 0 && (
                        <div className="flex gap-2 overflow-x-auto pl-[52px]">
                            {photos.pending.map((item) => (
                                <div key={item.localId} className="relative w-16 h-16 shrink-0 rounded-xl overflow-hidden bg-[#EDE6F5]">
                                    <img src={item.url || item.preview} alt="" className="w-full h-full object-cover" />
                                    {item.uploading && <div className="absolute inset-0 bg-white/50" />}
                                    <button
                                        type="button"
                                        onClick={() => photos.remove(item.localId)}
                                        className="absolute top-1 right-1 w-5 h-5 rounded-full bg-[#1A1916]/70 text-white flex items-center justify-center"
                                    >
                                        <X className="w-3 h-3" />
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                    <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-50">
                        <div className="flex items-center gap-2 md:gap-3 text-slate-400 min-w-0">
                            <input
                                ref={photos.inputRef}
                                type="file"
                                accept="image/jpeg,image/png,image/webp,image/gif"
                                multiple
                                className="hidden"
                                onChange={(e) => {
                                    photos.addFiles(e.target.files);
                                    e.target.value = '';
                                }}
                            />
                            <button
                                type="button"
                                onClick={photos.openPicker}
                                className="hover:text-[#5C4B7A] transition"
                                title="Прикрепить фото"
                            >
                                <ImageIcon className="w-5 h-5" />
                            </button>
                            <label className="flex items-center gap-1.5 text-[11px] md:text-xs text-slate-500 cursor-pointer ml-1 md:ml-2 select-none">
                                <input
                                    type="checkbox"
                                    checked={commentsAllowed}
                                    onChange={(e) => setCommentsAllowed(e.target.checked)}
                                    className="rounded text-[#5C4B7A] focus:ring-[#5C4B7A]/20 border-slate-300"
                                /> Разрешить комментарии
                            </label>
                        </div>
                        <Button
                            type="submit"
                            disabled={!canPublish}
                            className="bg-[#5C4B7A] hover:bg-[#4A3C66] text-white py-2 px-4 md:px-5 text-xs rounded-full transition disabled:opacity-50 shrink-0"
                        >
                            Опубликовать
                        </Button>
                    </div>
                </form>
            </Card>
        </div>
    );
}
