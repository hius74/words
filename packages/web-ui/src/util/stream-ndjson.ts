/**
 * From file upload
 * <code>
 *   inputElement.addEventListener("change", async (e) => {
 *   const file = (e.target as HTMLInputElement).files?.[0];
 *   if (!file) return;
 *
 *   await readNDJSON(file, {
 *     onObject: (obj) => {
 *       console.log("File object:", obj);
 *     }
 *   });
 * });
 * </code>
 *
 * From URL
 * <code>
 *   await readNDJSON("https://example.com/data.ndjson", {
 *   onObject: (obj) => {
 *     console.log("Fetched object:", obj);
 *   }
 * });
 * </code>
 *
 * With cancelation
 * <code>
 *     const controller = new AbortController();
 *
 * readNDJSON("https://example.com/data.ndjson", {
 *   signal: controller.signal,
 *   onObject: async (obj) => {
 *     console.log(obj);
 *   }
 * });
 *
 * // cancel later
 * controller.abort();
 * </code>
 */

export type NDJSONSource = string | File;

export type NDJSONOptions<T = any> = {
    onObject: (obj: T) => void | Promise<void>;
    onError?: (error: unknown, line: string) => void;
    signal?: AbortSignal; // optional cancel support
};

export async function readNDJSON<T = any>(source: NDJSONSource, options: NDJSONOptions<T>): Promise<void> {
    const { onObject, onError, signal } = options;

    const stream = await getStream(source, signal);

    const textStream = stream
        .pipeThrough(createTextDecoderStream())
        .pipeThrough(createNDJSONTransform<T>(onError));

    for await (const obj of textStream) {
        if (signal?.aborted) break;
        await onObject(obj);
    }
}

function createTextDecoderStream(): TransformStream<Uint8Array, string> {
    const decoder = new TextDecoder();

    return new TransformStream<Uint8Array, string>({
        transform(chunk, controller) {
            controller.enqueue(decoder.decode(chunk, { stream: true }));
        },
        flush(controller) {
            const remaining = decoder.decode();
            if (remaining) controller.enqueue(remaining);
        }
    });
}

async function getStream(
    source: NDJSONSource,
    signal?: AbortSignal
): Promise<ReadableStream<Uint8Array>> {
    if (typeof source === "string") {
        const response = await fetch(source, { signal });
        if (!response.body) {
            throw new Error("Streaming not supported in this browser.");
        }
        return response.body;
    }

    // File case
    return source.stream();
}

function createNDJSONTransform<T>(
    onError?: (error: unknown, line: string) => void
): TransformStream<string, T> {
    let buffer = "";

    return new TransformStream<string, T>({
        transform(chunk, controller) {
            buffer += chunk;

            const lines = buffer.split("\n");
            buffer = lines.pop() || "";

            for (const line of lines) {
                if (!line.trim()) continue;

                try {
                    controller.enqueue(JSON.parse(line));
                } catch (err) {
                    onError?.(err, line);
                }
            }
        },
        flush(controller) {
            if (buffer.trim()) {
                try {
                    controller.enqueue(JSON.parse(buffer));
                } catch (err) {
                    onError?.(err, buffer);
                }
            }
        }
    });
}
