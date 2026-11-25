import {expect} from '@esm-bundle/chai';
import {html, render} from 'lit';
import {currency, dateFormat, formatCadence, formatOrdinalDay, isProjected} from './format';
import {type GqlBalance} from './gql_types';

const renderCurrency = (value: number | null | undefined): string => {
  const container = document.createElement('div');
  render(html`<span>${currency(value)}</span>`, container);
  return container.textContent ?? '';
};

describe('format utilities', () => {
  describe('dateFormat', () => {
    it('returns n/a for null and undefined', () => {
      expect(dateFormat(null)).to.equal('n/a');
      expect(dateFormat(undefined)).to.equal('n/a');
    });

    it('returns string values unchanged', () => {
      expect(dateFormat('2026-07-01')).to.equal('2026-07-01');
    });

    it('formats Date instances as YYYY-MM-DD', () => {
      expect(dateFormat(new Date('2021-03-05T12:00:00Z'))).to.equal('2021-03-05');
    });
  });

  describe('currency', () => {
    it('renders em-dash for null and undefined', () => {
      expect(renderCurrency(null)).to.equal('—');
      expect(renderCurrency(undefined)).to.equal('—');
    });

    it('formats zero, positive, and negative cent values with thousands separators', () => {
      expect(renderCurrency(0)).to.equal('0.00');
      expect(renderCurrency(123456)).to.equal('1,234.56');
      expect(renderCurrency(-123456789)).to.equal('-1,234,567.89');
    });
  });

  describe('isProjected', () => {
    it('returns true for null, undefined, missing type, or PROJECTED balances', () => {
      expect(isProjected(null)).to.equal(true);
      expect(isProjected(undefined)).to.equal(true);
      expect(isProjected({} as GqlBalance)).to.equal(true);
      expect(isProjected({type: 'PROJECTED'} as GqlBalance)).to.equal(true);
    });

    it('returns false for CONFIRMED balances', () => {
      expect(isProjected({type: 'CONFIRMED'} as GqlBalance)).to.equal(false);
    });
  });

  describe('formatCadence', () => {
    it('maps known cadences and falls back to Recurring', () => {
      expect(formatCadence('WEEKLY')).to.equal('Weekly');
      expect(formatCadence('BIWEEKLY')).to.equal('Bi-weekly');
      expect(formatCadence('MONTHLY')).to.equal('Monthly');
      expect(formatCadence('QUARTERLY')).to.equal('Quarterly');
      expect(formatCadence('YEARLY')).to.equal('Yearly');
      expect(formatCadence('IRREGULAR')).to.equal('Recurring');
      expect(formatCadence(null)).to.equal('Recurring');
    });
  });

  describe('formatOrdinalDay', () => {
    it('formats 1st, 2nd, 3rd, teen, and regular ordinals', () => {
      expect(formatOrdinalDay(1)).to.equal('1st');
      expect(formatOrdinalDay(2)).to.equal('2nd');
      expect(formatOrdinalDay(3)).to.equal('3rd');
      expect(formatOrdinalDay(4)).to.equal('4th');
      expect(formatOrdinalDay(11)).to.equal('11th');
      expect(formatOrdinalDay(12)).to.equal('12th');
      expect(formatOrdinalDay(13)).to.equal('13th');
      expect(formatOrdinalDay(21)).to.equal('21st');
      expect(formatOrdinalDay(22)).to.equal('22nd');
      expect(formatOrdinalDay(23)).to.equal('23rd');
      expect(formatOrdinalDay(31)).to.equal('31st');
    });
  });
});
