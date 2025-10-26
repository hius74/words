export type Card = {
    /**
     * ID of the card for editing.
     */
    id: number;

    /**
     * Forward size of the card.
     * Learning word replaced with […]
     */
    forwardSide: string;

    /**
     * Backward size of the card.
     * Learning word show as BOLD.
     */
    backwardSide: string;

    /**
     * Answer to the card.
     */
    answer: string;

    /**
     * Next time to repeat the card.
     */
    nextRepeatTime: number;

    /**
     * Stage of the card.
     * Used to calculate next repeat time if get correct answer.
     */
    stage: number;
};
