import {
  Account,
  AccountType,
  Balance,
  BalanceType,
  BudgetBuilder,
  Month,
  TransferData
} from '@tally-lib';

import * as yaml from 'js-yaml';

interface BalanceData {
  grp?: string;
  date?: Date;
  camt?: number;
  pamt?: number;
}

interface TransferYamlData {
  grp?: string;
  date?: Date;
  camt?: number;
  pamt?: number;
  desc?: string;
}

interface YamlData {
  name?: string;
  desc?: string;
  number?: string;
  type?: string;
  opened_on?: string;
  closed_on?: string;
  owner?: string[];
  url?: string;
  phone?: string;
  address?: string;
  username?: string;
  pswd?: string;
  balances?: BalanceData[];
  transfers_to?: { [key: string]: TransferYamlData[] };
}

function lookupAccountType(type: string): AccountType | undefined {
  for (const [key, value] of Object.entries(AccountType)) {
    if (value == type) {
      return AccountType[key as keyof typeof AccountType];
    }
  }
  return undefined;
}

function makeBalance(data: BalanceData) {
  let amount = 0;
  let balanceType = BalanceType.UNKNOWN;
  if (data.camt !== undefined) {
    amount = Math.round(100 * data.camt);
    balanceType = BalanceType.CONFIRMED;
  } else if (data.pamt !== undefined) {
    amount = Math.round(100 * data.pamt);
    balanceType = BalanceType.PROJECTED;
  } else {
    throw new Error(
      `Balance ${JSON.stringify(data)} does not have date set type, expected camt or pamt entry.`
    );
  }
  if (!data.date) {
    throw new Error(`Balance ${JSON.stringify(data)} does not have date set.`);
  }
  return new Balance(amount, data.date, balanceType);
}

function processYamlData(budgetBuilder: BudgetBuilder, data: YamlData | undefined) {
  if (!data || !data.name) {
    // Ignore data which dont represent account.
    return;
  }
  if (!data.type) {
    throw new Error(`Type is not set for account '${data.name}'`);
  }
  const accountType = lookupAccountType(data.type);
  if (!accountType) {
    throw new Error(`Unknown type '${data.type}' for account '${data.name}'`);
  }
  const account = new Account({
    name: data.name,
    description: data.desc,
    type: accountType,
    number: data.number,
    openedOn: data.opened_on ? Month.fromString(data.opened_on) : undefined,
    closedOn: data.closed_on ? Month.fromString(data.closed_on) : undefined,
    owners: data.owner || [],
    url: data.url,
    phone: data.phone,
    address: data.address,
    userName: data.username,
    password: data.pswd
  });
  budgetBuilder.setAccount(account);
  if (data.balances) {
    for (const balanceData of data.balances) {
      if (!balanceData.grp) {
        throw Error(`Balance entry ${JSON.stringify(balanceData)} has no grp setting.`);
      }
      let month;
      try {
        month = Month.fromString(balanceData.grp);
      } catch (e) {
        const message = e instanceof Error ? e.message : 'unknown';
        throw new Error(`Balance ${JSON.stringify(balanceData)} has bad grp setting: ${message}`);
      }
      budgetBuilder.setBalance(account.name, month.toString(), makeBalance(balanceData));
    }
  }
  if (data.transfers_to) {
    for (const [account_name, transfers] of Object.entries(data.transfers_to)) {
      if (!transfers) continue;
      for (const transfer_data of transfers) {
        if (!transfer_data.grp) {
          throw new Error(
            `For account "${account.name} transfer to ${account_name}" does not have "grp" field.`
          );
        }
        if (!transfer_data.date) {
          throw new Error(
            `For account "${account.name}" transfer to "${account_name}" does not have "date" field.`
          );
        }
        let balance: Balance | undefined;
        if (transfer_data.pamt !== undefined) {
          balance = new Balance(
            Math.round(100 * transfer_data.pamt),
            transfer_data.date,
            BalanceType.PROJECTED
          );
        } else if (transfer_data.camt !== undefined) {
          balance = new Balance(
            Math.round(100 * transfer_data.camt),
            transfer_data.date,
            BalanceType.CONFIRMED
          );
        }
        if (balance === undefined) {
          throw new Error(
            `For account "${account.name}" transfer to "${account_name}" ` +
              `does not have "pamt" or "camt" field: ${JSON.stringify(transfer_data)}.`
          );
        }

        const transfer: TransferData = {
          fromAccount: account.name,
          fromMonth: Month.fromString(transfer_data.grp),
          toAccount: account_name,
          toMonth: Month.fromString(transfer_data.grp),
          balance,
          description: transfer_data.desc
        };
        budgetBuilder.addTransfer(transfer);
      }
    }
  }
}

export function loadYamlFile(
  budgetBuilder: BudgetBuilder,
  content: string,
  relative_file_path: string
): void {
  const accountData = yaml.safeLoad(content, {
    filename: relative_file_path
  }) as YamlData | undefined;
  try {
    processYamlData(budgetBuilder, accountData);
  } catch (e) {
    const message = ' while processing ' + relative_file_path;
    if (e instanceof Error) {
      e.message += message;
      console.error(e.name + ': ' + e.message);
    } else {
      console.error('Error' + message);
    }
    console.log('Account Data', accountData);
    throw e;
  }
}
