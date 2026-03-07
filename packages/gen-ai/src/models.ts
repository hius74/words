import path from 'path';
import { fileURLToPath } from 'url';

import { GoogleGenAI } from "@google/genai";
import {readSettings} from "./settings.ts"

// Display available models
async function models() {
    const __filename = fileURLToPath(import.meta.url);
    const __dirname = path.dirname(__filename);
    const settings = readSettings(__dirname + '/../../../genai-settings.json');
    const ai = new GoogleGenAI({
        apiKey: settings.api,
    });
    const pager = await ai.models.list({config: {pageSize: 100}});
    let page = pager.page;
    while (true) {
        for (const model of page) {
            console.log(model.name);
        }
        if (!pager.hasNextPage()) {
            break;
        }
        page = await pager.nextPage();
    }
}

await models()
