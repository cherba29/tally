import { Account as TallyAccount, AccountType, Balance, BalanceType, Month } from '@tally-lib';
import { GqlBalance, GqlAccount } from '@backend/types';

export function gqlToAccount(gqlAccount: GqlAccount|null): TallyAccount|null {
  return new TallyAccount({
    name: gqlAccount.name,
    description: gqlAccount.description,
    openedOn: gqlAccount.openedOn ? Month.fromString(gqlAccount.openedOn) : undefined,
    closedOn: gqlAccount.closedOn ? Month.fromString(gqlAccount.closedOn) : undefined,
    owners: gqlAccount.owners ?? [],
    url: gqlAccount.url,
    type: gqlAccount.type as AccountType,
    address: gqlAccount.address,
    userName: gqlAccount.userName,
    number: gqlAccount.number,
    phone: gqlAccount.phone,
    password: gqlAccount.password,
  });
}

export function gqlToBalance(gqlBalance: GqlBalance|null): Balance|null {
  if (!gqlBalance) { return null; }
  return new Balance(gqlBalance.amount, new Date(gqlBalance.date), gqlBalance.type as BalanceType);
}
