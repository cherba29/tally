import { Statement, SummaryStatement, TallyAccount } from './base';
import { transformBudgetData } from './utils';

describe("transformBudgetData", function () {

  it("works on empty", function () {
    const months: string[] = [];
    const accountNameToAccount = {};
    const statements = {};
    const summaries = {};
    const data_view = transformBudgetData(
        months, accountNameToAccount, statements, summaries);

    expect(data_view).toEqual({
      months: [],
      rows: [],
      popupCells: []
    });
  });

  it("works on single", function () {
    const months = ["Sep2014"];
    const account: TallyAccount = {
      name: "main",
      address: "some place",
      closedOn: null,
      description: "Main account",
      external: false,
      number: "123",
      openedOn: "Aug2014",
      owners: ["owner1", "owner2"],
      password: "password",
      phone: "123-456-7890",
      summary: false,
      type: "credit",
      url: "http://somewhere",
      userName: "user1"
    };
    const accountNameToAccount: {[accountName:string]: TallyAccount} = { main: account };
    const statement: Statement =  {
      isClosed: true,
      addSub: 100,
      inFlows: 150,
      outFlows: 50,
      income: 0,
      totalPayments: 0,
      totalTransfers: 0,
      startBalance: {
        amount: 2900,
        date: new Date('2020-04-03'),
        type: 'PROJECTED',
      },
      endBalance: {
        amount: 3000,
        date: new Date('2020-04-30'),
        type: 'PROJECTED',
      },
      isCovered: true,
      isProjectedCovered: true,
      hasProjectedTransfer: true,
      percentChange: 1.4,
      unaccounted: 500,
      transactions: []
    };
    const statements: {[accountName:string]: {[month:string]: Statement}} = {
      main: { Sep2014: statement }
    };
    const summaries: {[ownerAccountType:string]: {[month:string]: SummaryStatement}} = {
      'owner1 credit': {
        Sep2014: {
          isClosed: true,
          accounts: ["main"],
          addSub: 100,
          inFlows: 150,
          outFlows: 50,
          income: 0,
          totalPayments: 0,
          totalTransfers: 0,
          startBalance: {
            amount: 2900,
            date: new Date('2020-04-03'),
            type: 'PROJECTED',
          },
          endBalance: {
            amount: 3000,
            date: new Date('2020-04-30'),
            type: 'PROJECTED',
          },
          isCovered: true,
          isProjectedCovered: true,
          hasProjectedTransfer: true,
          percentChange: 1.4,
          unaccounted: 200,
          transactions: []
        }
      },
      'owner2 credit': {
        Sep2014: {
          isClosed: true,
          accounts: ["main"],
          addSub: 100,
          inFlows: 150,
          outFlows: 50,
          income: 0,
          totalPayments: 0,
          totalTransfers: 0,
          startBalance: {
            amount: 2900,
            date: new Date('2020-04-30'),
            type: 'PROJECTED',
          },
          endBalance: {
            amount: 3000,
            date: new Date('2020-04-03'),
            type: 'PROJECTED',
          },
          isCovered: true,
          isProjectedCovered: true,
          hasProjectedTransfer: true,
          percentChange: 1.4,
          unaccounted: 200,
          transactions: []
        }
      }
    };
    const data_view = transformBudgetData(
        months, accountNameToAccount, statements, summaries);

    expect(data_view).toMatchSnapshot();
  });

});
