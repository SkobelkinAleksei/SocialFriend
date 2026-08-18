import React, { useEffect, useMemo, useState } from 'react';

type Category = { id: string; label: string; icon: string; emojis: string[] };

const CATEGORIES: Category[] = [
    {
        id: 'smileys',
        label: 'Эмоции',
        icon: '😄',
        emojis: ['😀','😃','😄','😁','😆','🥹','😅','😂','🤣','🥲','☺️','😊','😇','🙂','😉','😌','😍','🥰','😘','😗','😙','😚','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🫡','🤐','🤨','😐','😑','😶','🫥','😏','😒','🙄','😬','😮‍💨','🤥','😌','😔','😪','🤤','😴','😷','🤒','🤕','🤢','🤮','🥴','😵','🤯','🤠','🥳','😎','🤓','🧐','😕','🫤','😟','🙁','☹️','😮','😯','😲','😳','🥺','🥹','😦','😧','😨','😰','😥','😢','😭','😱','😖','😣','😞','😓','😩','😫','🥱','😤','😡','😠','🤬','😈','👿','💀','💩','🤡','👻','👽','🤖','🎃'],
    },
    {
        id: 'hands',
        label: 'Жесты',
        icon: '👍',
        emojis: ['👋','🤚','🖐️','✋','🖖','👌','🤌','🤏','✌️','🤞','🫰','🤟','🤘','🤙','👈','👉','👆','🖕','👇','☝️','👍','👎','✊','👊','🤛','🤜','👏','🙌','🫶','👐','🤲','🤝','🙏','✍️','💅','🤳','💪','🦾','🦵','🦶'],
    },
    {
        id: 'hearts',
        label: 'Сердца',
        icon: '❤️',
        emojis: ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❣️','💕','💞','💓','💗','💖','💘','💝','💟','❤️‍🔥','❤️‍🩹','💋','💌','💯','💥','💫','💦','🔥','✨','⭐','🌟','⚡'],
    },
    {
        id: 'people',
        label: 'Люди',
        icon: '🙋',
        emojis: ['👶','👧','🧒','👦','👩','🧑','👨','👩‍🦰','🧑‍🦰','👩‍🦱','🧑‍🦱','👩‍🦳','🧔','👵','🧓','👴','👲','👳‍♀️','👳‍♂️','🧕','👮','👷','💂','🕵️','👩‍⚕️','👩‍🌾','👩‍🍳','👩‍🎓','👩‍🎤','👩‍🏫','👩‍💻','👩‍🚀','👩‍🚒','🥷','🦸','🦹','🧙','🧚','🧛','🧜','🧝','🧞','🧟','💃','🕺','👯','🧖','🧘'],
    },
    {
        id: 'animals',
        label: 'Животные',
        icon: '🦊',
        emojis: ['🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐻‍❄️','🐨','🐯','🦁','🐮','🐷','🐸','🐵','🙈','🙉','🙊','🐒','🐔','🐧','🐦','🐤','🐣','🐥','🦆','🦅','🦉','🦇','🐺','🐗','🐴','🦄','🐝','🪱','🐛','🦋','🐌','🐞','🐜','🪰','🪲','🪳','🐢','🐍','🦎','🐙','🦑','🦐','🦞','🦀','🐡','🐠','🐟','🐬','🐳','🐋','🦈','🐊','🐅','🐆','🦓','🦍','🐘','🦣','🦛','🦏','🐪','🐫','🦒','🦘','🦬','🐃','🐂','🐄','🐎','🐖','🐏','🐑','🦙','🐐','🦌','🐕','🐩','🐈','🐈‍⬛','🪶','🐓','🦃','🦤','🦚','🦜','🦢','🦩','🕊️','🐇','🦝','🦨','🦡','🦫','🦦','🦥','🐁','🐀','🐿️','🦔'],
    },
    {
        id: 'food',
        label: 'Еда',
        icon: '🍕',
        emojis: ['🍏','🍎','🍐','🍊','🍋','🍌','🍉','🍇','🍓','🫐','🍈','🍒','🍑','🥭','🍍','🥥','🥝','🍅','🍆','🥑','🥦','🥬','🥒','🌶️','🫑','🌽','🥕','🫒','🧄','🧅','🥔','🍠','🥐','🥯','🍞','🥖','🥨','🧀','🥚','🍳','🧈','🥞','🧇','🥓','🥩','🍗','🍖','🌭','🍔','🍟','🍕','🫓','🥪','🥙','🧆','🌮','🌯','🫔','🥗','🥘','🫕','🍝','🍜','🍲','🍛','🍣','🍱','🥟','🦪','🍤','🍙','🍚','🍘','🍥','🥠','🥮','🍢','🍡','🍧','🍨','🍦','🥧','🧁','🍰','🎂','🍮','🍭','🍬','🍫','🍿','🍩','🍪','🌰','🥜','🍯','🥛','☕','🍵','🧃','🥤','🧋','🍺','🍻','🥂','🍷','🥃','🍸','🍹','🍾','🧊'],
    },
    {
        id: 'travel',
        label: 'Город',
        icon: '🏙️',
        emojis: ['🚗','🚕','🚙','🚌','🚎','🏎️','🚓','🚑','🚒','🚐','🛻','🚚','🚛','🚜','🛵','🏍️','🛺','🚲','🛴','🚁','🛸','✈️','🛩️','🚀','🛶','⛵','🚤','🛥️','🛳️','⛴️','🚢','🏠','🏡','🏘️','🏚️','🏗️','🏢','🏬','🏭','🏛️','⛪','🕌','🕍','🛕','🕋','⛲','⛺','🗼','🗽','🗿','🌁','🌃','🏙️','🌄','🌅','🌆','🌇','🌉','🎠','🎡','🎢','🚂','🚃','🚄','🚅','🚆','🚇','🚉','🚊','⛵','⛱️','🏖️','🏝️','🏜️','🌋','⛰️','🏔️','🗻','🏕️','🏟️','🏞️'],
    },
    {
        id: 'activity',
        label: 'Досуг',
        icon: '🎉',
                emojis: ['⚽','🏀','🏈','⚾','🥎','🎾','🏐','🏉','🥏','🎱','🪀','🏓','🏸','🏒','🏑','🥍','🏏','🪃','🥅','⛳','🪁','🏹','🎣','🤿','🥊','🥋','🎽','🛹','🛼','🛷','⛸️','🎿','⛷️','🏂','🪂','🏋️','🤸','🤺','⛹️','🤾','🏌️','🏇','🧘','🏄','🏊','🤽','🚣','🧗','🚴','🏆','🥇','🥈','🥉','🏅','🎖️','🎗️','🎫','🎟️','🎪','🤹','🎭','🩰','🎨','🎬','🎤','🎧','🎼','🎹','🥁','🪘','🎷','🎺','🪗','🎸','🪕','🎲','♟️','🎯','🎳','🎮','🕹️','🎰','🧩'],
    },
    {
        id: 'objects',
        label: 'Вещи',
        icon: '🎁',
        emojis: ['⌚','📱','💻','⌨️','🖥️','🖨️','🖱️','🕹️','📷','📸','📹','🎥','📞','☎️','📺','📻','🎙️','⏰','⌛','⏳','📡','💡','🔦','🕯️','🛢️','💸','💵','💴','💶','💷','💰','💳','💎','⚖️','🧰','🔨','⚒️','🛠️','⛏️','🔩','⚙️','🧱','🧲','🔫','💣','🧨','🪓','🔪','🗡️','🛡️','🚬','⚰️','🪦','⚱️','🏺','🔮','📿','🧿','💈','⚗️','🔭','🔬','🕳️','🩹','🩺','💊','💉','🩸','🧬','🦠','🧫','🧪','🌡️','🧹','🧺','🧻','🚽','🚰','🚿','🛁','🛀','🧼','🪥','🪒','🧽','🪣','🧴','🛎️','🔑','🗝️','🚪','🪑','🛋️','🛏️','🧸','🖼️','🛍️','🛒','🎁','🎈','🎏','🎀','🪄','🪅','🎊','🎉','🎎','🏮','🎐','🧧','✉️','📦','📫','📬','📭','📮','📯','📜','📃','📄','📑','🧾','📊','📈','📉','🗒️','🗓️','📆','📅','🗑️','📇','🗃️','🗳️','🗄️','📋','📁','📂','🗂️','📰','📓','📔','📒','📕','📗','📘','📙','📚','📖','🔖','🧷','🔗','📎','🖇️','📐','📏','🧮','📌','📍','✂️','🖊️','🖋️','✒️','🖌️','🖍️','📝','✏️','🔍','🔎','🔏','🔐','🔒','🔓'],
    },
    {
        id: 'symbols',
        label: 'Символы',
        icon: '✨',
        emojis: ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','💔','💕','💖','💗','💘','💝','💞','💟','❣️','💋','💯','💢','💥','💫','💦','💨','🕳️','💬','🗨️','🗯️','💭','💤','💮','♨️','💈','🛑','🕛','🕐','🕑','🕒','🕓','🕔','🕕','🕖','🕗','🕘','🕙','🕚','🌑','🌒','🌓','🌔','🌕','🌖','🌗','🌘','🌙','🌚','🌛','🌜','🌡️','☀️','🌝','🌞','🪐','⭐','🌟','✨','🌠','🌌','☁️','⛅','⛈️','🌤️','🌥️','🌦️','🌧️','🌨️','🌩️','🌪️','🌫️','🌬️','🌀','🌈','🌂','☂️','☔','⛱️','⚡','❄️','☃️','⛄','☄️','🔥','💧','🌊','🎄','✨','🎀','🎁','🎂','🎃','🎗️','🎟️','🎫','🎖️','🏆','🏅','🥇','⚽','🏀','✅','❌','❓','❗','❕','❔','‼️','⁉️','💯','🔆','🔅','🎵','🎶','➕','➖','➗','✖️','♾️','💲','💱','™️','©️','®️','〰️','➰','➿','✔️','☑️','🔘','🔴','🟠','🟡','🟢','🔵','🟣','⚫','⚪','🟤','🔺','🔻','🔸','🔹','🔶','🔷','🔳','🔲','▪️','▫️','◾','◽','◼️','◻️','🟥','🟧','🟨','🟩','🟦','🟪','⬛','⬜','🟫'],
    },
];

const RECENT_KEY = 'myraion.chat.emoji.recent';

function emojiToTwemoji(emoji: string): string {
    const codes: string[] = [];
    for (const ch of emoji) {
        const cp = ch.codePointAt(0);
        if (cp == null || cp === 0xfe0f) continue;
        codes.push(cp.toString(16));
    }
    return `https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/svg/${codes.join('-')}.svg`;
}

function readRecent(): string[] {
    try {
        const raw = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]');
        return Array.isArray(raw) ? raw.filter((x) => typeof x === 'string').slice(0, 24) : [];
    } catch {
        return [];
    }
}

function writeRecent(emoji: string) {
    const next = [emoji, ...readRecent().filter((item) => item !== emoji)].slice(0, 24);
    try {
        localStorage.setItem(RECENT_KEY, JSON.stringify(next));
    } catch {
        // ignore
    }
}

export function insertEmojiAtCursor(
    input: HTMLInputElement | HTMLTextAreaElement | null,
    emoji: string,
    value: string,
    setValue: (next: string) => void
) {
    const start = input?.selectionStart ?? value.length;
    const end = input?.selectionEnd ?? value.length;
    const next = value.slice(0, start) + emoji + value.slice(end);
    setValue(next);
    requestAnimationFrame(() => {
        if (!input) return;
        input.focus();
        const pos = start + emoji.length;
        input.setSelectionRange(pos, pos);
    });
}

function Twemoji({ emoji, className }: { emoji: string; className?: string }) {
    const [failed, setFailed] = useState(false);
    if (failed) return <span className={className}>{emoji}</span>;
    return (
        <img
            src={emojiToTwemoji(emoji)}
            alt={emoji}
            draggable={false}
            className={className}
            onError={() => setFailed(true)}
        />
    );
}

export default function ChatEmojiPicker({
    onPick,
    onClose,
}: {
    onPick: (emoji: string) => void;
    onClose: () => void;
}) {
    const [tab, setTab] = useState(CATEGORIES[0].id);
    const [query, setQuery] = useState('');
    const [recent, setRecent] = useState<string[]>(readRecent);

    const visible = useMemo(() => {
        const q = query.trim();
        if (q) {
            const all = CATEGORIES.flatMap((c) => c.emojis);
            return all.filter((e) => e.includes(q)).slice(0, 80);
        }
        if (tab === 'recent') return recent;
        return CATEGORIES.find((c) => c.id === tab)?.emojis || [];
    }, [query, tab, recent]);

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    const pick = (emoji: string) => {
        writeRecent(emoji);
        setRecent(readRecent());
        onPick(emoji);
    };

    return (
        <div className="absolute bottom-12 right-0 z-50 w-[320px] bg-white border border-[#1A1916]/10 shadow-2xl rounded-2xl overflow-hidden animate-fadeIn">
            <div className="px-3 pt-3 pb-2">
                <input
                    autoFocus
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Поиск эмодзи"
                    className="w-full px-3 py-2 text-xs bg-[#EFEAF6] border border-[#1C1824]/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/20"
                />
            </div>
            <div className="h-[220px] overflow-y-auto px-2 pb-2">
                {visible.length === 0 ? (
                    <div className="text-xs text-slate-400 text-center py-10">Ничего не найдено</div>
                ) : (
                    <div className="grid grid-cols-8 gap-0.5">
                        {visible.map((emoji, index) => (
                            <button
                                key={`${emoji}-${index}`}
                                type="button"
                                onClick={() => pick(emoji)}
                                className="w-9 h-9 rounded-lg hover:bg-[#EDE6F5] flex items-center justify-center"
                                title={emoji}
                            >
                                <Twemoji emoji={emoji} className="w-6 h-6" />
                            </button>
                        ))}
                    </div>
                )}
            </div>
            {!query && (
                <div className="flex items-center gap-0.5 px-2 py-1.5 border-t border-slate-100 bg-[#FAF8FC]">
                    <button
                        type="button"
                        onClick={() => setTab('recent')}
                        className={`w-8 h-8 rounded-lg flex items-center justify-center ${tab === 'recent' ? 'bg-white shadow-sm' : 'hover:bg-white/70'}`}
                        title="Недавние"
                    >
                        <span className="text-sm">🕒</span>
                    </button>
                    {CATEGORIES.map((c) => (
                        <button
                            key={c.id}
                            type="button"
                            onClick={() => setTab(c.id)}
                            className={`w-8 h-8 rounded-lg flex items-center justify-center ${tab === c.id ? 'bg-white shadow-sm' : 'hover:bg-white/70'}`}
                            title={c.label}
                        >
                            <Twemoji emoji={c.icon} className="w-4 h-4" />
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
