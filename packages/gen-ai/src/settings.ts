import { z } from 'zod';
import * as fs from "node:fs";

/**
 * Settings for Google API
 */
const settingsSchema = z.object({
    api: z.string(),
    model: z.string(),
});

export type Settings = z.infer<typeof settingsSchema>;

export function readSettings(filename: string): Settings {
    const data = fs.readFileSync(filename);
    const text = data.toString();
    const json = JSON.parse(text);
    return settingsSchema.parse(json);
}
