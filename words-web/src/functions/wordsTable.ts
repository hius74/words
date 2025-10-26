/**
 * Display all words from the database.
 */

import { open } from '../services/dataStorage.js';

const dataStorage = await open();

// Display results in Table
function displayTableBody() {
    const bodyElement = document.getElementById('dutch_grammar-table-body');
    bodyElement?.replaceChildren();
}
