import { expect, test } from 'vitest'
import {extractWord} from "./wordExctract.ts";

test('extract word from sentence by position', () => {
    expect(extractWord(`Ik ben moe.`, 0)).toBe('Ik');
    expect(extractWord(`Ik ben moe.`, 1)).toBe('Ik');
    expect(extractWord(`Ik ben moe.`, 2)).toBe('Ik');
    expect(extractWord(`Ik ben moe.`, 3)).toBe('ben');
    expect(extractWord(`Ik ben moe.`, 7)).toBe('moe');
    expect(extractWord(`Ik ben moe.`, 10)).toBe('moe');
    expect(extractWord(`Ik ben moe.`, 11)).toBe(null);
})
