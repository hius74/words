import * as esbuild from 'esbuild'

await esbuild.build({
    entryPoints: ['./src/page/load_cards.ts'],
    bundle: true,
    outdir: 'public',
})