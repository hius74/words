import * as fs from "fs";
import * as readline from "readline";
import { z } from "zod";
import { GoogleGenAI } from "@google/genai";
import {type Card, parseCard} from "../type/card.ts";
import {getRootFolderPath, readSettings} from "../lib/settings.ts";
import {loadCards, saveCards} from "../lib/cards-storage.ts";

const verbSchema = z.object({
    description: z.string().describe('Werkwoord beschrijven in het Nederlands zonder het woord zelf in de beschrijving te gebruiken. De minimale zinslengte is 20 woorden.'),
    ott: z.object({
        singular_1: z.object({
            verb: z.string().describe('Werkwoord in de tegenwoordige tijd van de eerste persoon enkelvoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de tegenwoordige tijd van de eerste persoon enkelvoud. De minimale zinslengte is 7 woorden.'),
        }),
        singular_2_3: z.object({
            verb: z.string().describe('Werkwoord in de tegenwoordige tijd van de tweede of derde persoon enkelvoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de tegenwoordige tijd van de tweede of derde persoon enkelvoud. De minimale zinslengte is 7 woorden.'),
        }),
        plural: z.object({
            verb: z.string().describe('Het werkwoord staat in de tegenwoordige tijd meervoud.'),
            sentence: z.string().describe('Een zin met een werkwoord staat in de tegenwoordige tijd meervoud. De minimale zinslengte is 7 woorden.'),
        }),
    }).describe('Tegenwoordige tijd (OTT)'),
    ovt: z.object({
        singular: z.object({
            verb: z.string().describe('Werkwoord in de onvoltooid verleden tijd enkelvoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de onvoltooid verleden tijd enkelvoud. De minimale zinslengte is 7 woorden.'),
        }),
        plural: z.object({
            verb: z.string().describe('Werkwoord in de onvoltooid verleden tijd meervoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de onvoltooid verleden tijd meervoud. De minimale zinslengte is 7 woorden.'),
        }),
    }).describe('Onvoltooid verleden tijd (OVT)'),
    vtt: z.object({
        singular: z.object({
            verb: z.string().describe('Werkwoord in de voltooid tegenwoordige tijd enkelvoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de voltooid tegenwoordige tijd enkelvoud. De minimale zinslengte is 7 woorden.'),
        }),
        plural: z.object({
            verb: z.string().describe('Werkwoord in de voltooid tegenwoordige tijd meervoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de voltooid tegenwoordige tijd meervoud. De minimale zinslengte is 7 woorden.'),
        }),
    }).describe('Voltooid tegenwoordige tijd (VTT)'),
    vvt: z.object({
        singular: z.object({
            verb: z.string().describe('Werkwoord in de voltooid verleden tijd enkelvoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de voltooid verleden tijd enkelvoud. De minimale zinslengte is 7 woorden.'),
        }),
        plural: z.object({
            verb: z.string().describe('Werkwoord in de voltooid verleden tijd meervoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de voltooid verleden tijd meervoud. De minimale zinslengte is 7 woorden.'),
        }),
    }).describe('Voltooid verleden tijd (VVT)'),
    ott_zullen: z.object({
        singular: z.object({
            verb: z.string().describe('Werkwoord in de toekomende tijd enkelvoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de toekomende tijd enkelvoud met hulpwoord zullen. De minimale zinslengte is 7 woorden.'),
        }),
        plural: z.object({
            verb: z.string().describe('Werkwoord in de toekomende tijd meervoud.'),
            sentence: z.string().describe('Een zin met een werkwoord in de toekomende tijd meervoud met hulpwoord zullen. De minimale zinslengte is 7 woorden.'),
        }),
    }).describe('Toekomende tijd (OTT + zullen)')
});

type Verb = z.infer<typeof verbSchema>;

/**
 * Return array of new verbs only
 */
async function readVerbs(cards: Map<string, Card>, filepath: string): Promise<string[]> {
    const fileStream = fs.createReadStream(filepath, "utf-8");
    const rl = readline.createInterface({
        input: fileStream,
        crlfDelay: Infinity,
    });
    const verbs: string[] = [];
    for await (const line of rl) {
        const verb = line.trim();
        if (verb.length > 0 && verb[0] != '#' && !cards.has(verb)) {
            verbs.push(verb);
        }
    }
    return verbs;
}

async function generateCards(verbs: string[]): Promise<Verb[]> {
    const prompt = `Je bent docent Nederlands op B1-niveau.
Je maakt flashcards om werkwoorden in verschillende vormen te memoriseren.
Maak flashcards voor de volgende werkwoorden:
- ${verbs.join('\n- ')}`;
    console.log(prompt);
    const settings = readSettings();
    const ai = new GoogleGenAI({
        apiKey: settings.api,
    });
    // Process multiple verbs (batch)
    const verbsSchema = z.array(verbSchema);
    const response = await ai.models.generateContent({
        model: settings.model,
        contents: prompt,
        config: {
            responseMimeType: "application/json",
            responseJsonSchema: z.toJSONSchema(verbsSchema),
        },
    });
    console.log(response.text);
    if (response.text) {
        return verbsSchema.parse(JSON.parse(response.text));
    }
    throw new Error('Response is invalid');
}

/**
 * Replace learning word to mask
 */
function maskWords(sentence: string, words: string[]): string {
    // Escape regex special characters in words
    const escapedWords = words.map(w => w.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));

    // Build regex pattern for whole words
    const pattern = new RegExp(`\\b(${escapedWords.join("|")})\\b`, "gi");

    // Replace matches with [...]
    return sentence.replace(pattern, "[...]");
}

/**
 * Generate front and backward side of card
 */
function generateCard(description: string, {verb, sentence}: {verb: string, sentence: string}, hint?: string): Card {
    // Let zod validate array length and fill defaults
    const maskSentence = maskWords(sentence, verb.split(' '));
    return parseCard({
        name: verb,
        sides: [
            `<p>${description}</p>${hint ? `<p>${hint}</p>`: ''}<p>${maskSentence}</p>`,
            `<p>${description}</p><p>${sentence}<p>`,
        ],
    });
}

function setNewCard(cards: Map<string, Card>, verb: Verb) {
    cards.set(verb.ott.singular_1.verb, generateCard(verb.description, verb.ott.singular_1, verbSchema.def.shape.ott.def.shape.singular_1.def.shape.verb.description));
    cards.set(verb.ott.singular_2_3.verb, generateCard(verb.description, verb.ott.singular_2_3, verbSchema.def.shape.ott.def.shape.singular_2_3.def.shape.verb.description));
    cards.set(verb.ott.plural.verb, generateCard(verb.description, verb.ott.plural, verbSchema.def.shape.ott.def.shape.plural.def.shape.verb.description));

    cards.set(verb.ovt.singular.verb, generateCard(verb.description, verb.ovt.singular, verbSchema.def.shape.ovt.def.shape.singular.description));
    cards.set(verb.ovt.plural.verb, generateCard(verb.description, verb.ovt.plural, verbSchema.def.shape.ovt.def.shape.plural.def.shape.verb.description));

    cards.set(verb.vtt.singular.verb, generateCard(verb.description, verb.vtt.singular, verbSchema.def.shape.vtt.def.shape.singular.def.shape.verb.description));
    cards.set(verb.vtt.plural.verb, generateCard(verb.description, verb.vtt.plural, verbSchema.def.shape.vtt.def.shape.plural.def.shape.verb.description));

    cards.set(verb.vvt.singular.verb, generateCard(verb.description, verb.vvt.singular, verbSchema.def.shape.vvt.def.shape.singular.def.shape.verb.description));
    cards.set(verb.vvt.plural.verb, generateCard(verb.description, verb.vvt.plural, verbSchema.def.shape.vvt.def.shape.plural.def.shape.verb.description));

    cards.set(verb.ott_zullen.singular.verb, generateCard(verb.description, verb.ott_zullen.singular, verbSchema.def.shape.ott_zullen.def.shape.singular.def.shape.verb.description));
    cards.set(verb.ott_zullen.plural.verb, generateCard(verb.description, verb.ott_zullen.plural, verbSchema.def.shape.ott_zullen.def.shape.plural.def.shape.verb.description));
}

/**
 * Create cards for verbs.
 * Load all cards, check if the learning ward is missing, send request and store result
 */
async function verbs() {
    const cardsFilename= `${getRootFolderPath()}cards.ndjson`
    const cards = await loadCards(cardsFilename);

    const verbs = await readVerbs(cards, `${getRootFolderPath()}verbs.txt`);
    if (verbs.length > 0) {
        (await generateCards(verbs)).forEach((verb) => setNewCard(cards, verb));
        await saveCards(cards, cardsFilename)
    } else {
        console.log('No new verbs found.');
    }
}

await verbs();
