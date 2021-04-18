import { Account, Type as AccountType } from '../core/account';
import { Balance, Type as BalanceType } from '../core/balance';
import { BudgetBuilder } from '../core/budget';
import { Month } from '../core/month';
import { buildTransactionStatementTable, Type as TransactionType } from './transaction';

describe('Build', () => {
  test('empty', () => {
    const builder = new BudgetBuilder();
    builder.setPeriod(Month.fromString('Nov2019'), Month.fromString('Feb2020'));
    const table = buildTransactionStatementTable(builder.build());
    expect(table).toEqual([]);
  });

  test('no months', () => {
    const builder = new BudgetBuilder();
    expect(() => buildTransactionStatementTable(builder.build())).toThrow(
      new Error('Budget must have at least one month.')
    );
  });

  test('single account no transfers', () => {
    const builder = new BudgetBuilder();
    builder.setPeriod(Month.fromString('Dec2019'), Month.fromString('Jan2020'));
    const account = new Account({
      name: 'test-account',
      type: AccountType.EXTERNAL,
      owners: []
    });
    builder.setAccount(account);
    const table = buildTransactionStatementTable(builder.build());
    expect(table).toEqual([
      {
        account,
        coversPrevious: false,
        coversProjectedPrevious: false,
        endBalance: undefined,
        hasProjectedTransfer: false,
        inFlows: 0,
        income: 0,
        isCovered: false,
        isProjectedCovered: false,
        month: new Month(2019, 11),
        name: 'test-account',
        outFlows: 0,
        startBalance: undefined,
        totalPayments: 0,
        totalTransfers: 0,
        transactions: []
      }
    ]);
    expect(table[0].isClosed).toBe(true);
  });

  test('two accounts with single transfers', () => {
    const builder = new BudgetBuilder();
    builder.setPeriod(Month.fromString('Dec2019'), Month.fromString('Jan2020'));
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.EXTERNAL,
      owners: []
    });
    const account2 = new Account({
      name: 'test-account2',
      type: AccountType.CREDIT,
      owners: []
    });

    builder.setAccount(account1);
    builder.setBalance(
      'test-account1',
      'Dec2019',
      new Balance(10, new Date('2019-12-01'), BalanceType.CONFIRMED)
    );
    builder.setBalance(
      'test-account1',
      'Jan2020',
      new Balance(20, new Date('2020-01-01'), BalanceType.CONFIRMED)
    );
    builder.setBalance(
      'test-account1',
      'Feb2020',
      new Balance(30, new Date('2020-02-01'), BalanceType.PROJECTED)
    );
    builder.setAccount(account2);
    const transferBalance = new Balance(2000, new Date('2019-12-05'), BalanceType.PROJECTED);
    builder.addTransfer({
      fromAccount: 'test-account1',
      toAccount: 'test-account2',
      toMonth: Month.fromString('Dec2019'),
      fromMonth: Month.fromString('Dec2019'),
      balance: transferBalance
    });

    const table = buildTransactionStatementTable(builder.build());
    expect(table).toEqual([
      {
        account: account1,
        coversPrevious: false,
        coversProjectedPrevious: false,
        endBalance: new Balance(20, new Date('2020-01-01'), BalanceType.CONFIRMED),
        hasProjectedTransfer: true,
        inFlows: 0,
        income: 0,
        isCovered: false,
        isProjectedCovered: false,
        month: new Month(2019, 11),
        name: 'test-account1',
        outFlows: -2000,
        startBalance: new Balance(10, new Date('2019-12-01'), BalanceType.CONFIRMED),
        totalPayments: -2000,
        totalTransfers: 0,
        transactions: [
          {
            account: account2,
            balance: Balance.negated(transferBalance),
            type: TransactionType.EXPENSE
          }
        ]
      },
      {
        account: account2,
        coversPrevious: false,
        coversProjectedPrevious: false,
        endBalance: undefined,
        hasProjectedTransfer: true,
        inFlows: 2000,
        income: 2000,
        isCovered: false,
        isProjectedCovered: false,
        month: new Month(2019, 11),
        name: 'test-account2',
        outFlows: 0,
        startBalance: undefined,
        totalPayments: 0,
        totalTransfers: 0,
        transactions: [
          {
            account: account1,
            balance: transferBalance,
            type: TransactionType.INCOME
          }
        ]
      }
    ]);
    expect(table[0].isClosed).toBe(true);
  });
});
