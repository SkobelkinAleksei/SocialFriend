const NAME_RE = /^[A-Za-zА-Яа-яЁё]+(?:[ -][A-Za-zА-Яа-яЁё]+)*$/;
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_RE = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{8,64}$/;

const RU_STEMS = [
    'хуй', 'хуя', 'хуе', 'хуи', 'хуё',
    'пизд', 'пезд',
    'ебан', 'ебат', 'еблан', 'ебло', 'ебуч', 'ёбан', 'ёбат', 'ёбуч',
    'бляд', 'блять', 'блядь', 'блят',
    'мудак', 'мудил',
    'залуп',
    'пидор', 'пидр', 'педик',
    'гандон',
    'нахуй', 'похуй', 'охуе', 'ахуе',
    'спизд', 'выеб', 'заеб', 'уебок', 'уёб',
    'сука', 'сучар',
];

const EN_WORDS = [
    'fuck', 'fucking', 'fucker', 'fucked', 'motherfucker',
    'shit', 'shitty', 'asshole', 'bitch', 'cunt',
    'dick', 'cock', 'pussy', 'bastard', 'slut', 'whore',
    'nigger', 'nigga',
    'blyat', 'blyad', 'pizda', 'pidor', 'suka', 'huy', 'nahuy',
];

export const SIGNUP = {
    nameMin: 2,
    nameMax: 30,
    passwordMin: 8,
    passwordMax: 64,
    minAge: 14,
    maxAge: 100,
};

export const EVENT = {
    titleMin: 3,
    titleMax: 50,
    descriptionMax: 2000,
    locationMin: 3,
    locationMax: 150,
    minLeadMinutes: 15,
    maxAheadMonths: 3,
    limitMin: 2,
    limitMax: 300,
    tagsMax: 8,
    tagMin: 2,
    tagMax: 24,
    photosMax: 5,
};

export function formatRuPhone(input: string): string {
    let d = input.replace(/\D/g, '');
    if (d.startsWith('8')) d = `7${d.slice(1)}`;
    if (!d.startsWith('7')) d = `7${d}`;
    d = d.slice(0, 11);
    const rest = d.slice(1);
    let out = '+7';
    if (rest.length > 0) out += ` (${rest.slice(0, 3)}`;
    if (rest.length >= 3) out += ')';
    if (rest.length > 3) out += ` ${rest.slice(3, 6)}`;
    if (rest.length > 6) out += `-${rest.slice(6, 8)}`;
    if (rest.length > 8) out += `-${rest.slice(8, 10)}`;
    return out;
}

export function toE164Ru(input: string): string | null {
    let d = input.replace(/\D/g, '');
    if (d.startsWith('8')) d = `7${d.slice(1)}`;
    if (!d.startsWith('7')) d = `7${d}`;
    if (d.length !== 11) return null;
    return `+${d}`;
}

export function ageYears(isoDate: string): number | null {
    if (!isoDate) return null;
    const born = new Date(`${isoDate}T00:00:00`);
    if (Number.isNaN(born.getTime())) return null;
    const now = new Date();
    let years = now.getFullYear() - born.getFullYear();
    const m = now.getMonth() - born.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < born.getDate())) years -= 1;
    return years;
}

export function isoDateShift(years: number): string {
    const d = new Date();
    d.setFullYear(d.getFullYear() + years);
    return d.toISOString().slice(0, 10);
}

export function hasProfanity(raw: string): boolean {
    if (!raw || !raw.trim()) return false;
    const normalized = raw.toLowerCase().replace(/ё/g, 'е');
    const spaced = normalized.replace(/[^a-zа-я0-9]+/gi, ' ').trim();
    if (!spaced) return false;
    for (const word of EN_WORDS) {
        const re = new RegExp(`\\b${word}\\b`, 'i');
        if (re.test(spaced)) return true;
    }
    const compact = spaced.replace(/\s+/g, '');
    return RU_STEMS.some((stem) => compact.includes(stem.replace(/ё/g, 'е')));
}

const MARKETPLACE_SELL = [
    'продам', 'продаю', 'продаем', 'продадим', 'продадите', 'продашь', 'продаете',
    'продажа', 'продаже', 'продажей', 'продажу', 'продаж',
    'продается', 'продаются',
    'продать', 'продавать',
    'перепродам', 'перепродаю',
];
const MARKETPLACE_BUY = ['куплю', 'скуплю', 'выкуплю', 'прикуплю'];
export const MARKETPLACE_MESSAGE =
    'Сервис не для объявлений о купле-продаже. Встреча — это совместное дело, а не витрина.';

function marketplaceSpaced(raw: string): string {
    return raw.toLowerCase().replace(/ё/g, 'е').replace(/[^a-zа-я0-9]+/gi, ' ').trim();
}

function hasRuWord(spaced: string, word: string): boolean {
    let from = 0;
    while (from <= spaced.length - word.length) {
        const i = spaced.indexOf(word, from);
        if (i < 0) return false;
        const leftOk = i === 0 || !isMarketplaceWordChar(spaced.charAt(i - 1));
        const end = i + word.length;
        const rightOk = end >= spaced.length || !isMarketplaceWordChar(spaced.charAt(end));
        if (leftOk && rightOk) return true;
        from = i + 1;
    }
    return false;
}

function isMarketplaceWordChar(c: string): boolean {
    return /[а-яa-z0-9]/i.test(c);
}

export function hasMarketplaceListing(raw: string): boolean {
    if (!raw || !raw.trim()) return false;
    const spaced = marketplaceSpaced(raw);
    if (!spaced) return false;
    if (MARKETPLACE_SELL.some((w) => hasRuWord(spaced, w))) return true;
    if (MARKETPLACE_BUY.some((w) => hasRuWord(spaced, w))) return true;
    if (hasRuWord(spaced, 'торг')) return true;
    return /(?:^| )(?:отдам|отдаю|отдаем)(?: [а-яa-z0-9]+){0,6} за \d/.test(` ${spaced} `);
}

export function validateName(value: string): string | null {
    const v = value.trim();
    if (!v) return 'Поле обязательно.';
    if (v.length < SIGNUP.nameMin || v.length > SIGNUP.nameMax) {
        return `От ${SIGNUP.nameMin} до ${SIGNUP.nameMax} символов.`;
    }
    if (!NAME_RE.test(v)) return 'Только буквы, пробел и дефис (например Анна-Мария).';
    return null;
}

export function validateEmail(value: string): string | null {
    const v = value.trim().toLowerCase();
    if (!v) return 'Укажите email.';
    if (v.length > 100 || !EMAIL_RE.test(v)) return 'Введите корректный email.';
    return null;
}

export function validatePhone(value: string): string | null {
    if (!toE164Ru(value)) return 'Телефон должен быть в формате +7 и 10 цифр.';
    return null;
}

export function validatePassword(value: string): string | null {
    if (!PASSWORD_RE.test(value)) {
        return 'Пароль: от 8 до 64 символов, заглавная, строчная буква и цифра.';
    }
    return null;
}

export function passwordHints(value: string): { ok: boolean; text: string }[] {
    return [
        { ok: value.length >= SIGNUP.passwordMin, text: 'от 8 символов' },
        { ok: /[A-Z]/.test(value), text: 'заглавная' },
        { ok: /[a-z]/.test(value), text: 'строчная' },
        { ok: /\d/.test(value), text: 'цифра' },
    ];
}

export function validateBirthday(isoDate: string): string | null {
    const years = ageYears(isoDate);
    if (years == null) return 'Укажите дату рождения.';
    if (years < SIGNUP.minAge) return 'Возраст не соответствует';
    if (years > SIGNUP.maxAge) return 'Укажите корректную дату рождения.';
    return null;
}

export function validateEventTitle(value: string): string | null {
    const v = value.trim();
    if (!v) return 'Укажите название.';
    if (v.length < EVENT.titleMin || v.length > EVENT.titleMax) {
        return `Название — от ${EVENT.titleMin} до ${EVENT.titleMax} символов.`;
    }
    if (hasProfanity(v)) return 'В названии нельзя использовать ненормативную лексику.';
    if (hasMarketplaceListing(v)) return MARKETPLACE_MESSAGE;
    return null;
}

export function validateEventDescription(value: string): string | null {
    if (value.length > EVENT.descriptionMax) {
        return `Описание не длиннее ${EVENT.descriptionMax} символов.`;
    }
    if (hasProfanity(value)) return 'В описании нельзя использовать ненормативную лексику.';
    if (hasMarketplaceListing(value)) return MARKETPLACE_MESSAGE;
    return null;
}

export function validateEventLocation(value: string, hasPoint: boolean): string | null {
    const v = value.trim();
    if (!v) return 'Укажите адрес.';
    if (v.length < EVENT.locationMin || v.length > EVENT.locationMax) {
        return `Адрес — от ${EVENT.locationMin} до ${EVENT.locationMax} символов.`;
    }
    if (!hasPoint) return 'Выберите адрес из подсказок — так точка на карте будет точной.';
    return null;
}

export function validateEventDateTime(date: string, time: string): string | null {
    if (!date || !time) return 'Укажите дату и время.';
    const when = new Date(`${date}T${time}:00`);
    if (Number.isNaN(when.getTime())) return 'Некорректные дата или время.';
    const min = new Date(Date.now() + EVENT.minLeadMinutes * 60 * 1000);
    const max = new Date();
    max.setMonth(max.getMonth() + EVENT.maxAheadMonths);
    if (when < min) return 'Встречу можно назначить не раньше чем через 15 минут.';
    if (when > max) return 'Встречу можно назначить не позже чем через 3 месяца.';
    return null;
}

export function validateEventLimit(limit: number): string | null {
    if (limit < EVENT.limitMin || limit > EVENT.limitMax) {
        return `Лимит участников — от ${EVENT.limitMin} до ${EVENT.limitMax}.`;
    }
    return null;
}

export function eventDateBounds(): { min: string; max: string } {
    const min = new Date();
    const max = new Date();
    max.setMonth(max.getMonth() + EVENT.maxAheadMonths);
    return {
        min: min.toISOString().slice(0, 10),
        max: max.toISOString().slice(0, 10),
    };
}
