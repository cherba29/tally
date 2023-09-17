import { Account, Type as AccountType } from '../core/account';
import { Balance, Type as BalanceType } from '../core/balance';
import { Month } from '../core/month';
import { BudgetBuilder, TransferData } from '../core/budget';

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

export interface YamlData {
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

function isValidDate(date: any) {
  return date && Object.prototype.toString.call(date) === '[object Date]' && !isNaN(date);
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
      `Balance ${JSON.stringify(data)} does not have amount type set, expected camt or pamt entry.`
    );
  }
  if (!data.date || !isValidDate(data.date)) {
    throw new Error(`Balance ${JSON.stringify(data)} does not have date set.`);
  }
  return new Balance(amount, data.date, balanceType);
}

function processYamlData(budgetBuilder: BudgetBuilder, data: YamlData) {
  if (!data.name) {
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
  if (!data.owner || data.owner.length === 0) {
    throw new Error(`Account '${data.name}' has no owners`);
  }
  const account = new Account({
    name: data.name,
    description: data.desc,
    type: accountType,
    number: data.number,
    openedOn: data.opened_on ? Month.fromString(data.opened_on) : undefined,
    closedOn: data.closed_on ? Month.fromString(data.closed_on) : undefined,
    owners: data.owner,
    url: data.url,
    phone: data.phone,
    address: data.address,
    userName: data.username,
    password: data.pswd,
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
      const balance = makeBalance(balanceData);
      const balanceMonthDiff = Math.abs(
        balance.date.getUTCFullYear() * 12 +
          balance.date.getUTCMonth() -
          month.year * 12 -
          month.month
      );
      if (balanceMonthDiff > 2) {
        throw new Error(
          `For ${account.name} account ${balance} and month ${month} are ${balanceMonthDiff} months apart (2 max).`
        );
      }
      budgetBuilder.setBalance(account.name, month.toString(), balance);
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
        if (!transfer_data.date || !isValidDate(transfer_data.date)) {
          throw new Error(
            `For account "${account.name}" transfer to "${account_name}" does not have a valid "date" field.`
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

        const transferMonth = Month.fromString(transfer_data.grp);
        const balanceMonth = Month.fromDate(balance.date);
        if (Math.abs(balanceMonth.distance(transferMonth)) > 2) {
          throw new Error(
            `For account "${account.name}" transfer to "${account_name}" ` +
              `for ${transferMonth} date ${balance.date
                .toISOString()
                .slice(0, 10)} (${balanceMonth}) are too far apart.`
          );
        }
        const transfer: TransferData = {
          fromAccount: account.name,
          fromMonth: transferMonth,
          toAccount: account_name,
          toMonth: transferMonth,
          balance,
          description: transfer_data.desc,
        };

        budgetBuilder.addTransfer(transfer);
      }
    }
  }
}

export function parseYamlContent(content: string, relativeFilePath: string): YamlData | undefined {
  return yaml.load(content, {
    filename: relativeFilePath,
    onWarning: (ex: yaml.YAMLException) => {
      console.warn(ex);
    },
    schema: yaml.DEFAULT_SCHEMA,
    json: false,
  }) as YamlData | undefined;
}

export function loadYamlFile(
  budgetBuilder: BudgetBuilder,
  accountData: YamlData,
  relative_file_path: string
): void {
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
