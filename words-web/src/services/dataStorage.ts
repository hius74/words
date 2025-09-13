import { Card } from '../types/Card.js';

/**
 * Function to work with indexedDb.
 * We don't use class to make esbuild bundled only necessary functions.
 */
const dbName = 'words';
const cardsStoreName = 'cards';
const cardsIndexName = 'cards_answer';

/**
 * To support adding new cards ordering, we increment time up to this time
 */
const maxNewCardTime = 1_000_000;

export async function open(): Promise<IDBDatabase> {
    const request = indexedDB.open(dbName, 1);
    return new Promise((resolve, reject) => {
        request.onerror = () => {
            reject(request.error);
        };
        request.onsuccess = () => {
            resolve(request.result);
        };
        request.onupgradeneeded = (event) => {
            const db = request.result;
            const cardsStore = db.createObjectStore(cardsStoreName, { keyPath: "id", autoIncrement: true });
            cardsStore.createIndex(cardsIndexName, "answer", { unique: true });
            cardsStore.createIndex("card_nextRepeatTime", "nextRepeatTime", { unique: false });
        };
    });
}

/**
 * Find card by an answer.
 */
export async function findCard(db: IDBDatabase, answer: string): Promise<Card | undefined> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(cardsStoreName, "readonly");
        const store = transaction.objectStore(cardsStoreName);
        const request = store.index(cardsIndexName).get(answer);
        request.onerror = () => {
            reject(request.error);
        };
        request.onsuccess = () => {
            resolve(request.result);
        }
    })
}

export async function addCard(db: IDBDatabase, card: Card): Promise<void> {
    const totalNewCards = await getTotalNewCards(db);
    card.nextRepeatTime = totalNewCards + 1;
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(cardsStoreName, "readwrite");
        const store = transaction.objectStore(cardsStoreName);
        const request = store.add(card);
        request.onerror = () => {
            reject(request.error);
        };
        request.onsuccess = () => {
            resolve();
        }
    })
}

export async function updateCard(db: IDBDatabase, card: Card): Promise<void> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(cardsStoreName, "readwrite");
        const store = transaction.objectStore(cardsStoreName);
        const request = store.put(card);
        request.onerror = () => {
            reject(request.error);
        };
        request.onsuccess = () => {
            resolve();
        }
    })
}

export async function getTotalNewCards(db: IDBDatabase): Promise<number> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(cardsStoreName, "readonly");
        const store = transaction.objectStore(cardsStoreName);
        const index = store.index("card_nextRepeatTime");
        const request = index.count(IDBKeyRange.lowerBound(maxNewCardTime));
        request.onerror = () => {
            reject(request.error);
        };
        request.onsuccess = () => {
            resolve(request.result);
        }
    })
}

export async function getCardsToLearn(db: IDBDatabase, limit: number): Promise<Card[]> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(cardsStoreName, "readonly");
        const store = transaction.objectStore(cardsStoreName);
        const index = store.index("card_nextRepeatTime");
        const request = index.getAll(IDBKeyRange.upperBound(maxNewCardTime), limit);
        request.onerror = () => {
            reject(request.error);
        };
        request.onsuccess = () => {
            resolve(request.result);
        }
    })
}
