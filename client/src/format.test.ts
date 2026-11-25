import {expect} from '@esm-bundle/chai';
import {html, render} from 'lit';
import {currency, dateFormat, isProjected} from './format';
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
});
