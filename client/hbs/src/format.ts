import {Balance, Type as BalanceType} from '@tally/lib/core/balance';

export function dateFormat(value: Date | undefined | null): string {
  if (value === null || value === undefined) {
    return 'n/a';
  }
  return value.toISOString().slice(0, 10);
}

export function currency(value: number | undefined | null): string {
  if (value === null || value === undefined) {
    return 'n/a';
  }
  return (value / 100.0).toFixed(2).replace(/(\d)(?=(\d\d\d)+(?!\d))/g, '$1,');
}

export function isProjected(balance: Balance): boolean {
  return (balance?.type ?? BalanceType.UNKNOWN) !== BalanceType.CONFIRMED;
}
