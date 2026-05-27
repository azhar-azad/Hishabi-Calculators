import {describe, it, expect} from "vitest";

describe('vitest smoke test', () => {
    it('runs basic assertion', () => {
        expect(1 + 1).toBe(2);
    });
});