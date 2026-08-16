// Shared styling for native <select> elements used across the app's forms.
// Mirrors the Input primitive's DESIGN.md treatment (18px radius, #f5f5f5
// resting fill, hairline focus ring) so every form's dropdown looks identical
// instead of each screen re-declaring its own copy of this class string.
export const selectFieldClassName =
  'flex h-10 w-full rounded-input border border-input bg-muted px-2.5 py-2 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring'
