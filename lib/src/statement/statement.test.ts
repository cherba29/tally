import { describe, expect, test } from '@jest/globals';
import { Balance, Type as BalanceType } from '../core/balance';
import { Month } from '../core/month';
import { Statement } from './statement';
import { Account, Type as AccountType } from '../core/account';

class TestStatement extends Statement {
  get isClosed() {
    return true;
  }
}

function stmtToObj(stmt: Statement) {
  return {
    ...stmt,
    addSub: stmt.addSub,
    change: stmt.change,
    percentChange: stmt.percentChange,
    unaccounted: stmt.unaccounted,
    isClosed: stmt.isClosed,
  };
}

describe('Creation', () => {
  test('basic', () => {
    const stmt = new TestStatement(
        new Account({name:'test', type: AccountType.BILL, owners:[]}),
        Month.fromString('Mar2021')
    );
    expect(stmt).toEqual({
      account: new Account({name:'test', type: AccountType.BILL, owners:[]}),
      month: new Month(2021, 2),
      inFlows: 0,
      income: 0,
      outFlows: 0,
      totalPayments: 0,
      totalTransfers: 0,
    });
    expect(stmt.addSub).toBe(0);
    expect(stmt.change).toBeUndefined();
    expect(stmt.percentChange).toBeUndefined();
    expect(stmt.unaccounted).toBeUndefined();
    expect(stmt.isClosed).toBe(true);
  });

  test('with inFlow outFlow no start/end balance', () => {
    const stmt = new TestStatement(
        new Account({name:'test', type: AccountType.BILL, owners:[]}),
        Month.fromString('Mar2021')
    );
    stmt.addInFlow(100);
    stmt.addInFlow(-10);
    stmt.addOutFlow(-30);
    stmt.addOutFlow(10);
    expect(stmtToObj(stmt)).toStrictEqual({
      account: new Account({name:'test', type: AccountType.BILL, owners:[]}),
      month: new Month(2021, 2),
      addSub: 70,
      change: undefined,
      endBalance: undefined,
      inFlows: 110,
      income: 0,
      isClosed: true,
      outFlows: -40,
      percentChange: undefined,
      startBalance: undefined,
      totalPayments: 0,
      totalTransfers: 0,
      unaccounted: undefined,
    });
  });

  test('with inFlow outFlow with start/end balance', () => {
    const stmt = new TestStatement(
        new Account({name:'test', type: AccountType.BILL, owners:[]}),
        Month.fromString('Mar2021')
    );
    const startBalance = new Balance(1000, new Date('2020-01-01'), BalanceType.PROJECTED);
    const endBalance = new Balance(2000, new Date('2020-02-01'), BalanceType.PROJECTED);
    stmt.startBalance = startBalance;
    stmt.endBalance = endBalance;
    stmt.addInFlow(100);
    stmt.addInFlow(-10);
    stmt.addOutFlow(-30);
    stmt.addOutFlow(10);
    expect(stmtToObj(stmt)).toStrictEqual({
      account: new Account({name:'test', type: AccountType.BILL, owners:[]}),
      month: new Month(2021, 2),
      addSub: 70,
      change: 1000,
      endBalance,
      inFlows: 110,
      income: 0,
      isClosed: true,
      outFlows: -40,
      percentChange: 100,
      startBalance,
      totalPayments: 0,
      totalTransfers: 0,
      unaccounted: 930,
    });
  });

    test('with empty statement', () => {
        const stmt = new TestStatement(
            new Account({name:'test', type: AccountType.BILL, owners:[]}),
            Month.fromString('Mar2021')
        );
        expect(stmt.isEmpty()).toBe(true);
    });

    test('with no start/end balance', () => {
        const stmt = new TestStatement(
            new Account({name:'test', type: AccountType.BILL, owners:[]}),
            Month.fromString('Mar2021')
        );
        stmt.addInFlow(100);
        stmt.addInFlow(-10);
        stmt.addOutFlow(-30);
        stmt.addOutFlow(10);
        expect(stmt.addSub).toBe(70);
        expect(stmt.change).toBeUndefined();
        expect(stmt.percentChange).toBeUndefined();
        expect(stmt.unaccounted).toBeUndefined();
    });

    test('with inFlow outFlow with start balance', () => {
        const stmt = new TestStatement(
            new Account({name:'test', type: AccountType.BILL, owners:[]}),
            Month.fromString('Mar2021')
        );
        const startBalance = new Balance(1000, new Date('2020-01-01'), BalanceType.PROJECTED);
        stmt.startBalance = startBalance;
        stmt.addInFlow(100);
        stmt.addInFlow(-10);
        stmt.addOutFlow(-30);
        stmt.addOutFlow(10);
        expect(stmt.addSub).toBe(70);
        expect(stmt.change).toBeUndefined();
        expect(stmt.percentChange).toBeUndefined();
        expect(stmt.unaccounted).toBeUndefined();
    });

    test('with inFlow outFlow with end balance', () => {
        const stmt = new TestStatement(
            new Account({name:'test', type: AccountType.BILL, owners:[]}),
            Month.fromString('Mar2021')
        );
        const endBalance = new Balance(2000, new Date('2020-02-01'), BalanceType.PROJECTED);
        stmt.endBalance = endBalance;
        stmt.addInFlow(100);
        stmt.addInFlow(-10);
        stmt.addOutFlow(-30);
        stmt.addOutFlow(10);
        expect(stmt.addSub).toBe(70);
        expect(stmt.change).toBe(undefined);
        expect(stmt.percentChange).toBe(undefined);
        expect(stmt.unaccounted).toBe(undefined);
    });

    test('percentChange', () => {
        const stmt = new TestStatement(
            new Account({name:'test', type: AccountType.BILL, owners:[]}),
            Month.fromString('Mar2021')
        );
        const startBalance = new Balance(1000, new Date('2020-01-01'), BalanceType.PROJECTED);
        const endBalance = new Balance(2000, new Date('2020-02-01'), BalanceType.PROJECTED);
        stmt.startBalance = startBalance;
        stmt.endBalance = endBalance;
        stmt.addInFlow(100);
        stmt.addInFlow(-10);
        stmt.addOutFlow(-30);
        stmt.addOutFlow(10);
        expect(stmt.percentChange).toBe(100);
    });

    test('change', () => {
        const stmt = new TestStatement(
            new Account({name:'test', type: AccountType.BILL, owners:[]}),
            Month.fromString('Mar2021')
        );
        const startBalance = new Balance(1000, new Date('2020-01-01'), BalanceType.PROJECTED);
        const endBalance = new Balance(2000, new Date('2020-02-01'), BalanceType.PROJECTED);
        stmt.startBalance = startBalance;
        stmt.endBalance = endBalance;
        stmt.addInFlow(100);
        stmt.addInFlow(-10);
        stmt.addOutFlow(-30);
        stmt.addOutFlow(10);
        expect(stmt.change).toBe(1000);
    });
});
