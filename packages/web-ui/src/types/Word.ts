
export type Word = {
    /**
     * Learning word
     */
    word: string;

    /**
     * Sentence from the book (original)
     */
    sentence: string;

    /**
     * Stage of repeating card
     */
    stage: number;

    /**
     * Next repeating time
     */
    nextTime: number;

    /**
     * Description on NL
     */
    description: string;

    /**
     * Front card views
     */
    frontSides: string[];
}
