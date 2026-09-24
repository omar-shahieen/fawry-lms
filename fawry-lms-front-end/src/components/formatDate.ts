/** Shared localized date rendering (§8.2 — Intl.DateTimeFormat, browser-local zone). */
export function formatDate(value?: string | null): string {
  if (!value) return ''
  try {
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  } catch {
    return value
  }
}
