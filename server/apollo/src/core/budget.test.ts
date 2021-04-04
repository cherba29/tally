import { Account, Type as AccountType } from './account';
import { Balance, Type as BalanceType } from './balance';
import { BudgetBuilder } from './budget';
import { Month } from './month';

test('build empty budget', () => {
  const builder = new BudgetBuilder();
  const budget = builder.build();
  expect(budget.accounts.size).toBe(0);
  expect(budget.balances.size).toBe(0);
  expect(budget.months.length).toBe(0);
  expect(budget.transfers.size).toBe(0);
});

test('build simple', () => {
  const builder = new BudgetBuilder();
  const account1 = new Account({
    name: 'test-account1',
    type: AccountType.EXTERNAL,
    owners: []
  });
  const account2 = new Account({
    name: 'test-account2',
    type: AccountType.BILL,
    owners: []
  });
  const account3 = new Account({
    name: 'test-account3',
    type: AccountType.BILL,
    owners: []
  });

  builder.setAccount(account1);
  builder.setAccount(account2);
  builder.setAccount(account3);
  builder.setPeriod(new Month(2019, 10), new Month(2020, 2));
  builder.setBalance(
    'test-account1',
    'Nov2019',
    new Balance(100, new Date(2019, 10, 1), BalanceType.PROJECTED)
  );
  builder.setBalance(
    'test-account1',
    'Dec2019',
    new Balance(200, new Date(2019, 11, 1), BalanceType.PROJECTED)
  );
  builder.setBalance(
    'test-account2',
    'Nov2019',
    new Balance(200, new Date(2019, 10, 3), BalanceType.CONFIRMED)
  );
  builder.addTransfer({
    toAccount: 'test-account1',
    toMonth: new Month(2019, 10),
    fromAccount: 'test-account2',
    fromMonth: new Month(2019, 10),
    balance: new Balance(50, new Date(2019, 10, 2), BalanceType.CONFIRMED)
  });
  builder.addTransfer({
    toAccount: 'test-account3',
    toMonth: new Month(2019, 10),
    fromAccount: 'test-account2',
    fromMonth: new Month(2019, 10),
    balance: new Balance(70, new Date(2019, 10, 2), BalanceType.CONFIRMED)
  });

  const budget = builder.build();
  expect(budget.accounts.size).toBe(3);
  expect(builder.accounts.get('test-account1')).toEqual(account1);
  expect(builder.accounts.get('test-account2')).toEqual(account2);
  expect(budget.balances.size).toBe(2);
  expect(budget.months).toEqual(['Nov2019', 'Dec2019', 'Jan2020', 'Feb2020'].map(Month.fromString));
  expect(budget.transfers.size).toBe(3);
});

test('build budget - bad to account', () => {
  const builder = new BudgetBuilder();
  builder.addTransfer({
    toAccount: 'test-account1',
    toMonth: new Month(2019, 10),
    fromAccount: 'test-account2',
    fromMonth: new Month(2019, 10),
    balance: new Balance(50, new Date(2019, 11, 2), BalanceType.CONFIRMED)
  });
  expect(() => builder.build()).toThrow('Unknown account test-account1');
});

test('build budget - bad from account', () => {
  const builder = new BudgetBuilder();
  const account1 = new Account({
    name: 'test-account1',
    type: AccountType.EXTERNAL,
    owners: []
  });
  builder.setAccount(account1);
  builder.addTransfer({
    toAccount: 'test-account1',
    toMonth: new Month(2019, 10),
    fromAccount: 'test-account2',
    fromMonth: new Month(2019, 10),
    balance: new Balance(50, new Date(2019, 11, 2), BalanceType.CONFIRMED)
  });
  expect(() => builder.build()).toThrow('Unknown account test-account2');
});
