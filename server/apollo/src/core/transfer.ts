import { Account } from "./account";
import { Balance } from "./balance";
import { Month } from "./month";

export interface Transfer {
  fromAccount: Account;
  toAccount: Account;
  fromMonth: Month;
  toMonth: Month;
  description?: string;
  balance: Balance;
}