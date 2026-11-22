import {expect} from '@esm-bundle/chai';
import {Month} from './month';

describe('Month', () => {
  describe('Creation', () => {
    it('basic', () => {
      const month = new Month(2019, 11);
      expect(month.year).to.equal(2019);
      expect(month.month).to.equal(11);
    });

    it('bad month too big', () => {
      expect(() => new Month(2019, 12)).to.throw('Invalid value for month 12');
    });

    it('bad month too negative', () => {
      expect(() => new Month(2019, -1)).to.throw('Invalid value for month -1');
    });
  });

  describe('Ordering', () => {
    it('next month', () => {
      const month = new Month(2020, 1);
      const nextMonth = month.next();
      expect(nextMonth.year).to.equal(2020);
      expect(nextMonth.month).to.equal(2);
    });

    it('next month becomes next year', () => {
      const month = new Month(2019, 11);
      const nextMonth = month.next();
      expect(nextMonth.year).to.equal(2020);
      expect(nextMonth.month).to.equal(0);
    });

    it('prev month', () => {
      const month = new Month(2020, 2);
      const prevMonth = month.previous();
      expect(prevMonth.year).to.equal(2020);
      expect(prevMonth.month).to.equal(1);
    });

    it('prev month becomes prev year', () => {
      const month = new Month(2020, 0);
      const prevMonth = month.previous();
      expect(prevMonth.year).to.equal(2019);
      expect(prevMonth.month).to.equal(11);
    });

    it('is less - different year', () => {
      const monthA = new Month(2020, 2);
      const monthB = new Month(2019, 11);
      expect(monthB.isLess(monthA)).to.equal(true);
      expect(monthA.isLess(monthB)).to.equal(false);
    });

    it('is less - same year', () => {
      const monthA = new Month(2019, 11);
      const monthB = new Month(2019, 2);
      expect(monthB.isLess(monthA)).to.equal(true);
      expect(monthA.isLess(monthB)).to.equal(false);
    });

    it('is less - equal', () => {
      const monthA = new Month(2019, 11);
      const monthB = new Month(2019, 11);
      expect(monthB.isLess(monthA)).to.equal(false);
      expect(monthA.isLess(monthB)).to.equal(false);
    });

    it('compareTo - different year', () => {
      const monthA = new Month(2020, 2);
      const monthB = new Month(2019, 11);
      expect(monthB.compareTo(monthA)).to.be.below(0);
      expect(monthA.compareTo(monthB)).to.be.above(0);
    });

    it('compareTo - same year', () => {
      const monthA = new Month(2019, 11);
      const monthB = new Month(2019, 2);
      expect(monthB.compareTo(monthA)).to.be.below(0);
      expect(monthA.compareTo(monthB)).to.be.above(0);
    });

    it('compareTo - equal', () => {
      const monthA = new Month(2019, 11);
      const monthB = new Month(2019, 11);
      expect(monthB.compareTo(monthA)).to.equal(0);
      expect(monthA.compareTo(monthB)).to.equal(0);
    });

    it('isBetween - before', () => {
      const monthA = new Month(2019, 10);
      const monthB = new Month(2022, 10);
      expect(new Month(2018, 11).isBetween(monthA, monthB)).to.equal(false);
      expect(new Month(2019, 9).isBetween(monthA, monthB)).to.equal(false);
      expect(new Month(2019, 10).isBetween(monthA, monthB)).to.equal(true);
      expect(new Month(2020, 1).isBetween(undefined, monthA)).to.equal(false);
    });

    it('isBetween - after', () => {
      const monthA = new Month(2019, 10);
      const monthB = new Month(2022, 10);
      expect(new Month(2022, 10).isBetween(monthA, monthB)).to.equal(true);
      expect(new Month(2022, 11).isBetween(monthA, monthB)).to.equal(false);
      expect(new Month(2023, 1).isBetween(monthA, monthB)).to.equal(false);
    });
  });

  describe('distance', () => {
    it('same', () => {
      const monthA = new Month(2019, 11);
      const monthB = new Month(2019, 11);
      expect(monthB.distance(monthA)).to.equal(0);
      expect(monthA.distance(monthB)).to.equal(0);
    });

    it('one month apart', () => {
      const monthA = new Month(2019, 11);
      const monthB = new Month(2020, 0);
      expect(monthB.distance(monthA)).to.equal(1);
      expect(monthA.distance(monthB)).to.equal(-1);
    });

    it('one year apart', () => {
      const monthA = new Month(2019, 11);
      const monthB = new Month(2020, 11);
      expect(monthB.distance(monthA)).to.equal(12);
      expect(monthA.distance(monthB)).to.equal(-12);
    });
  });

  describe('Naming', () => {
    it('toString', () => {
      const month = new Month(2019, 11);
      expect(month.toString()).to.equal('Dec2019');
    });

    it('fromString', () => {
      const month = Month.fromString('Dec2019');
      expect(month.year).to.equal(2019);
      expect(month.month).to.equal(11);
    });

    it('fromString incomplete', () => {
      expect(() => Month.fromString('Dec')).to.throw('Cant get month from small string "Dec"');
    });

    it('fromString bad name', () => {
      expect(() => Month.fromString('Sec2020')).to.throw('Cant find month for "Sec2020"');
    });

    it('fromString bad year', () => {
      expect(() => Month.fromString('Sep202A')).to.throw('Cant get year from "Sep202A"');
    });
  });

  describe('fromDate', () => {
    it('first month', () => {
      const month = Month.fromDate(new Date(Date.UTC(2020, 0, 1)));
      expect(month.year).to.equal(2020);
      expect(month.month).to.equal(0);
    });

    it('last month', () => {
      const month = Month.fromDate(new Date(Date.UTC(2020, 11, 31)));
      expect(month.year).to.equal(2020);
      expect(month.month).to.equal(11);
    });
  });

  it('Range generator', () => {
    const start = new Month(2019, 10);
    const dec = start.next();
    const jan = dec.next();
    const end = new Month(2020, 1);
    expect(Array.from(Month.generate(start, end))).to.deep.equal([start, dec, jan]);
  });
});
