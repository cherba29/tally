import type { Arguments, Argv, CommandBuilder, CommandModule, Options, ArgumentsCamelCase, number } from 'yargs';
import { loadBudget } from '@tally/lib/data/loader';
import { Account } from '@tally/lib/core/account';
import { Balance, Type as BalanceType } from '@tally/lib/core/balance';
import { Budget } from '@tally/lib/core/budget';
import { Month } from '@tally/lib/core/month';
import { Transfer } from '@tally/lib/core/transfer';

type GenerateOptions = {
  account: string;
  startMonth: Month;
};

const command: string = 'generate <account>';
const desc: string = 'Generate balances record based on transfers.';

const builder: CommandBuilder<unknown, GenerateOptions> = (yargs: Argv<unknown>) =>
  yargs
    .options({
      'start-month': { type: 'string', demandOption: true },
    })
    .positional('account', { type: 'string', demandOption: true })
    .coerce('start-month', Month.fromString) as unknown as Argv<GenerateOptions>;

export const generate: CommandModule<unknown, GenerateOptions> = {
  command,
  describe: desc,
  builder,
  handler:  ({ account, startMonth }): void => {
    const budget: Budget = loadBudget();
    process.stdout.write(`Generating balances for ${account} starting from ${startMonth?.toString()}!\n`);
    const acct: Account|undefined = budget.accounts.get(account);
    if (!acct) {
      process.stderr.write(`The account "${account}" does not exist.\n`);
      process.exit(1);
    }
    if (acct.isClosed(startMonth)) {
      process.stderr.write(`The account "${account}" is closed as of ${startMonth} since ${acct.closedOn}.\n`);
      process.exit(1);
    }
    // Compute max padding for amounts to be right aligned.
    const ammountLengths: number[] = budget.months.map(
        m => `${budget.balances.get(account)?.get(m.toString())?.amount ?? 0}`.length);
    const padAmtLength = Math.max(...ammountLengths) + 2;  // 1 extra leading space plus "."

    let predictedBalance: Balance|undefined = undefined;
    for (let currentMonth = startMonth; ; currentMonth = currentMonth.next()) {
      const currentBalance: Balance|undefined = 
          budget.balances.get(account)?.get(currentMonth.toString()) ?? predictedBalance;
      if (!currentBalance) { break; }
      const amtPrefix = currentBalance.type === BalanceType.PROJECTED ? 'pamt' : 'camt';
      const amtValue = (currentBalance.amount/100).toFixed(2).padStart(padAmtLength);
      const dateValue = currentBalance.date.toISOString().slice(0, 10);
      process.stdout.write(`- { grp: ${currentMonth}, date: ${dateValue}, ${amtPrefix}: ${amtValue} }\n`);
      const transfers: Set<Transfer>|undefined = budget.transfers.get(account)?.get(currentMonth.toString());
      predictedBalance = nextBalance(account, currentBalance, transfers);
    }

    process.exit(0);
  }
}

function nextBalance(
  accountName: string, 
  startBalance: Balance, 
  transfers: Set<Transfer>|undefined
): Balance| undefined {
  if (!transfers || transfers.size === 0) { return undefined; }
  const nextDate = new Date(Date.UTC(
      startBalance.date.getUTCFullYear(),
      startBalance.date.getUTCMonth() + 1, 
      startBalance.date.getUTCDate()));
  let balance = new Balance(startBalance.amount, nextDate, BalanceType.PROJECTED);
  for (const transfer of transfers) {
    if (transfer.toAccount.name === accountName) {
      balance = Balance.add(balance, transfer.balance);
    } else {
      balance = Balance.subtract(balance, transfer.balance);
    }
  }
  return balance;
}