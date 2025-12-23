/**
 * Store prompt into a clipboard for werkWorden.
 */

const text = document.getElementById('werkwoorden') as HTMLTextAreaElement;
const result = document.getElementById('store_werkwoorden_result') as HTMLSpanElement;

async function werkwoordenPrompt() {

    const lines = text?.value.split(/\r?\n|\r|\n/g);
    const words = lines
        .map(line => line.trim())
        .filter(line => line.length > 0)
        .map(line => `- ${line}`);

    const prompt = `Van de volgende werkwoorden in het Nederlands:

${words.join('\n')}

maak een tabel in het Nederlands met de volgende kolommen:

- Enkelvoud_TW: ik form van het werkwoord in het tegenwoordige tijd;
- Meervoud_TW: meervoud form van het werkwoord in het tegenwoordige tijd;
- Enkelvoud_VT: ik vorm van het werkwoord in het verleende tijd;
- Meervoud_VT: meervoud form van werkwoord in het verleende tijd;
- Voltooide_T: het werkwoord in het voltooide tijd;
- Met_H: gebruikt het werkwoord met hebben: Ja of Nee;
- Met_Z: gebruikt het werkwoord met zijn: Ja of Nee;
- Uitleg_NL: uitleg van het werkwoord op niveau B1 in het Nederlands;
- Uitleg_RU: uitleg van het werkwoord in het Rus;
- Enkelvoud_TW_Zin: een voorbeeldzin in de tegenwoordige tijd van een enkelvoudig werkwoord op niveau B1;
- Meervoud_TW_Zin: een voorbeeldzin in de tegenwoordige tijd van een meervoudig werkwoord op niveau B1;
- Enkelvoud_VT_Zin: een voorbeeldzin in de verleende tijd van een enkelvoudig werkwoord op niveau B1;
- Meervoud_VT_Zin: een voorbeeldzin in de verleende tijd van een meervoudig werkwoord op niveau B1;
- Voltooide_T_Zin: een voorbeeldzin in de  voltooide tijd van een werkwoord op niveau B1.`;
    await navigator.clipboard.writeText(prompt);
    result.innerText = 'OK';
    setTimeout(() => result.innerText = '', 2000);
}

(document.getElementById('werkwoorden_prompt') as HTMLButtonElement).onclick = werkwoordenPrompt;
