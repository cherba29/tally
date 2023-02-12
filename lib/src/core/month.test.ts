import { Month } from './month';

describe('Creation', () => {
  test('basic', () => {
    const month = new Month(2019, 11);
    expect(month.year).toBe(2019);
    expect(month.month).toBe(11);
  });

  test('bad month too big', () => {
    expect(() => new Month(2019, 12)).toThrow('Invalid value for month 12');
  });

  test('bad month too negative', () => {
    expect(() => new Month(2019, -1)).toThrow('Invalid value for month -1');
  });
});

describe('Ordering', () => {
  test('next month', () => {
    const month = new Month(2020, 1);
    const nextMonth = month.next();
    expect(nextMonth.year).toBe(2020);
    expect(nextMonth.month).toBe(2);
  });

  test('next month becomes next year', () => {
    const month = new Month(2019, 11);
    const nextMonth = month.next();
    expect(nextMonth.year).toBe(2020);
    expect(nextMonth.month).toBe(0);
  });

  test('prev month', () => {
    const month = new Month(2020, 2);
    const prevMonth = month.previous();
    expect(prevMonth.year).toBe(2020);
    expect(prevMonth.month).toBe(1);
  });

  test('prev month becomes prev year', () => {
    const month = new Month(2020, 0);
    const prevMonth = month.previous();
    expect(prevMonth.year).toBe(2019);
    expect(prevMonth.month).toBe(11);
  });

  test('is less - different year', () => {
    const monthA = new Month(2020, 2);
    const monthB = new Month(2019, 11);
    expect(monthB.isLess(monthA)).toBe(true);
    expect(monthA.isLess(monthB)).toBe(false);
  });

  test('is less - same year', () => {
    const monthA = new Month(2019, 11);
    const monthB = new Month(2019, 2);
    expect(monthB.isLess(monthA)).toBe(true);
    expect(monthA.isLess(monthB)).toBe(false);
  });

  test('is less - equal', () => {
    const monthA = new Month(2019, 11);
    const monthB = new Month(2019, 11);
    expect(monthB.isLess(monthA)).toBe(false);
    expect(monthA.isLess(monthB)).toBe(false);
  });

  test('compareTo - different year', () => {
    const monthA = new Month(2020, 2);
    const monthB = new Month(2019, 11);
    expect(monthB.compareTo(monthA)).toBeLessThan(0);
    expect(monthA.compareTo(monthB)).toBeGreaterThan(0);
  });

  test('compareTo - same year', () => {
    const monthA = new Month(2019, 11);
    const monthB = new Month(2019, 2);
    expect(monthB.compareTo(monthA)).toBeLessThan(0);
    expect(monthA.compareTo(monthB)).toBeGreaterThan(0);
  });

  test('compareTo - equal', () => {
    const monthA = new Month(2019, 11);
    const monthB = new Month(2019, 11);
    expect(monthB.compareTo(monthA)).toBe(0);
    expect(monthA.compareTo(monthB)).toBe(0);
  });
});

describe('distance', () => {
  test('same', () => {
    const monthA = new Month(2019, 11);
    const monthB = new Month(2019, 11);
    expect(monthB.distance(monthA)).toBe(0);
    expect(monthA.distance(monthB)).toBe(0);
  });

  test('one month apart', () => {
    const monthA = new Month(2019, 11);
    const monthB = new Month(2020, 0);
    expect(monthB.distance(monthA)).toBe(1);
    expect(monthA.distance(monthB)).toBe(-1);
  });

  test('one year apart', () => {
    const monthA = new Month(2019, 11);
    const monthB = new Month(2020, 11);
    expect(monthB.distance(monthA)).toBe(12);
    expect(monthA.distance(monthB)).toBe(-12);
  });
});

describe('Naming', () => {
  test('toString', () => {
    const month = new Month(2019, 11);
    expect(month.toString()).toBe('Dec2019');
  });

  test('fromString', () => {
    const month = Month.fromString('Dec2019');
    expect(month.year).toBe(2019);
    expect(month.month).toBe(11);
  });

  test('fromString incomplete', () => {
    expect(() => Month.fromString('Dec')).toThrow('Cant get month from small string "Dec"');
  });

  test('fromString bad name', () => {
    expect(() => Month.fromString('Sec2020')).toThrow('Cant find month for "Sec2020"');
  });

  test('fromString bad year', () => {
    expect(() => Month.fromString('Sep202A')).toThrow('Cant get year from "Sep202A"');
  });
});

describe('fromDate', () => {
  test('first month', () => {
    const month = Month.fromDate(new Date(Date.UTC(2020, 0, 1)));
    expect(month.year).toBe(2020);
    expect(month.month).toBe(1);
  });

  test('last month', () => {
    const month = Month.fromDate(new Date(Date.UTC(2020, 11, 31)));
    expect(month.year).toBe(2020);
    expect(month.month).toBe(11);
  });
});

test('Range generator', () => {
  const start = new Month(2019, 10);
  const dec = start.next();
  const jan = dec.next();
  const end = new Month(2020, 1);
  expect(Array.from(Month.generate(start, end))).toEqual([start, dec, jan]);
});
