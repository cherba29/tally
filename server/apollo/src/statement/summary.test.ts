import { Account, Type as AccountType } from '../core/account';
import { buildSummaryStatementTable } from './summary';
import { Month } from '../core/month';
import { TransactionStatement } from './transaction';

describe('Build', () => {
  test('empty', () => {
    const statements = Array.from(buildSummaryStatementTable([]));
    expect(statements).toEqual([]);
  });

  test('single closed account - not included', () => {
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.CHECKING,
      owners: ['john'],
      openedOn: Month.fromString('Jan2019'),
      closedOn: Month.fromString('Jan2020')
    });

    const stmt = new TransactionStatement(account1, Month.fromString('Mar2021'));
    const statements = Array.from(buildSummaryStatementTable([stmt]));
    expect(statements).toEqual([]);
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
        name: 'john external',
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
        name: 'john checking',
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
