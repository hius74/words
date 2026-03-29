import { z } from 'zod';
import * as fs from "node:fs";
import {fileURLToPath} from "url";
import path from "path";

/**
 * Settings for Google API
 */
const settingsSchema = z.object({
    api: z.string(),
    model: z.string(),
});

export type Settings = z.infer<typeof settingsSchema>;

export function readSettings(filename?: string): Settings {
    if (!filename) {
        const __filename = fileURLToPath(import.meta.url);
        const __dirname = path.dirname(__filename);
        filename = __dirname + '/../../../genai-settings.json';
    }

    const data = fs.readFileSync(filename);
    const text = data.toString();
    const json = JSON.parse(text);
    return settingsSchema.parse(json);
}
