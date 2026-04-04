import eslint from '@eslint/js';
import { defineConfig } from 'eslint/config';
import tseslint from 'typescript-eslint';

export default defineConfig(
    eslint.configs.recommended,
    tseslint.configs.strict,
    tseslint.configs.stylistic,
    {
        rules: {
            "@/no-restricted-syntax": [
                "error",
                { selector: "TSEnumDeclaration", message: "No enums" },
                { selector: "TSModuleDeclaration", message: "No namespaces" }
            ]
        }
    }
);