import {Account} from '../core/account';
import {Budget} from '../core/budget';

import { Balance, Type as BalanceType } from '../core/balance';
import {Month} from '../core/month';

import * as yaml from 'js-yaml';

interface BalanceData {
  grp: string;
  date: Date;
  camt?: number;
  pamt?: number;
}

interface YamlData {
  name: string
  balances: BalanceData[];
}


function makeBalance(data: BalanceData) {
  let amount = 0;
  let balanceType = BalanceType.UNKNOWN;
  if (data.camt !== undefined) {
    amount = data.camt;
    balanceType = BalanceType.CONFIRMED;
  } else if (data.pamt !== undefined) {
    amount = data.pamt;
    balanceType = BalanceType.PROJECTED;
  } else {
    throw new Error('Unable to determine balance type, expected camt or pamt entry.')
  }
  return new Balance(amount, data.date, balanceType);
}

function processYamlData(budget: Budget, data: YamlData) {
  const account = new Account(
    { name: data.name }
  );
  budget.setAccount(account);
  if (data.balances) {
    for (const balanceData of data.balances) {
      const month = Month.fromString(balanceData.grp);
      if (!month) { throw Error(`Balance ${balanceData} has no grp setting.`); }
      budget.setBalance(account.name, month.toString(), makeBalance(balanceData));
    }
  }
}

export function loadYamlFile(budget: Budget, content: string, relative_file_path: string) {
  const accountData = yaml.safeLoad(content, {
    filename: relative_file_path
  }) as YamlData;
  try {
    processYamlData(budget, accountData);
  }
  catch (e) {
    console.error(e.name + ': ' + e.message + ' in ' + relative_file_path);
    console.log('Account Data', accountData)
    e.message += ' while processing ' + relative_file_path;
    throw e
  }

}