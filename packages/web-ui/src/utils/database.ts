import type {Word} from "../types/Word.ts";

export class Database {

    private readonly dbName = "cards";

    private readonly cardsObjectStoreName = "cards";

    private readonly cardsObjectStoreWordsIndexName = "word_index";

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
                const objectStore = db.createObjectStore(this.cardsObjectStoreName, { autoIncrement: true, });
                const wordPath: keyof Word = 'word';
                objectStore.createIndex(this.cardsObjectStoreWordsIndexName, wordPath, { unique: true });
                const nextTimePath: keyof Word = 'nextTime';
                objectStore.createIndex(this.cardsObjectStoreNextTimeIndexName, nextTimePath);
            }
        })
    }

    async findWord(word: string): Promise<Word | null> {
        return new Promise((resolve, error) => {
            const transaction = this.db?.transaction([this.cardsObjectStoreName]);
            if (transaction == null) {
                error('Can not create transaction');
            } else {
                const objectStore = transaction.objectStore(this.cardsObjectStoreName);
                if (objectStore == null) {
                    error('Can not find object store');
                } else {
                    const index = objectStore?.index(this.cardsObjectStoreWordsIndexName);
                    if (index == null) {
                        error('Can not open index');
                    } else {
                        const request =  index.get(word);
                        request.onsuccess = () => {
                            resolve(request.result);
                        }
                        request.onerror = () => {
                            error(request.error);
                        }
                    }
                }
            }
        });
    }

    async save(word: Word) {
        return new Promise((resolve, error) => {
            const transaction = this.db?.transaction([this.cardsObjectStoreName], "readwrite");
            if (transaction == null) {
                error('Can not create transaction');
            } else {
                const objectStore = transaction.objectStore(this.cardsObjectStoreName);
                if (objectStore == null) {
                    error('Can not find object store');
                } else {
                    const request = objectStore.add(word);
                    request.onsuccess = () => resolve(request.result);
                    request.onerror = () => error(request.error);
                }
            }
        });
    }

    /**
     * IndexedDB doesn't support async/await inside the transaction.
     * @param batchSize
     * @param lastKey
     */
    async readBatch(batchSize: number, lastKey?: IDBValidKey): Promise<{ records: Word[]; lastKey?: IDBValidKey }> {
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
                const records: Word[] = [];
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
