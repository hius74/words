/**
 * Words application (repeat words).
 */
import { DataStorage } from '../services/dataStorage.js';

const dataStorage = new DataStorage('XXX');

(document.getElementById("add") as HTMLButtonElement).onclick = () => {
    alert(dataStorage.getDbName());
};
