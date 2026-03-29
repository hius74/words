import { GoogleGenAI } from "@google/genai";
import {readSettings} from "./settings.ts"

// Display available models
async function models() {
    const settings = readSettings();
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
