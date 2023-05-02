import {Account as TallyAccount, AccountType, Balance, BalanceType, Month} from '@tally/lib';
import {Statement, SummaryStatement} from './base';
import {transformBudgetData} from './utils';

describe('transformBudgetData', function() {
  it('works on empty', function() {
    const months: string[] = [];
    const accountNameToAccount = {};
    const statements = {};
    const summaries = {};
    const dataView = transformBudgetData(months, accountNameToAccount, statements, summaries);

    expect(dataView).toEqual({
      months: [],
      rows: [],
      popupCells: [],
    });
  });

  it('works on single', function() {
    const months = ['Sep2014'];
    const account = new TallyAccount({
      name: 'main',
      address: 'some place',
      closedOn: undefined,
      description: 'Main account',
      number: '123',
      openedOn: new Month(2014, 7),
      owners: ['owner1', 'owner2'],
      password: 'password',
      phone: '123-456-7890',
      type: AccountType.CREDIT,
      url: 'http://somewhere',
      userName: 'user1',
    });
    const accountNameToAccount: {[accountName: string]: TallyAccount} = {
      main: account,
    };
    const statement: Statement = {
      isClosed: true,
      addSub: 100,
      inFlows: 150,
      outFlows: 50,
      income: 0,
      totalPayments: 0,
      totalTransfers: 0,
      startBalance: new Balance(2900, new Date('2020-04-03'), BalanceType.PROJECTED),
      endBalance: new Balance(3000, new Date('2020-04-30'), BalanceType.PROJECTED),
      isCovered: true,
      isProjectedCovered: true,
      hasProjectedTransfer: true,
      percentChange: 1.4,
      unaccounted: 500,
      transactions: [],
    };
    const statements: {[accountName: string]: {[month: string]: Statement}} = {
      main: {Sep2014: statement},
    };
    const summaries: {
      [ownerAccountType: string]: {[month: string]: SummaryStatement};
    } = {
      'owner1 credit': {
        Sep2014: {
          isClosed: true,
          accounts: ['main'],
          addSub: 100,
          inFlows: 150,
          outFlows: 50,
          income: 0,
          totalPayments: 0,
          totalTransfers: 0,
          startBalance: new Balance(2900, new Date('2020-04-03'), BalanceType.PROJECTED),
          endBalance: new Balance(3000, new Date('2020-04-30'), BalanceType.PROJECTED),
          isCovered: true,
          isProjectedCovered: true,
          hasProjectedTransfer: true,
          percentChange: 1.4,
          unaccounted: 200,
          transactions: [],
        },
      },
      'owner2 credit': {
        Sep2014: {
          isClosed: true,
          accounts: ['main'],
          addSub: 100,
          inFlows: 150,
          outFlows: 50,
          income: 0,
          totalPayments: 0,
          totalTransfers: 0,
          startBalance: new Balance(2900, new Date('2020-04-30'), BalanceType.PROJECTED),
          endBalance: new Balance(3000, new Date('2020-04-03'), BalanceType.PROJECTED),
          isCovered: true,
          isProjectedCovered: true,
          hasProjectedTransfer: true,
          percentChange: 1.4,
          unaccounted: 200,
          transactions: [],
        },
      },
    };
    const dataView = transformBudgetData(months, accountNameToAccount, statements, summaries);

    expect(dataView).toMatchSnapshot();
  });
});
