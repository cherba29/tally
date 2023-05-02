import { Account, AccountType, Month } from '@tally/lib';
import { buildSummaryStatementTable } from './summary';
import { TransactionStatement } from './transaction';

describe('Build', () => {
  test('empty', () => {
    const statements = Array.from(buildSummaryStatementTable([]));
    expect(statements).toEqual([]);
  });

  test('single closed account - produces summary without it', () => {
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.CHECKING,
      owners: ['john'],
      openedOn: Month.fromString('Jan2019'),
      closedOn: Month.fromString('Jan2020')
    });

    const stmt = new TransactionStatement(account1, Month.fromString('Mar2021'));
    const statements = Array.from(buildSummaryStatementTable([stmt]));
    expect(statements).toEqual([
      {
        startBalance: undefined,
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        name: 'john CHECKING',
        outFlows: 0,
        statements: [],
        totalPayments: 0,
        totalTransfers: 0
      },
      {
        startBalance: undefined,
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        name: 'john SUMMARY',
        outFlows: 0,
        statements: [],
        totalPayments: 0,
        totalTransfers: 0
      }
    ]);
  });

  test('single external account - no SUMMARY', () => {
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.EXTERNAL,
      owners: ['john'],
      openedOn: Month.fromString('Jan2019')
    });

    const stmt = new TransactionStatement(account1, Month.fromString('Mar2021'));
    const statements = Array.from(buildSummaryStatementTable([stmt]));
    expect(statements).toEqual([
      {
        startBalance: undefined,
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        name: 'john EXTERNAL',
        outFlows: 0,
        statements: [stmt],
        totalPayments: 0,
        totalTransfers: 0
      }
    ]);
  });

  test('single account - no transfers', () => {
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.CHECKING,
      owners: ['john'],
      openedOn: Month.fromString('Jan2021')
    });

    const stmt = new TransactionStatement(account1, Month.fromString('Mar2021'));
    const statements = Array.from(buildSummaryStatementTable([stmt]));
    expect(statements).toEqual([
      {
        startBalance: undefined,
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        name: 'john CHECKING',
        outFlows: 0,
        statements: [stmt],
        totalPayments: 0,
        totalTransfers: 0
      },
      {
        startBalance: undefined,
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        name: 'john SUMMARY',
        outFlows: 0,
        statements: [stmt],
        totalPayments: 0,
        totalTransfers: 0
      }
    ]);
  });
});
