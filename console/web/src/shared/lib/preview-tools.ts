import { useEffect, useRef } from "react";

type PreviewSnapshot = {
  mode: "simulation";
  jobState: string;
  total: number;
  completed: number;
  selected: string | null;
};
type Tool = {
  name: string;
  description: string;
  inputSchema: object;
  annotations: { readOnlyHint: boolean };
  execute: (input: unknown) => unknown;
};
type Context = {
  registerTool: (
    tool: Tool,
    options: { signal: AbortSignal },
  ) => void | Promise<void>;
};

// Optional read-only WebMCP bridge. No tools can send motion commands.
export function usePreviewTools(snapshot: PreviewSnapshot) {
  const current = useRef(snapshot);
  useEffect(() => {
    current.current = snapshot;
  }, [snapshot]);
  useEffect(() => {
    const context = (document as Document & { modelContext?: Context })
      .modelContext;
    if (!context?.registerTool) return;
    const lifecycle = new AbortController();
    try {
      void Promise.resolve(
        context.registerTool(
          {
            name: "get_smt_preview_state",
            description:
              "Read the local SMT interface simulation state. This is not real machine telemetry.",
            inputSchema: {
              type: "object",
              properties: {},
              additionalProperties: false,
            },
            annotations: { readOnlyHint: true },
            execute(input) {
              if (
                !input ||
                typeof input !== "object" ||
                Array.isArray(input) ||
                Object.keys(input).length
              )
                throw new Error("Expected an empty object");
              return { ...current.current };
            },
          },
          { signal: lifecycle.signal },
        ),
      ).catch((error) => console.warn("Preview tool unavailable", error));
    } catch (error) {
      console.warn("Preview tool unavailable", error);
    }
    return () => lifecycle.abort();
  }, []);
}
