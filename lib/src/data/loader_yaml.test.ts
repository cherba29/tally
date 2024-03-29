/* eslint-disable  @typescript-eslint/no-non-null-assertion */
import { afterEach, describe, expect, jest, test } from '@jest/globals';
import { Type as AccountType } from '../core/account';
import { Balance, Type as BalanceType } from '../core/balance';
import { Month } from '../core/month';
import { BudgetBuilder } from '../core/budget';
import { loadYamlFile, parseYamlContent } from './loader_yaml';

describe('loadYaml', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  test('empty', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    loadYamlFile(budgetBuilder, parseYamlContent('some: 123', relativeFilePath)!, relativeFilePath);
    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(0);
  });

  test('fails when unknown account type', () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});

    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    expect(() =>
      loadYamlFile(
        budgetBuilder,
        parseYamlContent('name: test\ntype: SOMETHING', relativeFilePath)!,
        relativeFilePath
      )
    ).toThrow(
      new Error("Unknown type 'SOMETHING' for account 'test' while processing path/file.yaml")
    );
    expect(console.error).toHaveBeenCalledTimes(1);
    expect(console.error).toHaveBeenCalledWith(
      "Error: Unknown type 'SOMETHING' for account 'test' while processing path/file.yaml"
    );
    expect(console.log).toHaveBeenCalledTimes(1);
    expect(console.log).toHaveBeenCalledWith('Account Data', {
      name: 'test',
      type: 'SOMETHING',
    });
  });

  test('fails when account has no owners', () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});

    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    expect(() =>
      loadYamlFile(
        budgetBuilder,
        parseYamlContent('name: test\ntype: external\nowner: []', relativeFilePath)!,
        relativeFilePath
      )
    ).toThrow(new Error("Account 'test' has no owners while processing path/file.yaml"));
    expect(console.error).toHaveBeenCalledTimes(1);
    expect(console.error).toHaveBeenCalledWith(
      "Error: Account 'test' has no owners while processing path/file.yaml"
    );
    expect(console.log).toHaveBeenCalledTimes(1);
    expect(console.log).toHaveBeenCalledWith('Account Data', {
      name: 'test',
      owner: [],
      type: 'external',
    });
  });

  test('empty account', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    const content = `
      name: test-account
      desc: 'Testing account'
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
    const parsedContent = parseYamlContent(content, relativeFilePath);
    expect(parsedContent).toBeDefined();
    loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath);
    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(1);
    expect(budget.balances.size).toBe(0);
    expect(budget.months.length).toBe(5);
    expect(budget.transfers.size).toBe(0);

    const account = budget.accounts.get('test-account');
    expect(account).toEqual({
      name: 'test-account',
      description: 'Testing account',
      number: '1223344',
      type: AccountType.EXTERNAL,
      path: [],
      url: 'example.com',
      phone: '111-222-3344',
      address: '55 Road',
      userName: 'john',
      password: 'xxxyyy',
      owners: ['arthur'],
      openedOn: new Month(2019, 10),
      closedOn: new Month(2020, 2),
    });
  });

  test('account with balances', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/test.yaml';
    const content = `
      name: test-account
      owner: [ someone ]
      type: external
      balances:
      - { grp: Feb2020, date: 2020-02-01, pamt: 10.00 }
      - { grp: Jan2020, date: 2020-01-01, camt:  0.00 }
      `;
    const parsedContent = parseYamlContent(content, relativeFilePath);
    expect(parsedContent).toBeDefined();
    loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath);
    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(1);
    expect(budget.balances.size).toBe(1);
    expect(budget.months.length).toBe(2);
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
    const relativeFilePath = 'path/file.yaml';
    const content = `
      name: test-account
      owner: [ someone ]
      type: external
      balances:
      - { date: 2020-01-01, camt:  0.00 }
      `;
    const parsedContent = parseYamlContent(content, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        'Balance entry {"date":"2020-01-01T00:00:00.000Z","camt":0} has no grp setting. while processing path/file.yaml'
      )
    );
  });

  test('fails with bad balance month', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    const content = `
      name: test-account
      owner: [ someone ]
      type: external
      balances:
      - { grp: Xxx2020, date: 2020-01-01, camt:  0.00 }
      `;
    const parsedContent = parseYamlContent(content, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        'Balance {"grp":"Xxx2020","date":"2020-01-01T00:00:00.000Z","camt":0} has bad grp setting: Cant find month for "Xxx2020" while processing path/file.yaml'
      )
    );
  });

  test('fails without balance date', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    const content = `
      name: test-account
      owner: [ someone ]
      type: external
      balances:
      - { grp: Jan2020, camt:  0.00 }
      `;
    const parsedContent = parseYamlContent(content, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        'Balance {"grp":"Jan2020","camt":0} does not have date set. while processing path/file.yaml'
      )
    );
  });

  test('fails with bad balance date', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    const content = `
      name: test-account
      owner: [ someone ]
      type: external
      balances:
      - { grp: Jan2020, date: 20200101, camt:  0.00 }
      `;
    const parsedContent = parseYamlContent(content, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        'Balance {"grp":"Jan2020","date":20200101,"camt":0} does not have date set. while processing path/file.yaml'
      )
    );
  });

  test('fails without balance type', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    const content = `
      name: test-account
      owner: [ someone ]
      type: external
      balances:
      - { grp: Jan2020, date: 2020-01-01, xamt:  0.00 }
      `;
    const parsedContent = parseYamlContent(content, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        `Balance {"grp":"Jan2020","date":"2020-01-01T00:00:00.000Z","xamt":0} does not have amount type set, expected camt or pamt entry. while processing path/file.yaml`
      )
    );
  });

  test('with projected and confirmed transfers', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/file.yaml';
    const testAccountData = `
      name: test-account
      owner: [ someone ]
      type: external
      balances:
      - { grp: Feb2020, date: 2020-02-01, pamt: 10.00 }
      - { grp: Jan2020, date: 2020-01-01, camt:  0.00 }
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-01-17, pamt: 37.50 }
        - { grp: Jan2020, date: 2020-01-15, camt: -22.48 }
      `;
    const parsedContent = parseYamlContent(testAccountData, relativeFilePath);
    expect(parsedContent).toBeDefined();
    loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath);
    const externalAccountData = `
      name: external
      owner: [ someone ]
      type: external
      `;
    const parsedExternalContent = parseYamlContent(externalAccountData, relativeFilePath);
    expect(parsedExternalContent).toBeDefined();
    loadYamlFile(budgetBuilder, parsedExternalContent!, relativeFilePath);

    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(2);
    const testAccount = budget.accounts.get('test-account');
    const externalAccount = budget.accounts.get('external');
    expect(budget.balances.size).toBe(1);
    expect(budget.months.length).toBe(2);
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
          balance: new Balance(3750, new Date('2020-01-17'), BalanceType.PROJECTED),
        },
        {
          fromAccount: testAccount,
          toAccount: externalAccount,
          fromMonth: Month.fromString('Jan2020'),
          toMonth: Month.fromString('Jan2020'),
          balance: new Balance(-2248, new Date('2020-01-15'), BalanceType.CONFIRMED),
        },
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
          balance: new Balance(3750, new Date('2020-01-17'), BalanceType.PROJECTED),
        },
        {
          fromAccount: testAccount,
          toAccount: externalAccount,
          fromMonth: Month.fromString('Jan2020'),
          toMonth: Month.fromString('Jan2020'),
          balance: new Balance(-2248, new Date('2020-01-15'), BalanceType.CONFIRMED),
        },
      ])
    );
  });

  test('fails with transfer and no grp', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/test.yaml';
    const testAccountData = `
      name: test-account
      owner: [ someone ]
      type: external
      transfers_to:
        external:
        - { date: 2020-01-17, pamt: 37.50 }
      `;
    const parsedContent = parseYamlContent(testAccountData, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        'For account "test-account transfer to external" does not have "grp" field. while processing path/test.yaml'
      )
    );
  });

  test('fails with transfer and no date', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/test.yaml';
    const testAccountData = `
      name: test-account
      owner: [ someone ]
      type: external
      transfers_to:
        external:
        - { grp: Jan2020, pamt: 37.50 }
      `;
    const parsedContent = parseYamlContent(testAccountData, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        'For account "test-account" transfer to "external" does not have a ' +
          'valid "date" field. while processing path/test.yaml'
      )
    );
  });

  test('fails with transfer and too far apart dates', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/test.yaml';
    const testAccountData = `
      name: test-account
      owner: [ someone ]
      type: external
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-04-01, pamt: 37.50 }
      `;
    const parsedContent = parseYamlContent(testAccountData, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        'For account "test-account" transfer to "external" for Jan2020 date ' +
          '2020-04-01 (Apr2020) are too far apart. while processing path/test.yaml'
      )
    );
  });

  test('fails with transfer and no balance', () => {
    const budgetBuilder = new BudgetBuilder();
    const relativeFilePath = 'path/test.yaml';
    const testAccountData = `
      name: test-account
      owner: [ someone ]
      type: external
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-01-17 }
      `;
    const parsedContent = parseYamlContent(testAccountData, relativeFilePath);
    expect(parsedContent).toBeDefined();
    expect(() => loadYamlFile(budgetBuilder, parsedContent!, relativeFilePath)).toThrow(
      new Error(
        'For account "test-account" transfer to "external" does not have "pamt" or "camt" field:' +
          ' {"grp":"Jan2020","date":"2020-01-17T00:00:00.000Z"}. while processing path/test.yaml'
      )
    );
  });
});
