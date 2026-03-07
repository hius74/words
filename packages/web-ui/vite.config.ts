import { defineConfig } from "vite";
import { resolve } from "path";

export default defineConfig({
    base: "./",
    build: {
        rollupOptions: {
            input: {
                main: resolve(__dirname, "reader.html"),
            }
        },
        outDir: "./dist",
        minify: false,
        sourcemap: false,
    }
});
