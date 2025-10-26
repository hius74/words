/**
 * Words application (repeat words).
 */
import { open } from '../services/dataStorage.js';

let dataStorage: IDBDatabase;

(document.getElementById('add') as HTMLButtonElement).onclick = () => {

};

(async () => {
    dataStorage = await open();
})();
