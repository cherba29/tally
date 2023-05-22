import {describe, expect, it} from '@jest/globals';
import {Balance, Type as BalanceType} from '@tally/lib/core/balance';
import {gqlToBalance} from './gql_utils';

describe('gqlToBalance', function () {
  it('returns null for null', function () {
    expect(gqlToBalance(null)).toBeUndefined();
  });
  it('returns null for null', function () {
    expect(gqlToBalance({amount: 100.0, date: '2022-01-02', type: 'CONFIRMED'})).toEqual(
      new Balance(100, new Date(Date.UTC(2022, 0, 2)), BalanceType.CONFIRMED)
    );
  });
});
