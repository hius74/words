import "../../styles/global.css";
import "./main.css";
import {getElementById} from "../../utils/dom.ts";
import {Editor} from "./editor.ts";
import {Card} from "./card.ts";
import {Database} from "../../utils/database.ts";

function bootstrap(): void {
    const database = new Database();

    const card = new Card(database, 'card_word', 'card_sentence')
    getElementById("card_save", HTMLButtonElement).addEventListener('click', () => card.save());
    getElementById("card_export", HTMLButtonElement).addEventListener('click', () => card.export());
    getElementById("card_import", HTMLButtonElement).addEventListener('click', () => card.import());

    const editor = new Editor('editor', 'editor_file_upload', card);
    getElementById("editor_font_decrease", HTMLButtonElement).addEventListener('click', () => editor.decreaseFont());
    getElementById("editor_font_increase", HTMLButtonElement).addEventListener('click', () => editor.increaseFont());
    getElementById("editor_upload", HTMLButtonElement).addEventListener('click', () => editor.upload());
    getElementById("editor_save", HTMLButtonElement).addEventListener('click', () => editor.save());
    getElementById("editor_download", HTMLButtonElement).addEventListener('click', () => editor.download());
}

document.addEventListener("DOMContentLoaded", bootstrap);
