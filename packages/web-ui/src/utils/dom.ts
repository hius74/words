/**
 * Return DOM element by Id with type checks
 */
type ElementConstructor<T extends Element> = {
    new (...args: any[]): T;
};

export function getElementById<T extends Element>(
    id: string,
    ctor: ElementConstructor<T>
): T {
    const el = document.getElementById(id);

    if (!el) {
        throw new Error(`Element #${id} not found`);
    }

    if (!(el instanceof ctor)) {
        throw new Error(`Element #${id} is not of expected type`);
    }

    return el;
}
