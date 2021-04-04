import { Balance, Type } from './balance';

test('negation', () => {
  const balance = new Balance(100.0, new Date(2020, 1, 3), Type.CONFIRMED);
  const negated = Balance.negated(balance);
  expect(negated.date).toEqual(new Date(2020, 1, 3));
  expect(negated.amount).toBe(-100.0);
  expect(negated.type).toBe(Type.CONFIRMED);
});

test('addition - same type', () => {
  const balance1 = new Balance(100.0, new Date(2020, 1, 3), Type.CONFIRMED);
  const balance2 = new Balance(200.0, new Date(2020, 2, 3), Type.CONFIRMED);
  const sum = Balance.add(balance1, balance2);
  expect(sum.date).toEqual(new Date(2020, 2, 3));
  expect(sum.amount).toBe(300.0);
  expect(sum.type).toBe(Type.CONFIRMED);
});

test('addition - different type', () => {
  const balance1 = new Balance(100.0, new Date(2020, 1, 3), Type.CONFIRMED);
  const balance2 = new Balance(200.0, new Date(2020, 0, 3), Type.PROJECTED);
  const sum = Balance.add(balance1, balance2);
  expect(sum.date).toEqual(new Date(2020, 1, 3));
  expect(sum.amount).toBe(300.0);
  expect(sum.type).toBe(Type.PROJECTED);
});

test('subtraction', () => {
  const balance1 = new Balance(100.0, new Date(2020, 1, 3), Type.CONFIRMED);
  const balance2 = new Balance(200.0, new Date(2020, 0, 3), Type.PROJECTED);
  const sum = Balance.subtract(balance1, balance2);
  expect(sum.date).toEqual(new Date(2020, 1, 3));
  expect(sum.amount).toBe(-100.0);
  expect(sum.type).toBe(Type.PROJECTED);
});
