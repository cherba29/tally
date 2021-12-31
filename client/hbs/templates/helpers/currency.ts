/**
 * Format number as currency.
 * @param {number} value number of cents.
 * @return {string} formatted string for currency.
 */
export default function(value: number): string {
  if (value === null || value === undefined) {
    return 'n/a';
  }
  return (value / 100.0).toFixed(2).replace(/(\d)(?=(\d\d\d)+(?!\d))/g, '$1,');
}
