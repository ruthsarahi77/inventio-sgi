import { useCallback, useRef, useState } from "react";
import { useFocusEffect } from "expo-router";

// Refresh on focus and cancel obsolete responses, including account changes/unmount.
export function useApiResource<T>(load: (signal: AbortSignal) => Promise<T>) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const current = useRef<AbortController | null>(null);
  const reload = useCallback(async () => {
    current.current?.abort();
    const controller = new AbortController();
    current.current = controller;
    setLoading(true); setError(null); setData(null);
    try {
      const result = await load(controller.signal);
      if (!controller.signal.aborted) setData(result);
    } catch (cause) {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : "No se pudieron cargar los datos.");
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  }, [load]);
  useFocusEffect(useCallback(() => {
    void reload();
    return () => current.current?.abort();
  }, [reload]));
  return { data, loading, error, reload };
}
