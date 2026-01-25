import { ref } from 'vue';

interface RunOptions {
  loading?: boolean;
  errorMessage?: string;
  logError?: boolean;
  onError?: (error: unknown) => string;
}

export function useAsyncAction(defaultErrorMessage?: string) {
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  const run = async <T>(action: () => Promise<T>, options: RunOptions = {}): Promise<T | null> => {
    const { loading = true, errorMessage, logError = true, onError } = options;
    if (loading) {
      isLoading.value = true;
    }
    error.value = null;

    try {
      return await action();
    } catch (err) {
      const message = onError?.(err) ?? errorMessage ?? defaultErrorMessage ?? 'Unexpected error';
      error.value = message;
      if (logError) {
        console.error(err);
      }
      return null;
    } finally {
      if (loading) {
        isLoading.value = false;
      }
    }
  };

  const clearError = () => {
    error.value = null;
  };

  return {
    isLoading,
    error,
    run,
    clearError,
  };
}
