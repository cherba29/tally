import { Balance, Type as BalanceType } from '../core/balance';
import { Month } from '../core/month';
import { Statement } from './statement';

class TestStatement extends Statement {
  get isClosed() {
    return true;
  }
}


function stmtToObj(stmt:Statement) {
  return {
    ...stmt,
    addSub: stmt.addSub,
    change: stmt.change,
    percentChange: stmt.percentChange,
    unaccounted: stmt.unaccounted,
    isClosed: stmt.isClosed
  }
}

describe('Creation', () => {
  test('basic', () => {
    const stmt = new TestStatement('test-statement', Month.fromString('Mar2021'));
    expect(stmt).toEqual({
      name: 'test-statement',
      month: new Month(2021, 2),
      inFlows: 0,
      income: 0,
      outFlows: 0,
      totalPayments: 0,
      totalTransfers: 0
    });
    expect(stmt.addSub).toBe(0);
    expect(stmt.change).toBeUndefined();
    expect(stmt.percentChange).toBeUndefined();
    expect(stmt.unaccounted).toBeUndefined();
    expect(stmt.isClosed).toBe(true);
  });

  test('with inFlow outFlow no start/end balance', () => {
    const stmt = new TestStatement('test-statement', Month.fromString('Mar2021'));
    stmt.addInFlow(100);
    stmt.addInFlow(-10);
    stmt.addOutFlow(-30);
    stmt.addOutFlow(10);
    expect(stmtToObj(stmt)).toStrictEqual({
      name: 'test-statement',
      month: new Month(2021, 2),
      addSub: 70,
      change: undefined,
      inFlows: 110,
      income: 0,
      isClosed: true,
      outFlows: -40,
      percentChange: undefined,
      totalPayments: 0,
      totalTransfers: 0,
      unaccounted: undefined
    });
  });

  test('with inFlow outFlow with start/end balance', () => {
    const stmt = new TestStatement('test-statement', Month.fromString('Mar2021'));
    const startBalance = new Balance(1000, new Date('2020-01-01'), BalanceType.PROJECTED);
    const endBalance = new Balance(2000, new Date('2020-02-01'), BalanceType.PROJECTED);
    stmt.startBalance = startBalance;
    stmt.endBalance = endBalance;
    stmt.addInFlow(100);
    stmt.addInFlow(-10);
    stmt.addOutFlow(-30);
    stmt.addOutFlow(10);
    expect(stmtToObj(stmt)).toStrictEqual({
      name: 'test-statement',
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
      unaccounted: 930
    });
  });
});
