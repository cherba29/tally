import { describe, expect, test } from '@jest/globals';
import { Account, Type as AccountType } from '../core/account';
import { Month } from '../core/month';
import { SummaryStatement, buildSummaryStatementTable } from './summary';
import { TransactionStatement } from './transaction';
import { Balance, Type as BalanceType } from '../core/balance';

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
      closedOn: Month.fromString('Jan2020'),
    });

    const stmt = new TransactionStatement(account1, Month.fromString('Mar2021'));
    stmt.startBalance = new Balance(100, new Date(2023, 11, 2), BalanceType.CONFIRMED);
    const statements: Map<string, Map<string, SummaryStatement>> = buildSummaryStatementTable([stmt]);
    expect(statements.get('john CHECKING')?.get('Mar2021')).toEqual(
      {
        account: new Account({name: 'john CHECKING', type: AccountType.SUMMARY, owners: ['john']}),
        startBalance: undefined,
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        startMonth: Month.fromString('Mar2021'),
        outFlows: 0,
        statements: [],
        totalPayments: 0,
        totalTransfers: 0,
      }
    );
    expect(statements.get('john SUMMARY')?.get('Mar2021')).toEqual(
      {
        account: new Account({name: 'john SUMMARY', type: AccountType.SUMMARY, owners: ['john']}),
        startBalance: undefined,
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        startMonth: Month.fromString('Mar2021'),
        outFlows: 0,
        statements: [],
        totalPayments: 0,
        totalTransfers: 0,
      },
    );
  });

  test('single external account - no SUMMARY', () => {
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.EXTERNAL,
      owners: ['john'],
      openedOn: Month.fromString('Jan2019'),
    });

    const stmt = new TransactionStatement(account1, Month.fromString('Mar2021'));
    stmt.startBalance = new Balance(100, new Date(2023, 11, 2), BalanceType.CONFIRMED);
    const statements: Map<string, Map<string, SummaryStatement>> = buildSummaryStatementTable([stmt]);
    expect(statements.get('john EXTERNAL')?.get('Mar2021')).toEqual(
      {
        account: new Account({name: 'john EXTERNAL', type: AccountType.SUMMARY, owners: ['john']}),
        startBalance: new Balance(100, new Date(2023, 11, 2), BalanceType.CONFIRMED),
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        startMonth: Month.fromString('Mar2021'),
        outFlows: 0,
        statements: [stmt],
        totalPayments: 0,
        totalTransfers: 0,
      },
    );
  });

  test('single account - no transfers', () => {
    const account1 = new Account({
      name: 'test-account1',
      type: AccountType.CHECKING,
      owners: ['john'],
      openedOn: Month.fromString('Jan2021'),
    });

    const stmt = new TransactionStatement(account1, Month.fromString('Mar2021'));
    stmt.startBalance = new Balance(100, new Date(2023, 11, 2), BalanceType.CONFIRMED);
    const statements: Map<string, Map<string, SummaryStatement>> = buildSummaryStatementTable([stmt]);
    expect(statements.get('john CHECKING')?.get('Mar2021')).toEqual(
      {
        account: new Account({name: 'john CHECKING', type: AccountType.SUMMARY, owners: ['john']}),
        startBalance: new Balance(100, new Date(2023, 11, 2), BalanceType.CONFIRMED),
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        startMonth: Month.fromString('Mar2021'),
        outFlows: 0,
        statements: [stmt],
        totalPayments: 0,
        totalTransfers: 0,
      },
    );
    expect(statements.get('john SUMMARY')?.get('Mar2021')).toEqual({
        account: new Account({name: 'john SUMMARY', type: AccountType.SUMMARY, owners: ['john']}),
        startBalance: new Balance(100, new Date(2023, 11, 2), BalanceType.CONFIRMED),
        endBalance: undefined,
        inFlows: 0,
        income: 0,
        month: Month.fromString('Mar2021'),
        startMonth: Month.fromString('Mar2021'),
        outFlows: 0,
        statements: [stmt],
        totalPayments: 0,
        totalTransfers: 0,
      },
    );
  });
});
