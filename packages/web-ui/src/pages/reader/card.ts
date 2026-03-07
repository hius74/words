import {getElementById} from "../../utils/dom.ts";
import type {Database} from "../../utils/database.ts";

export class Card {

    private readonly database: Database;

    private readonly word: HTMLInputElement;

    private readonly sentence: HTMLInputElement;

    constructor(database: Database, wordId: string, sentenceId: string) {
        this.database = database;
        this.word = getElementById(wordId, HTMLInputElement);
        this.sentence = getElementById(sentenceId, HTMLInputElement);
    }

    /**
     * New unknown word (user click on word in editor)
     * @param text
     * @return true if the card not found and need set sentence
     */
    async unknownWord(text: string): Promise<boolean> {
        this.word.value = text;

        const word = await this.database.findWord(text);
        if (word != null) {
            console.log('Find word', word);
        } else {
            console.log('New word', text);
        }

        return word == null;
    }

    setSentence(text: string) {
        this.sentence.value = text;
    }

    /**
     * Save card in DB
     */
    async save() {
        const word = this.word.value;
        if (word.length > 0) {
            await this.database.save({
                word: this.word.value,
                sentence: this.sentence.value,
                description: "",
                frontSides: [],
                nextTime: 0,
                stage: 0,
            });
        }
    }

    /**
     * Export from Database to JSON file.
     * Will use Streaming API because DB can be large.
     */
    async export() {
        if (!("showSaveFilePicker" in window)) {
            alert("Your browser does not support file streaming export.");
            return;
        }
        // Ask user where to save file
        try {
            const fileHandle: FileSystemFileHandle = await (window as any).showSaveFilePicker({
                suggestedName: "cards.json",
                types: [
                    {
                        description: "JSON File",
                        accept: { "application/json": [".json"] },
                    },
                ],
            });

            const writable = await fileHandle.createWritable();
            const BATCH_SIZE = 1000;
            let lastKey: IDBValidKey | undefined = undefined;
            while (true) {
                const { records, lastKey: newLastKey } = await this.database.readBatch(BATCH_SIZE, lastKey);
                if (records.length === 0) {
                    break;
                }
                // Convert batch to NDJSON string
                let chunk = "";
                for (const record of records) {
                    chunk += JSON.stringify(record) + "\n";
                }
                await writable.write(chunk);
                lastKey = newLastKey;
            }
            await writable.close();
        } catch (error) {
            if (error instanceof DOMException && error.name === "AbortError") {
                console.log("User cancelled file picker.");
                return;
            }
            console.log("Error on export cards", error);
            alert("Error: " + error);
        }
    }

    /**
     * Import JSON file into Database.
     */
    async import() {

    }
}
