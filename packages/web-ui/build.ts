import * as esbuild from 'esbuild'

await esbuild.build({
    entryPoints: ['./src/page/*.ts'],
    bundle: true,
    outdir: 'public',
    format: 'esm',
})