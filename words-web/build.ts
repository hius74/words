import { htmlPlugin } from '@craftamap/esbuild-plugin-html';
import * as es from 'esbuild';
import * as esbuild from 'esbuild';

const options: es.BuildOptions = {
    bundle: true,
    entryPoints: ['src/functions/wordsApp.ts', 'src/functions/wordsTable.ts', 'src/functions/wordsEdit.ts', 'html/words.css'],
    outdir: 'dist/',
    plugins: [
        htmlPlugin({
            files: [
                {
                    entryPoints: ['src/functions/wordsApp.ts', 'html/words.css'],
                    filename: 'words.html',
                    htmlTemplate: './html/words.html',
                    inline: true,
                },
                {
                    entryPoints: ['src/functions/wordsTable.ts', 'html/words.css'],
                    filename: 'words_list.html',
                    htmlTemplate: './html/words_list.html',
                    inline: true,
                },
                {
                    entryPoints: ['src/functions/wordsEdit.ts', 'html/words.css'],
                    filename: 'words_edit.html',
                    htmlTemplate: './html/words_edit.html',
                    inline: true,
                },
            ],
        }),
    ],
};

esbuild
    .build(options)
    .then((result) => {
        console.log(result);
    })
    .catch((err) => {
        console.error(err);
    });
