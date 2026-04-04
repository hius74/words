import * as fs from "fs";
import * as readline from "readline";
import { pipeline } from "stream/promises";
import {type Card, parseCard} from "../type/card.ts";

export async function loadCards(filename: string): Promise<Map<string, Card>> {
    const map = new Map<string, Card>();

    if (!fs.existsSync(filename)) {
        return map;
    }

    const fileStream = fs.createReadStream(filename, { encoding: "utf-8" });
    const rl = readline.createInterface({
        input: fileStream,
        crlfDelay: Infinity,
    });

    for await (const line of rl) {
        const trimmed = line.trim();
        if (!trimmed) continue;

        const obj = JSON.parse(trimmed);
        const card = parseCard(obj);
        map.set(card.name, card);
    }

    return map;
}

export async function saveCards(cards: Map<string, Card>, filename: string): Promise<void> {
    async function* generateLines() {
        for (const card of cards.values()) {
            yield JSON.stringify(card) + "\n";
        }
    }

    await pipeline(
        generateLines(),
        fs.createWriteStream(filename, { encoding: "utf-8" }),
    );
}