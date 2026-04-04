import * as z from 'zod';

const cardSchema = z.object({
    name: z.string().describe('learning word used as an answer for this card'),
    sides: z.array(z.string()).min(1).describe('HTML cards. Last element is answer for this card. Other elements - cards with hiding information'),
    nextTime: z.number().default(0).describe('next time to repeat the card'),
    level: z.number().default(0).describe('level number of cards. Increases when correct answer.'),
});

export type Card = z.infer<typeof cardSchema>;

export function parseCard(value: unknown): Card {
    return cardSchema.parse(value);
}