import type {Card} from "../../../gen-ai/src/type/card.ts";

/**
 * Main function working with DB.
 * Because this is single instance - only functions without classes.
 */


/**
 * Card with IndexDb primary key.
 */
export type CardWithId = Card & {
    id: number, // Primary key for DB able possible edit name value
}

const dbName = "cards";
const cardsObjectStoreName = "cards";
const cardsObjectStoreNamesIndexName = "name_index";
const cardsObjectStoreNextTimeIndexName = "next_time_index";
const version = 1;

const db = await open();

async function open(): Promise<IDBDatabase> {
    return new Promise<IDBDatabase>((resolve, error) => {
        const request = window.indexedDB.open(dbName, version);

        request.onerror = () => {
            error(request.error);
        };

        request.onsuccess = () => {
            resolve(request.result);
        };

        request.onupgradeneeded = () => {
            const db = request.result;
            const objectStore = db.createObjectStore(cardsObjectStoreName, {
                keyPath: 'id',
                autoIncrement: true,
            });
            const namePath: keyof Card = 'name';
            objectStore.createIndex(cardsObjectStoreNamesIndexName, namePath, {unique: true});
            const nextTimePath: keyof Card = 'nextTime';
            objectStore.createIndex(cardsObjectStoreNextTimeIndexName, nextTimePath);
        }
    });
}

/**
 * Find card by name and return with db index (primary key)
 */
export async function dbGetFirstCards(limit = 10): Promise<CardWithId[]> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction([cardsObjectStoreName]);
        if (transaction == null) {
            reject('Can not create transaction');
        } else {
            const objectStore = transaction.objectStore(cardsObjectStoreName);
            if (objectStore == null) {
                reject('Can not find object store');
            } else {
                const index = objectStore?.index(cardsObjectStoreNextTimeIndexName);
                if (index == null) {
                    reject('Can not open index');
                } else {
                    // Include lower range
                    const request = index.openCursor(IDBKeyRange.lowerBound(0, false));
                    const results: CardWithId[] = [];
                    request.onsuccess = (event) => {
                        const cursor = (event.target as IDBRequest<IDBCursorWithValue | null>).result;
                        if (!cursor || results.length >= limit) {
                            resolve(results);
                        } else {
                            results.push(cursor.value);
                            cursor.continue();
                        }
                    };
                    request.onerror = () => {
                        reject(request.error);
                    };
                }
            }
        }
    });
}

/**
 * Save card in DB
 * @param card
 * @return true for the new card, false - in case update
 */
export async function dbSave(card: Card): Promise<boolean> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction([cardsObjectStoreName], "readwrite");
        if (transaction == null) {
            reject('Can not create transaction');
        } else {
            const objectStore = transaction.objectStore(cardsObjectStoreName);
            if (objectStore == null) {
                reject('Can not find object store');
            } else {
                // Step 1: find existing card by name
                const index = objectStore.index(cardsObjectStoreNamesIndexName);
                const getRequest = index.get(card.name);
                getRequest.onerror = () => {
                    reject(getRequest.error);
                };
                let existingCard: CardWithId | undefined;
                getRequest.onsuccess = () => {
                    existingCard = getRequest.result;
                    // Step 2: decide add or update
                    const request = existingCard == null
                        ? objectStore.add(card)
                        : objectStore.put({
                            ...existingCard,
                            ...card
                        });
                    request.onerror = () => reject(request.error);
                };
                // Step 3: resolve when transaction completes
                transaction.oncomplete = () => resolve(!existingCard);
                transaction.onerror = () => reject(transaction.error);
                transaction.onabort = () => reject(transaction.error);
            }
        }
    });
}

    /**
     * IndexedDB doesn't support async/await inside the transaction.
     * This is the reason open a new transaction for each batch
     * @param batchSize
     * @param lastKey
     */
export async function dbReadBatch(batchSize: number, lastKey?: IDBValidKey): Promise<{ records: CardWithId[]; lastKey?: IDBValidKey }> {
    const transaction = db.transaction(cardsObjectStoreName, 'readonly');
    if (transaction == null) {
        throw new Error('Can not create transaction');
    } else {
        const objectStore = transaction.objectStore(cardsObjectStoreName);
        if (objectStore == null) {
            throw new Error('Can not find object store');
        } else {
            const range = lastKey !== undefined ? IDBKeyRange.lowerBound(lastKey, true) : undefined;
            const request = objectStore.openCursor(range);
            const records: CardWithId[] = [];
            let newLastKey: IDBValidKey | undefined;
            return new Promise((resolve, reject) => {
                request.onerror = () => reject(request.error);

                request.onsuccess = () => {
                    const cursor = request.result;

                    if (!cursor || records.length >= batchSize) {
                        resolve({ records, lastKey: newLastKey });
                        return;
                    }

                    records.push(cursor.value);
                    newLastKey = cursor.primaryKey;

                    cursor.continue();
                };
            });
        }
    }
}

export async function dbNextStage(id: number, level: number, nextTime: number): Promise<void> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction([cardsObjectStoreName], "readwrite");
        if (transaction == null) {
            reject('Can not create transaction');
        } else {
            const objectStore = transaction.objectStore(cardsObjectStoreName);
            if (objectStore == null) {
                reject('Can not find object store');
            } else {
                const getRequest = objectStore.get(id);
                let existingCard: CardWithId | undefined;
                getRequest.onsuccess = () => {
                    existingCard = getRequest.result;
                    if (existingCard) {
                        const putRequest = objectStore.put({
                            ...existingCard,
                            level,
                            nextTime,
                        });
                        putRequest.onerror = () => reject(putRequest.error);
                    }
                };
                transaction.oncomplete = () => resolve();
                transaction.onerror = () => reject(transaction.error);
                transaction.onabort = () => reject(transaction.error);
            }
        }
    });
}