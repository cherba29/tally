/* eslint-disable  @typescript-eslint/no-non-null-assertion */
import { Type as AccountType } from '../core/account';
import { Balance, Type as BalanceType } from '../core/balance';
import { Month } from '../core/month';
import { BudgetBuilder } from '../core/budget';
import { loadYamlFile } from './loader_yaml';

describe('loadYaml', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  test('empty', () => {
    const budgetBuilder = new BudgetBuilder();
    loadYamlFile(budgetBuilder, /*content=*/ '', /*relative_file_path=*/ 'path/file.yaml');
    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(0);
  });

  test('fails when no account type', () => {
    jest.spyOn(console, 'error').mockImplementation();
    jest.spyOn(console, 'log').mockImplementation();

    const budgetBuilder = new BudgetBuilder();
    expect(() =>
      loadYamlFile(
        budgetBuilder,
        /*content=*/ 'name: test',
        /*relative_file_path=*/ 'path/file.yaml'
      )
    ).toThrow(new Error("Type is not set for account 'test' while processing path/file.yaml"));
    expect(console.error).toHaveBeenCalledTimes(1);
    expect(console.error).toHaveBeenCalledWith(
      "Error: Type is not set for account 'test' while processing path/file.yaml"
    );
    expect(console.log).toHaveBeenCalledTimes(1);
    expect(console.log).toHaveBeenCalledWith('Account Data', { name: 'test' });
  });

  test('fails when unknown account type', () => {
    jest.spyOn(console, 'error').mockImplementation();
    jest.spyOn(console, 'log').mockImplementation();

    const budgetBuilder = new BudgetBuilder();
    expect(() =>
      loadYamlFile(
        budgetBuilder,
        /*content=*/ 'name: test\ntype: SOMETHING',
        /*relative_file_path=*/ 'path/file.yaml'
      )
    ).toThrow(
      new Error("Unknown type 'SOMETHING' for account 'test' while processing path/file.yaml")
    );
    expect(console.error).toHaveBeenCalledTimes(1);
    expect(console.error).toHaveBeenCalledWith(
      "Error: Unknown type 'SOMETHING' for account 'test' while processing path/file.yaml"
    );
    expect(console.log).toHaveBeenCalledTimes(1);
    expect(console.log).toHaveBeenCalledWith('Account Data', { name: 'test', type: 'SOMETHING' });
  });

  test('empty account', () => {
    const budgetBuilder = new BudgetBuilder();
    const content = `
      name: test-account
      description: 'Testing account'
      number: '1223344'
      owner: [ arthur ]
      opened_on: Nov2019
      closed_on: Mar2020
      type: external
      url: 'example.com'
      phone: '111-222-3344'
      address: '55 Road'
      username: 'john'
      pswd: 'xxxyyy'
      transfers_to:
        external:
      `;
    loadYamlFile(budgetBuilder, content, /*relative_file_path=*/ 'path/file.yaml');
    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(1);
    expect(budget.balances.size).toBe(0);
    expect(budget.months.length).toBe(0);
    expect(budget.transfers.size).toBe(0);

    const account = budget.accounts.get('test-account');
    expect(account).toEqual({
      name: 'test-account',
      description: 'Testing account',
      number: '1223344',
      type: AccountType.EXTERNAL,
      url: 'example.com',
      phone: '111-222-3344',
      address: '55 Road',
      userName: 'john',
      password: 'xxxyyy',
      owners: ['arthur'],
      openedOn: new Month(2019, 10),
      closedOn: new Month(2020, 2)
    });
  });

  test('account with balances', () => {
    const budgetBuilder = new BudgetBuilder();
    const content = `
      name: test-account
      type: external
      balances:
      - { grp: Feb2020, date: 2020-02-01, pamt: 10.00 }
      - { grp: Jan2020, date: 2020-01-01, camt:  0.00 }
      `;
    loadYamlFile(budgetBuilder, content, /*relative_file_path=*/ 'path/file.yaml');
    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(1);
    expect(budget.balances.size).toBe(1);
    expect(budget.months.length).toBe(0);
    expect(budget.transfers.size).toBe(0);

    const balances = budget.balances.get('test-account')!;
    expect(balances.size).toBe(2);
    expect(balances.get('Jan2020')).toEqual(
      new Balance(0, new Date('2020-01-01'), BalanceType.CONFIRMED)
    );
    expect(balances.get('Feb2020')).toEqual(
      new Balance(1000, new Date('2020-02-01'), BalanceType.PROJECTED)
    );
  });

  test('fails without balance month', () => {
    const budgetBuilder = new BudgetBuilder();
    const content = `
      name: test-account
      type: external
      balances:
      - { date: 2020-01-01, camt:  0.00 }
      `;
    expect(() =>
      loadYamlFile(budgetBuilder, content, /*relative_file_path=*/ 'path/file.yaml')
    ).toThrow(
      new Error(
        'Balance entry {"date":"2020-01-01T00:00:00.000Z","camt":0} has no grp setting. while processing path/file.yaml'
      )
    );
  });

  test('fails with bad balance month', () => {
    const budgetBuilder = new BudgetBuilder();
    const content = `
      name: test-account
      type: external
      balances:
      - { grp: Xxx2020, date: 2020-01-01, camt:  0.00 }
      `;
    expect(() =>
      loadYamlFile(budgetBuilder, content, /*relative_file_path=*/ 'path/file.yaml')
    ).toThrow(
      new Error(
        'Balance {"grp":"Xxx2020","date":"2020-01-01T00:00:00.000Z","camt":0} has bad grp setting: Cant find month for "Xxx2020" while processing path/file.yaml'
      )
    );
  });

  test('fails without balance date', () => {
    const budgetBuilder = new BudgetBuilder();
    const content = `
      name: test-account
      type: external
      balances:
      - { grp: Jan2020, camt:  0.00 }
      `;
    expect(() =>
      loadYamlFile(budgetBuilder, content, /*relative_file_path=*/ 'path/file.yaml')
    ).toThrow(
      new Error(
        'Balance {"grp":"Jan2020","camt":0} does not have date set. while processing path/file.yaml'
      )
    );
  });

  test('fails without balance type', () => {
    const budgetBuilder = new BudgetBuilder();
    const content = `
      name: test-account
      type: external
      balances:
      - { grp: Jan2020, date: 2020-01-01, xamt:  0.00 }
      `;
    expect(() =>
      loadYamlFile(budgetBuilder, content, /*relative_file_path=*/ 'path/file.yaml')
    ).toThrow(
      new Error(
        `Balance {"grp":"Jan2020","date":"2020-01-01T00:00:00.000Z","xamt":0} does not have date set type, expected camt or pamt entry. while processing path/file.yaml`
      )
    );
  });

  test('with projected and confirmed transfers', () => {
    const budgetBuilder = new BudgetBuilder();
    const testAccountData = `
      name: test-account
      type: external
      balances:
      - { grp: Feb2020, date: 2020-02-01, pamt: 10.00 }
      - { grp: Jan2020, date: 2020-01-01, camt:  0.00 }
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-01-17, pamt: 37.50 }
        - { grp: Jan2020, date: 2020-01-15, camt: -22.48 }
      `;
    loadYamlFile(budgetBuilder, testAccountData, /*relative_file_path=*/ 'path/test.yaml');
    const externalAccountData = `
      name: external
      type: external
      `;
    loadYamlFile(budgetBuilder, externalAccountData, /*relative_file_path=*/ 'path/external.yaml');

    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(2);
    const testAccount = budget.accounts.get('test-account');
    const externalAccount = budget.accounts.get('external');
    expect(budget.balances.size).toBe(1);
    expect(budget.months.length).toBe(0);
    expect(budget.transfers.size).toBe(2);

    const testAccountTransfers = budget.transfers.get('test-account')!;
    expect(testAccountTransfers.size).toBe(1);
    const testAccountMonthTransfers = testAccountTransfers.get('Jan2020');
    expect(testAccountMonthTransfers).toEqual(
      new Set([
        {
          fromAccount: testAccount,
          toAccount: externalAccount,
          fromMonth: Month.fromString('Jan2020'),
          toMonth: Month.fromString('Jan2020'),
          balance: new Balance(3750, new Date('2020-01-17'), BalanceType.PROJECTED)
        },
        {
          fromAccount: testAccount,
          toAccount: externalAccount,
          fromMonth: Month.fromString('Jan2020'),
          toMonth: Month.fromString('Jan2020'),
          balance: new Balance(-2248, new Date('2020-01-15'), BalanceType.CONFIRMED)
        }
      ])
    );

    const externalAccountTransfers = budget.transfers.get('external')!;
    expect(externalAccountTransfers.size).toBe(1);
    const externalAccountMonthTransfers = externalAccountTransfers.get('Jan2020');
    expect(externalAccountMonthTransfers).toEqual(
      new Set([
        {
          fromAccount: testAccount,
          toAccount: externalAccount,
          fromMonth: Month.fromString('Jan2020'),
          toMonth: Month.fromString('Jan2020'),
          balance: new Balance(3750, new Date('2020-01-17'), BalanceType.PROJECTED)
        },
        {
          fromAccount: testAccount,
          toAccount: externalAccount,
          fromMonth: Month.fromString('Jan2020'),
          toMonth: Month.fromString('Jan2020'),
          balance: new Balance(-2248, new Date('2020-01-15'), BalanceType.CONFIRMED)
        }
      ])
    );
  });

  test('fails with transfer and no grp', () => {
    const budgetBuilder = new BudgetBuilder();
    const testAccountData = `
      name: test-account
      type: external
      transfers_to:
        external:
        - { date: 2020-01-17, pamt: 37.50 }
      `;
    expect(() =>
      loadYamlFile(budgetBuilder, testAccountData, /*relative_file_path=*/ 'path/test.yaml')
    ).toThrow(
      new Error(
        'For account "test-account transfer to external" does not have "grp" field. while processing path/test.yaml'
      )
    );
  });

  test('fails with transfer and no date', () => {
    const budgetBuilder = new BudgetBuilder();
    const testAccountData = `
      name: test-account
      type: external
      transfers_to:
        external:
        - { grp: Jan2020, pamt: 37.50 }
      `;
    expect(() =>
      loadYamlFile(budgetBuilder, testAccountData, /*relative_file_path=*/ 'path/test.yaml')
    ).toThrow(
      new Error(
        'For account "test-account" transfer to "external" does not have "date" field. while processing path/test.yaml'
      )
    );
  });

  test('fails with transfer and no balance', () => {
    const budgetBuilder = new BudgetBuilder();
    const testAccountData = `
      name: test-account
      type: external
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-01-17 }
      `;
    expect(() =>
      loadYamlFile(budgetBuilder, testAccountData, /*relative_file_path=*/ 'path/test.yaml')
    ).toThrow(
      new Error(
        'For account "test-account" transfer to "external" does not have "pamt" or "camt" field:' +
          ' {"grp":"Jan2020","date":"2020-01-17T00:00:00.000Z"}. while processing path/test.yaml'
      )
    );
  });
});
