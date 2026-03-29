import {loadSettings, updateSettings} from "../../utils/settings.ts";
import {getElementById} from "../../utils/dom.ts";
import {readTextFile} from "../../utils/fileReader.ts";
import {extractSentence, extractWord} from "../../utils/wordExctract.ts";
import type {Card} from "./card.ts";

export class Editor {
    private readonly editor: HTMLTextAreaElement
    private readonly fileInput: HTMLInputElement;
    private readonly card: Card;

    constructor(textAreaId: string, fileInputId: string, card: Card) {
        this.editor = getElementById(textAreaId, HTMLTextAreaElement);
        this.fileInput = getElementById(fileInputId, HTMLInputElement);
        this.card = card;

        const settings = loadSettings();
        this.editor.value = settings.text || '';
        this.editor.style.fontSize = (settings.fontSize || 14) + 'px';

        this.editor.addEventListener("dragover", (e: DragEvent) => {
            e.preventDefault();
        });

        this.editor.addEventListener("drop", async (e: DragEvent) => {
            e.preventDefault();

            const file = e.dataTransfer?.files[0];
            if (!file) return;

            const text = await readTextFile(file);
            this.editor.value = text;
            updateSettings({
                text,
                filename: file.name,
            })
        });

        this.editor.addEventListener("close", () => {
            const text = this.editor.value;
            if (text.length > 0 ) {
                updateSettings({
                    text: this.editor.value,
                });
            }
        });

        this.editor.addEventListener("click", async (e: PointerEvent) => {
            const position = document.caretPositionFromPoint(e.clientX, e.clientY);
            if (position == null) return;

            const text = this.editor.value;
            const pos = position.offset;
            const word = extractWord(text, pos);
            if (word != null) {
                if (await this.card.unknownWord(word)) {
                    const sentence = extractSentence(text, pos);
                    if (sentence) {
                        this.card.setSentence(sentence);
                    }
                }
            }
        });

        this.fileInput.addEventListener("change", async () => {
            const file: File | undefined = this.fileInput.files?.[0];
            if (file) {
                try {
                    this.editor.value = await file.text();
                } catch (error) {
                    console.error("Error reading file:", error);
                }
            }

            // reset input so same file can be selected again
            this.fileInput.value = "";
        });
    }

    increaseFont() {
        const settings = loadSettings();
        const fontSize = (settings.fontSize || 14) + 1;
        console.log('New font size: ', fontSize);
        this.editor.style.fontSize = fontSize + 'px';
        updateSettings({
            fontSize,
        });
    }

    decreaseFont() {
        const settings = loadSettings();
        const fontSize = Math.max((settings.fontSize || 14) - 1, 8);
        console.log('New font size: ', fontSize);
        this.editor.style.fontSize = fontSize + 'px';
        updateSettings({
            fontSize,
        });
    }

    upload() {
        this.fileInput.click();
    }

    save() {
        updateSettings({
            text: this.editor.value,
        })
    }

    download() {
        const text = this.editor.value;
        if (text.length > 0) {
            const settings = loadSettings();
            const blob = new Blob([text], { type: "text/plain" });
            const a = document.createElement("a");
            a.href = URL.createObjectURL(blob);
            a.download = settings.filename || "words_boek.txt";
            a.click();
        }
    }
}
