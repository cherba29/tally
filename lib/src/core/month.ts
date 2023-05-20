const MONTH_NAMES = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];
const MONTH_NAME_TO_INDEX = Object.fromEntries(MONTH_NAMES.map((x, i) => [x, i]));

/**
 * Represents a finincial month for accounting purposes.
 * Transcations from neighboring months can be attributed to it, for example
 * creadit card statement can close on 24th of the month, and for last week
 * of the month transactions will be attributed to next month.
 *
 * Month is represented as "MonthYear" string, for example "Mar2021".
 */
export class Month {
  constructor(readonly year: number, readonly month: number) {
    if (this.month < 0 || this.month > 11) {
      throw new Error(`Invalid value for month ${this.month}`);
    }
  }

  toString(): string {
    return `${MONTH_NAMES[this.month]}${this.year}`;
  }

  next(): Month {
    if (this.month >= 11) {
      return new Month(this.year + 1, 0);
    }
    return new Month(this.year, this.month + 1);
  }

  previous(): Month {
    if (this.month == 0) {
      return new Month(this.year - 1, 11);
    }
    return new Month(this.year, this.month - 1);
  }

  isLess(other: Month): boolean {
    if (this.year === other.year) {
      return this.month < other.month;
    }
    return this.year < other.year;
  }

  compareTo(other: Month): number {
    if (this.year === other.year) {
      return this.month - other.month;
    }
    return this.year - other.year;
  }

  /**
   * Number of months between two month dates.
   * Negative is provided date is larger
   */
  distance(other: Month): number {
    return (this.year - other.year) * 12 + (this.month - other.month);
  }

  /** Convert string representation to internal representation. */
  static fromString(name: string): Month {
    if (name.length < 4) {
      throw new Error(`Cant get month from small string "${name}"`);
    }
    const month = MONTH_NAME_TO_INDEX[name.substring(0, 3)];
    if (month === undefined) {
      throw new Error(`Cant find month for "${name}"`);
    }
    const year = Number(name.substring(3));
    if (isNaN(year)) {
      throw Error(`Cant get year from "${name}"`);
    }
    return new Month(year, month);
  }

  static fromDate(date: Date): Month {
    return new Month(date.getUTCFullYear(), date.getUTCMonth());
  }

  /** Creates generator spanning start and end (but not including) months. */
  static *generate(start: Month, end: Month): IterableIterator<Month> {
    let current = start;
    while (current.isLess(end)) {
      yield current;
      current = current.next();
    }
    return current;
  }
}
