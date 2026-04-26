import {dbGetFirstCards, dbNextStage} from "../util/database.ts";
import {getElementById} from "../util/dom.ts";
import type {Card} from "@words/gen-ai/src/type/card.ts";

const cards = await dbGetFirstCards(10);

let cardIdx = -1; // newCard function increment index
let cardFaceIdx = 0;
let errorAnswer = false;

const progressElement = getElementById<HTMLProgressElement>('progress', HTMLProgressElement);
progressElement.value = 0;
progressElement.max = cards.length;

const cardElement = getElementById<HTMLDivElement>('card', HTMLDivElement);

const helpElement = getElementById<HTMLButtonElement>('help', HTMLButtonElement);
helpElement.addEventListener('click', () => {
    help();
});

const inputElement = getElementById<HTMLInputElement>('answer', HTMLInputElement);
inputElement.addEventListener('keydown', async (event) => {
    if (event.key === "Enter") {
        const value = inputElement.value;
        const card = cards[cardIdx];
        if (card.name === value) {
            if (!errorAnswer) {
                // Update time if answer was success from first time
                const {level, nextTime} = calculateNextTime(card);
                await dbNextStage(card.id, level, nextTime);
            }
            newCard();
        } else {
            errorAnswer = true;
            // Show correct answer
            inputElement.value = card.name;
        }
    }
});

/**
 * Help Button
 */
function help() {
    if (cardIdx < cards.length) {
        cardFaceIdx++;
        const card = cards[cardIdx];
        if (cardFaceIdx < card.sides.length - 1) {
            cardElement.innerHTML = card.sides[cardFaceIdx];
        } else {
            helpElement.disabled = true;
        }
    }
}

/**
 * new Card
 */
function newCard() {
    cardIdx++;
    if (cardIdx < cards.length) {
        const card = cards[cardIdx];
        progressElement.value = cardIdx;
        cardFaceIdx = 0;
        cardElement.innerHTML = card.sides[cardFaceIdx];
        helpElement.disabled = cards[cardIdx].sides.length >= 2;
        inputElement.value = '';
        errorAnswer = false;
    } else {
        alert('Finished')
    }
}

/**
 * Calculate next time if the answer was success
 */
function calculateNextTime(card: Card) {
    return {
        level: card.level + 1,
        nextTime: card.nextTime + card.level * 3 * 60 * 60 * 1000,
    }
}

if (cards.length > 0) {
    newCard();
} else {
    cardElement.innerHTML = 'No card found to learn. Please <a href="./load_cards.html">load cards.</a>';
    helpElement.disabled = true;
    inputElement.disabled = true;
}
