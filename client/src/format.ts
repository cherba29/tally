import {html, type TemplateResult} from 'lit';
import {type GqlBalance} from './gql_types';

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
  return (balance?.type ?? "UNKNOWN") !== "CONFIRMED";
}

export function formatCadence(cadence: string | undefined | null): string {
  switch (cadence) {
    case 'WEEKLY':
      return 'Weekly';
    case 'BIWEEKLY':
      return 'Bi-weekly';
    case 'MONTHLY':
      return 'Monthly';
    case 'QUARTERLY':
      return 'Quarterly';
    case 'YEARLY':
      return 'Yearly';
    default:
      return 'Recurring';
  }
}

export function formatOrdinalDay(day: number): string {
  const mod100 = day % 100;
  if (mod100 >= 11 && mod100 <= 13) {
    return `${day}th`;
  }
  switch (day % 10) {
    case 1:
      return `${day}st`;
    case 2:
      return `${day}nd`;
    case 3:
      return `${day}rd`;
    default:
      return `${day}th`;
  }
}
