import { inject } from 'vue';

export const LIBRARY_WORKSPACE_KEY = Symbol('libraryWorkspace');

export function useLibraryWorkspace() {
  const ctx = inject(LIBRARY_WORKSPACE_KEY, null);
  if (!ctx) {
    throw new Error('useLibraryWorkspace must be used within LibraryWorkspaceLayout');
  }
  return ctx;
}
