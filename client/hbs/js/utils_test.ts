import { transformBudgetData } from './utils';

describe("transformBudgetData", function () {

  it("works on empty", function () {
    let months = [];
    let accountNameToAccount = {};
    let statements = {};
    let summaries = {};
    let data_view = transformBudgetData(
        months, accountNameToAccount, statements, summaries);

    expect(data_view).toEqual({
      months: [],
      rows: [],
      popupCells: []
    });
  });

  it("works on single", function () {
    let months = ["Sep2014"];
    let accountNameToAccount = {
      main: {
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
      }
    };
    let statements = {
      main: {
        Sep2014: {
          isClosed: true,
          addSub: 100,
          endBalance: {
            amount: 3000,
            type: 'PROJECTED',
          },
          isCovered: true,
          isProjectedCovered: true,
          hasProjectedTransfer: true,
          percentChange: 1.4,
          unaccounted: 500,
          transactions: []
        },
      }
    };
    let summaries = {
      main: {
        Sep2014: {
          isClosed: true,
          accounts: ["main"],
          addSub: 100,
          endBalance: {
            amount: 3000,
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
    let data_view = transformBudgetData(
        months, accountNameToAccount, statements, summaries);

    expect(data_view).toEqual({
      months: ["Sep2014"],
      rows: [],
      popupCells: []
    });
  });

});
