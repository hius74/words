
export function extractWord(text: string, cursor: number): string | null {
    return extractByBoundary(
        text,
        cursor,
        /[\s.,!?;:()[\]{}"'`]/,
        false);
}

export function extractSentence(text: string, cursor: number): string | null {
    return extractByBoundary(
        text,
        cursor,
        /[.!?…\n]/,
        true
    );
}

/**
 * Extracts a text segment around cursor using boundary regex.
 *
 * @param text Full text
 * @param index Cursor position
 * @param boundaryRegex Regex that defines segment boundaries (must match separators)
 * @param includeBoundary Whether to include boundary character at end
 */
export function extractByBoundary(
    text: string,
    index: number,
    boundaryRegex: RegExp,
    includeBoundary: boolean = false
): string | null {

    if (!text || index < 0 || index > text.length) {
        return null;
    }

    // Ensure regex is not global (avoid lastIndex issues)
    const regex = new RegExp(boundaryRegex.source);

    // Find start
    let start = index;
    while (start > 0) {
        const char = text[start - 1];
        if (regex.test(char)) {
            break;
        }
        start--;
    }

    // Find end
    let end = index;
    while (end < text.length) {
        const char = text[end];
        if (regex.test(char)) {
            if (includeBoundary) end++;
            break;
        }
        end++;
    }

    const result = text.slice(start, end).trim();

    return result.length > 0 ? result : null;
}
