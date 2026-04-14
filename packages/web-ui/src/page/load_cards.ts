import {getElementById} from "../util/dom.ts";
import {readNDJSON} from "../util/stream-ndjson.ts";
import {parseCard} from "@words/gen-ai/src/type/card.ts";
import {dbReadBatch, dbSave} from "../util/database.ts";

// Load card from server with fetch and store in DB
/* getElementById<HTMLButtonElement>('load-cards', HTMLButtonElement)
    .addEventListener('click', async () => {
        const result = await saveCards('./cards.ndjson');
        alert(`Loaded: ${result}`);
    });*/

// parse upload file and store in DB
getElementById<HTMLInputElement>('load-cards-file', HTMLInputElement)
    .addEventListener('change', async (event: Event) => {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        if (!file) return;
        const result = await saveCards(file);
        alert(`Loaded: ${JSON.stringify(result, null, 2)}`);
    });

// Export all records
getElementById<HTMLButtonElement>('export-all', HTMLButtonElement)
    .addEventListener('click', async () => {
        // Ask user where to save
        const fileHandle = await window.showSaveFilePicker({
            suggestedName: "cards.ndjson",
            types: [{
                description: "NDJSON file",
                accept: { "application/x-ndjson": [".ndjson"] }
            }]
        });

        const writable = await fileHandle.createWritable();
        try {
            await downloadFile(writable);
        } catch (err) {
            alert(`Error while exporting: ${err}`);
        }
    });

async function saveCards(source: string | File) {
    let total = 0;
    let errors = 0;
    let newCards = 0;
    await readNDJSON<unknown>(source, {
        onObject: async (obj) => {
            total++;
            try {
                const card = parseCard(obj);
                if (await dbSave(card)) {
                    newCards++;
                }
            } catch (err) {
                errors++;
                console.log('Error on store card in DB', err, obj);
            }
        }
    });
    return {
        total,
        errors,
        newCards,
    }
}

// Download file
async function downloadFile(writable: FileSystemWritableFileStream) {
    let lastKey = undefined;
    let total = 0;

    try {
        while (true) {
            const { records, lastKey: newLastKey } =
                await dbReadBatch(25, lastKey);

            if (!records || records.length === 0) {
                break;
            }

            // Convert batch to NDJSON
            const chunk = records.map(({id, ...rest}) => JSON.stringify(rest)).join("\n") + "\n";

            // Write immediately (streaming)
            await writable.write(chunk);

            total += records.length;
            lastKey = newLastKey;

            // Optional: yield to UI thread (important for mobile)
            await new Promise(r => setTimeout(r, 0));
        }

        await writable.close();
        console.log("Export done. Total records:", total);

    } catch (err) {
        console.error("Export failed:", err);
        await writable.abort();
        throw err;
    }
}