export type ChatPollVoter = {
    userId: number;
    firstName?: string;
    lastName?: string;
    avatarUrl?: string | null;
};

export type ChatPollOption = {
    id: number;
    text: string;
    votes: number;
    selected: boolean;
    voters?: ChatPollVoter[];
};

export type ChatPoll = {
    id: number;
    messageId: number;
    chatId: number;
    creatorId: number;
    question: string;
    anonymous: boolean;
    multiple: boolean;
    closed: boolean;
    purpose?: string;
    closesAt?: string | null;
    totalVotes: number;
    options: ChatPollOption[];
};

/** Incoming WS poll is built for the voter; keep this viewer's ticks, take counts from the payload. */
export function mergeIncomingPoll(local: ChatPoll | undefined, incoming: ChatPoll): ChatPoll {
    if (!local) return incoming;
    const selected = new Set(local.options.filter((option) => option.selected).map((option) => option.id));
    return {
        ...incoming,
        options: incoming.options.map((option) => ({ ...option, selected: selected.has(option.id) })),
    };
}
