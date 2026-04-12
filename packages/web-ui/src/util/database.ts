import type {Card} from "../../../gen-ai/src/type/card.ts";

/**
 * Card with IndexDb primary key.
 */
export type CardWithId = Card & {
    id?: number, // Primary key for DB able possible edit name value
}

export class Database {

    private readonly dbName = "cards";

    private readonly cardsObjectStoreName = "cards";

    private readonly cardsObjectStoreNamesIndexName = "name_index";

    private readonly cardsObjectStoreNextTimeIndexName = "next_time_index";

    private readonly version = 1;

    private db: IDBDatabase | null = null;

    constructor() {
        this.open().then(db => {
            this.db = db;
        });
    }

    async open() {
        return new Promise<IDBDatabase>((resolve, error) => {
            const request = window.indexedDB.open(this.dbName, this.version);

            request.onerror = () => {
                error(request.error);
            };

            request.onsuccess = () => {
                resolve(request.result);
            };

            request.onupgradeneeded = () => {
                const db = request.result;
                const objectStore = db.createObjectStore(this.cardsObjectStoreName, {
                    keyPath: 'id',
                    autoIncrement: true,
                });
                const namePath: keyof Card = 'name';
                objectStore.createIndex(this.cardsObjectStoreNamesIndexName, namePath, { unique: true });
                const nextTimePath: keyof Card = 'nextTime';
                objectStore.createIndex(this.cardsObjectStoreNextTimeIndexName, nextTimePath);
            }
        })
    }

    /**
     * Find card by name and return with db index (primary key)
     */
    async find(name: string): Promise<CardWithId | null> {
        return new Promise((resolve, reject) => {
            const transaction = this.db?.transaction([this.cardsObjectStoreName]);
            if (transaction == null) {
                reject('Can not create transaction');
            } else {
                const objectStore = transaction.objectStore(this.cardsObjectStoreName);
                if (objectStore == null) {
                    reject('Can not find object store');
                } else {
                    const index = objectStore?.index(this.cardsObjectStoreNamesIndexName);
                    if (index == null) {
                        reject('Can not open index');
                    } else {
                        const request = index.openCursor(name);
                        request.onsuccess = () => {
                            const cursor = request.result;
                            if (cursor) {
                                resolve({
                                    ...cursor.value,
                                    id: cursor.primaryKey,
                                });
                            } else {
                                resolve(null);
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
    async save(card: Card): Promise<boolean> {
        return new Promise((resolve, reject) => {
            const transaction = this.db?.transaction([this.cardsObjectStoreName], "readwrite");
            if (transaction == null) {
                reject('Can not create transaction');
            } else {
                const objectStore = transaction.objectStore(this.cardsObjectStoreName);
                if (objectStore == null) {
                    reject('Can not find object store');
                } else {
                    // Step 1: find existing card by name
                    const index = objectStore.index(this.cardsObjectStoreNamesIndexName);
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
    async readBatch(batchSize: number, lastKey?: IDBValidKey): Promise<{ records: CardWithId[]; lastKey?: IDBValidKey }> {
        const transaction = this.db?.transaction(this.cardsObjectStoreName, 'readonly');
        if (transaction == null) {
            throw new Error('Can not create transaction');
        } else {
            const objectStore = transaction.objectStore(this.cardsObjectStoreName);
            if (objectStore == null) {
                throw new Error('Can not find object store');
            } else {
                const range = lastKey !== undefined ? IDBKeyRange.lowerBound(lastKey, true) : undefined;
                const request = objectStore.openCursor(range);
                const records: Card[] = [];
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
}
