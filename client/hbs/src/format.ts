import {html, TemplateResult} from 'lit';
import {Type as BalanceType} from '@tally/lib/core/balance';
import {GqlBalance} from './gql_types';

export function dateFormat(value: string | Date | undefined | null): string {
  if (value === null || value === undefined) {
    return 'n/a';
  }
  if (typeof value === 'string') {
    return value;
  }
  return value.toISOString().slice(0, 10);
}

export function currency(value: number | undefined | null): TemplateResult {
  if (value === null || value === undefined) {
    return html`&mdash;`;
  }
  return html`${(value / 100.0).toFixed(2).replace(/(\d)(?=(\d\d\d)+(?!\d))/g, '$1,')}`;
}

export function isProjected(balance: GqlBalance | undefined | null): boolean {
  return (balance?.type ?? BalanceType.UNKNOWN) !== BalanceType.CONFIRMED;
}
