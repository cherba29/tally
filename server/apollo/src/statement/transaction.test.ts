import { Account, Type as AccountType } from '../core/account';
import { Balance } from '../core/balance';
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
        isCovered: true,
        isProjectedCovered: true,
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

  test('bad account name on transfer', () => {
    const builder = new BudgetBuilder();
    builder.setPeriod(Month.fromString('Dec2019'), Month.fromString('Jan2020'));
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.CHECKING,
      owners: ['john']
    });
    builder.setAccount(account1);
    builder.addTransfer({
      fromAccount: 'test-account1',
      toAccount: 'test-account2',
      toMonth: Month.fromString('Dec2019'),
      fromMonth: Month.fromString('Dec2019'),
      balance: Balance.projected(2000, '2019-12-05'),
      description: 'First transfer'
    });
    expect(() => buildTransactionStatementTable(builder.build())).toThrow(
      new Error('Unknown account test-account2')
    );
  });

  test('two accounts with common owner and transfers', () => {
    const builder = new BudgetBuilder();
    builder.setPeriod(Month.fromString('Dec2019'), Month.fromString('Jan2020'));
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.CHECKING,
      owners: ['john']
    });
    const account2 = new Account({
      name: 'test-account2',
      type: AccountType.CREDIT,
      owners: ['john']
    });

    builder.setAccount(account1);
    builder.setBalance('test-account1', 'Dec2019', Balance.confirmed(10, '2019-12-01'));
    builder.setBalance('test-account1', 'Jan2020', Balance.confirmed(20, '2020-01-01'));
    builder.setBalance('test-account1', 'Feb2020', Balance.projected(30, '2020-02-01'));
    builder.setAccount(account2);
    builder.addTransfer({
      fromAccount: 'test-account1',
      toAccount: 'test-account2',
      toMonth: Month.fromString('Dec2019'),
      fromMonth: Month.fromString('Dec2019'),
      balance: Balance.projected(2000, '2019-12-05'),
      description: 'First transfer'
    });

    builder.addTransfer({
      fromAccount: 'test-account1',
      toAccount: 'test-account2',
      toMonth: Month.fromString('Dec2019'),
      fromMonth: Month.fromString('Dec2019'),
      balance: Balance.projected(1000, '2019-12-05'),
      description: 'Second transfer'
    });

    const table = buildTransactionStatementTable(builder.build());
    expect(table).toEqual([
      {
        account: account1,
        coversPrevious: false,
        coversProjectedPrevious: false,
        endBalance: Balance.confirmed(20, '2020-01-01'),
        hasProjectedTransfer: true,
        inFlows: 0,
        income: 0,
        isCovered: true,
        isProjectedCovered: true,
        month: new Month(2019, 11),
        name: 'test-account1',
        outFlows: -3000,
        startBalance: Balance.confirmed(10, '2019-12-01'),
        totalPayments: 0,
        totalTransfers: -3000,
        transactions: [
          {
            account: account2,
            balance: Balance.projected(-2000, '2019-12-05'),
            type: TransactionType.TRANSFER,
            description: 'First transfer',
            balanceFromEnd: 2020,
            balanceFromStart: -2990,
          },
          {
            account: account2,
            balance: Balance.projected(-1000, '2019-12-05'),
            type: TransactionType.TRANSFER,
            description: 'Second transfer',
            balanceFromEnd: 3020,
            balanceFromStart: -990,
          }
        ]
      },
      {
        account: account2,
        coversPrevious: false,
        coversProjectedPrevious: true,
        endBalance: undefined,
        hasProjectedTransfer: true,
        inFlows: 3000,
        income: 0,
        isCovered: true,
        isProjectedCovered: true,
        month: new Month(2019, 11),
        name: 'test-account2',
        outFlows: 0,
        startBalance: undefined,
        totalPayments: 0,
        totalTransfers: 3000,
        transactions: [
          {
            account: account1,
            balance: Balance.projected(2000, '2019-12-05'),
            type: TransactionType.TRANSFER,
            description: 'First transfer'
          },
          {
            account: account1,
            balance: Balance.projected(1000, '2019-12-05'),
            type: TransactionType.TRANSFER,
            description: 'Second transfer'
          }
        ]
      }
    ]);
    expect(table[0].isClosed).toBe(true);
  });

  test('two accounts with extenal transfer', () => {
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
      owners: ['john']
    });

    builder.setAccount(account1);
    builder.setBalance('test-account1', 'Dec2019', Balance.confirmed(10, '2019-12-01'));
    builder.setBalance('test-account1', 'Jan2020', Balance.confirmed(20, '2020-01-01'));
    builder.setBalance('test-account1', 'Feb2020', Balance.projected(30, '2020-02-01'));
    builder.setAccount(account2);
    builder.addTransfer({
      fromAccount: 'test-account1',
      toAccount: 'test-account2',
      toMonth: Month.fromString('Dec2019'),
      fromMonth: Month.fromString('Dec2019'),
      balance: Balance.projected(2000, '2019-12-05'),
      description: 'First transfer'
    });

    builder.addTransfer({
      fromAccount: 'test-account1',
      toAccount: 'test-account2',
      toMonth: Month.fromString('Dec2019'),
      fromMonth: Month.fromString('Dec2019'),
      balance: Balance.projected(1000, '2019-12-05'),
      description: 'Second transfer'
    });

    const table = buildTransactionStatementTable(builder.build());
    expect(table).toEqual([
      {
        account: account1,
        coversPrevious: false,
        coversProjectedPrevious: false,
        endBalance: Balance.confirmed(20, '2020-01-01'),
        hasProjectedTransfer: true,
        inFlows: 0,
        income: 0,
        isCovered: true,
        isProjectedCovered: true,
        month: new Month(2019, 11),
        name: 'test-account1',
        outFlows: -3000,
        startBalance: Balance.confirmed(10, '2019-12-01'),
        totalPayments: -3000,
        totalTransfers: 0,
        transactions: [
          {
            account: account2,
            balance: Balance.projected(-2000, '2019-12-05'),
            type: TransactionType.EXPENSE,
            description: 'First transfer',
            balanceFromEnd: 2020,
            balanceFromStart: -2990,
          },
          {
            account: account2,
            balance: Balance.projected(-1000, '2019-12-05'),
            type: TransactionType.EXPENSE,
            description: 'Second transfer',
            balanceFromEnd: 3020,
            balanceFromStart: -990,
          }
        ]
      },
      {
        account: account2,
        coversPrevious: false,
        coversProjectedPrevious: false,
        endBalance: undefined,
        hasProjectedTransfer: true,
        inFlows: 3000,
        income: 3000,
        isCovered: true,
        isProjectedCovered: true,
        month: new Month(2019, 11),
        name: 'test-account2',
        outFlows: 0,
        startBalance: undefined,
        totalPayments: 0,
        totalTransfers: 0,
        transactions: [
          {
            account: account1,
            balance: Balance.projected(2000, '2019-12-05'),
            type: TransactionType.INCOME,
            description: 'First transfer'
          },
          {
            account: account1,
            balance: Balance.projected(1000, '2019-12-05'),
            type: TransactionType.INCOME,
            description: 'Second transfer'
          }
        ]
      }
    ]);
    expect(table[0].isClosed).toBe(true);
  });
});
