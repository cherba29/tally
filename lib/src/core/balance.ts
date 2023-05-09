export enum Type {
  UNKNOWN = 'UNKNOWN',
  CONFIRMED = 'CONFIRMED',
  PROJECTED = 'PROJECTED',
  AUTO_PROJECTED = 'AUTO_PROJECTED'
}

const combineTypes = (t1: Type, t2: Type): Type => (t1 < t2 ? t2 : t1);

export class Balance {
  constructor(readonly amount: number, readonly date: Date, readonly type: Type) {}

  // Helper contructor.
  static confirmed(amount: number, date: string): Balance {
    return new Balance(amount, new Date(date), Type.CONFIRMED);
  }

  static projected(amount: number, date: string): Balance {
    return new Balance(amount, new Date(date), Type.PROJECTED);
  }

  static negated(balance: Balance): Balance {
    return new Balance(-balance.amount, balance.date, balance.type);
  }

  static add(balance1: Balance, balance2: Balance): Balance {
    const maxDate = balance1.date.getTime() < balance2.date.getTime() ? balance2.date : balance1.date;
    return new Balance(
      balance1.amount + balance2.amount,
      maxDate,
      combineTypes(balance1.type, balance2.type)
    );
  }

  static subtract(balance1: Balance, balance2: Balance): Balance {
    return Balance.add(balance1, new Balance(-balance2.amount, balance2.date, balance2.type));
  }

  compareTo(other: Balance): number {
    const dateDiff = this.date.getTime() - other.date.getTime();
    if (dateDiff !== 0) {
      return dateDiff;
    }
    const amountDiff = this.amount - other.amount;
    if (amountDiff !== 0) {
      return amountDiff;
    }
    return this.type.localeCompare(other.type);
  }

  toString(): string {
    return `Balance { amount: ${this.amount}, date: ${this.date
      .toISOString()
      .slice(0, 10)}, type: ${Type[this.type]} }`;
  }
}
