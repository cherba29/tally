import { transformBudgetData } from './utils';
import { expect } from 'chai';
import 'mocha';

describe("transformBudgetData", function () {

  it("works on empty", function () {
    let months = [];
    let accountNameToAccount = {};
    let statements = {};
    let summaries = {};
    let data_view = transformBudgetData(
        months, accountNameToAccount, statements, summaries);

    expect(data_view).to.deep.equal({
      months: [],
      rows: [],
      popupCells: []
    });
  });

  it("works on single", function () {
    let months = ["Sep2014", "Oct2014"];
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
    let statements = {};
    let summaries = {};
    let data_view = transformBudgetData(
        months, accountNameToAccount, statements, summaries);

    expect(data_view).to.deep.equal({
      months: ["Sep2014"],
      rows: [],
      popupCells: []
    });
  });

});
