import { describe, expect, test } from '@jest/globals';
import { Transfer } from './transfer';
import { Account, Type as AccountType } from './account';
import { Balance, Type as BalanceType } from './balance';
import { Month } from './month';

describe('Creation', () => {
  test('basic', () => {
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.CHECKING,
      owners: ['john'],
    });
    const account2 = new Account({
      name: 'test-account2',
      type: AccountType.CREDIT,
      owners: ['john'],
    });
    const month = new Month(2020, 1);
    const balance = new Balance(100, new Date(2020, 1, 3), BalanceType.CONFIRMED);
    const transfer = new Transfer(account1, account2, month, month, balance, "test");
    expect(transfer.fromAccount.name).toBe('test-account1');
    expect(transfer.toAccount.name).toBe('test-account2');
    expect(transfer.fromMonth.toString()).toBe('Feb2020');
    expect(transfer.toMonth.toString()).toBe('Feb2020');
    expect(transfer.balance.amount).toBe(100);
    expect(transfer.balance.date.toISOString().slice(0, 10)).toBe('2020-02-03');
    expect(transfer.description).toBe("test");
  });

  test('invalid transfer', () => {
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.CHECKING,
      owners: ['john'],
    });
    const account2 = new Account({
      name: 'test-account2',
      type: AccountType.CREDIT,
      owners: ['john'],
    });
    const month1 = new Month(2020, 1);
    const month2 = new Month(2020, 2);
    const balance = new Balance(100, new Date(2020, 1, 3), BalanceType.CONFIRMED);
    expect(() => new Transfer(account1, account2, month1, month2, balance, "test")).toThrow(new Error("Transfer to and from month must be the same"));
  });
});
