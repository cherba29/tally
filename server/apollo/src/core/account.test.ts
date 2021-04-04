import { Account, Type } from './account';
import { Month } from './month';

test('isClosed - by default', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['bob']
  });
  expect(account.isClosed(new Month(2021, 2))).toBe(true);
});

test('isClosed false if closedOn not set', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['bob'],
    openedOn: new Month(2021, 1)
  });
  expect(account.isClosed(new Month(2021, 2))).toBe(false);
});

test('isClosed true if closedOn is set', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['bob'],
    openedOn: new Month(2021, 1),
    closedOn: new Month(2021, 3)
  });
  expect(account.isClosed(new Month(2021, 4))).toBe(true);
});

test('isSummary false if type is not summary', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['bob']
  });
  expect(account.isSummary).toBe(false);
});

test('isSummary when type is summary', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.SUMMARY,
    owners: ['bob']
  });
  expect(account.isSummary).toBe(true);
});

test('has common owner is false if no common owners', () => {
  const account1 = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['bob']
  });
  const account2 = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['john']
  });

  expect(account1.hasCommonOwner(account2)).toBe(false);
});

test('has common owner is true if common owners', () => {
  const account1 = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['bob']
  });
  const account2 = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['john', 'bob']
  });

  expect(account1.hasCommonOwner(account2)).toBe(true);
});

test('isExternal false by default', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['bob']
  });
  expect(account.isExternal).toBe(false);
});

test('isExternal true when type is EXTERNAL', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.EXTERNAL,
    owners: ['bob']
  });
  expect(account.isExternal).toBe(true);
});

test('isExternal true when type is TAX', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.TAX,
    owners: ['bob']
  });
  expect(account.isExternal).toBe(true);
});

test('isExternal true when type is DEFFERED_INCOME', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.DEFERRED_INCOME,
    owners: ['bob']
  });
  expect(account.isExternal).toBe(true);
});
