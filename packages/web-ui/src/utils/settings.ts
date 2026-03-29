import type {Settings} from "../types/Settings.ts";

const SETTINGS_KEY = "words_settings";

export function updateSettings(settings: Partial<Settings>): void {
    const newSettings = {
        ...loadSettings(),
        ...settings,
    }
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(newSettings));
}

export function loadSettings(): Settings {
    const json = localStorage.getItem(SETTINGS_KEY);
    return json == null ? {} : JSON.parse(json);
}

