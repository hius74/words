import { test } from 'node:test';
import assert from 'node:assert/strict';
import 'fake-indexeddb/auto';

import * as storage from './dataStorage.ts';

test('Add new Card and Get it', async () => {
    const db = await storage.open();
    const card = await storage.findCard(db, 'test');
    assert(card === undefined);
    const newCard = {
        id: 1,
        forwardSide: 'test',
        backwardSide: 'test',
        answer: 'test',
        nextRepeatTime: 0,
        stage: 0,
    };
    await storage.addCard(db, newCard);
    const card2 = await storage.findCard(db, newCard.answer);
    assert.deepEqual(card2, newCard);

    // Adding the card with the same name
    await assert.rejects(
        storage.addCard(db, {
            id: 2,
            forwardSide: 'test 2',
            backwardSide: 'test 2',
            answer: 'test', // the same answer
            nextRepeatTime: 0,
            stage: 0,
        }),
    );

    db.close();
});
