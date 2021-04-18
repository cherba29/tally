export enum Type {
  UNKNOWN,
  CONFIRMED,
  PROJECTED,
  AUTO_PROJECTED
}

const combineTypes = (t1: Type, t2: Type): Type => (t1 < t2 ? t2 : t1);

export class Balance {
  constructor(readonly amount: number, readonly date: Date, readonly type: Type) {}

  static negated(balance: Balance): Balance {
    return new Balance(-balance.amount, balance.date, balance.type);
  }

  static add(balance1: Balance, balance2: Balance): Balance {
    const maxDate = balance1.date < balance2.date ? balance2.date : balance1.date;
    return new Balance(
      balance1.amount + balance2.amount,
      maxDate,
      combineTypes(balance1.type, balance2.type)
    );
  }

  static subtract(balance1: Balance, balance2: Balance): Balance {
    return Balance.add(balance1, new Balance(-balance2.amount, balance2.date, balance2.type));
  }

  toString(): string {
    return `Balance { amount: ${this.amount}, date: ${this.date
      .toISOString()
      .slice(0, 10)}, type: ${Type[this.type]} }`;
  }
}
