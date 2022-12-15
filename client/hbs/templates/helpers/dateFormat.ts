/**
 * Format date as YYYY-MM-DD.
 * @param {Date} value date.
 * @return {string} formatted string for date.
 */
export default function(value: Date): string {
  if (value === null || value === undefined) {
    return 'n/a';
  }
  return value.toISOString().slice(0, 10);
}
