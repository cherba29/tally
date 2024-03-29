import type { Argv, CommandBuilder, CommandModule } from 'yargs';
import { loadBudget } from '@tally/lib/data/loader';
import { Account } from '@tally/lib/core/account';
import { Balance, Type as BalanceType } from '@tally/lib/core/balance';
import { Budget } from '@tally/lib/core/budget';
import { Month } from '@tally/lib/core/month';
import { Transfer } from '@tally/lib/core/transfer';

type GenerateOptions = {
  account: string;
  startMonth: Month;
  useTransfers: boolean;
  showTransfers: boolean;
};

const command: string = 'generate <account>';
const desc: string = 'Generate balances record based on transfers.';

const builder: CommandBuilder<unknown, GenerateOptions> = (yargs: Argv<unknown>) =>
  yargs
    .options({
      'start-month': { type: 'string', demandOption: true },
      'use-transfers': {
        type: 'boolean',
        default: false,
        description: 'Ignore balance entries and use existing transfer to compute them.',
      },
      'show-transfers': {
        type: 'boolean',
        default: false,
        description: 'Show existing transfers for debugging.',
      },
      'with-annual-flush': {
        type: 'boolean',
        default: false,
        description: 'Generate annual flush transfers. (Make sure to remove existing transfers)',
      },
    })
    .positional('account', { type: 'string', demandOption: true })
    .coerce('start-month', Month.fromString) as unknown as Argv<GenerateOptions>;

export const generate: CommandModule<unknown, GenerateOptions> = {
  command,
  describe: desc,
  builder,
  handler: async ({
    account,
    startMonth,
    useTransfers,
    showTransfers,
    withAnnualFlush,
  }): Promise<void> => {
    const budget: Budget = (await loadBudget()).budget;
    process.stdout.write(
      `Generating balances for ${account} starting from ${startMonth?.toString()}!\n`
    );
    const acct: Account | undefined = budget.accounts.get(account);
    if (!acct) {
      process.stderr.write(`The account "${account}" does not exist.\n`);
      process.exit(1);
    }
    const accountBalances: Map<string, Balance> = budget.balances.get(account) ?? new Map();
    const accountTransfers: Map<string, Set<Transfer>> = budget.transfers.get(account) ?? new Map();

    // Find minimum/maximum month used by this account.
    const accountMonths = [...accountBalances.keys(), ...accountTransfers.keys()].map((val) =>
      Month.fromString(val)
    );
    const minMonth = accountMonths.reduce((prev, current) =>
      prev.compareTo(current) < 0 ? prev : current
    );
    const maxMonth = accountMonths.reduce((prev, current) =>
      prev.compareTo(current) > 0 ? prev : current
    );
    if (startMonth.compareTo(minMonth) < 0) {
      startMonth = minMonth.previous();
    }
    if (startMonth.compareTo(maxMonth) > 0) {
      process.stderr.write(
        `The account "${account}" has no balances or transactions after ${maxMonth}.\n`
      );
      process.exit(1);
    }

    // Compute max padding for amounts to be right aligned.
    const ammountLengths: number[] = budget.months.map(
      (m) => `${budget.balances.get(account)?.get(m.toString())?.amount ?? 0}`.length
    );
    const padAmtLength = Math.max(...ammountLengths) + 2; // 1 extra leading space plus "."

    const lines: string[] = [];
    const flushLines: string[] = [];
    // For each month, compute running predicted balance and actual balance.
    let predictedBalance: Balance | undefined = undefined;
    for (
      let currentMonth = startMonth;
      currentMonth.compareTo(maxMonth) <= 0;
      currentMonth = currentMonth.next()
    ) {
      const recordedBalance = accountBalances.get(currentMonth.toString());
      let currentBalance: Balance | undefined = useTransfers
        ? predictedBalance ?? recordedBalance
        : recordedBalance ?? predictedBalance;
      if (!currentBalance) {
        predictedBalance = undefined;
        const prevTransfers = accountTransfers.get(currentMonth.previous().toString()) ?? new Set();
        lines.push(
          `  - { grp: ${currentMonth} } # has no balance and had ${prevTransfers.size} transfers.`
        );
        continue;
      }
      lines.push(printBalanceLine(currentMonth, currentBalance, padAmtLength));
      // If there is disaggreement print it as a comment.
      if (predictedBalance && predictedBalance.amount !== currentBalance.amount) {
        const predictedAmtValue = (predictedBalance.amount / 100).toFixed(2).padStart(padAmtLength);
        const diffAmtValue = ((currentBalance.amount - predictedBalance.amount) / 100)
          .toFixed(2)
          .padStart(padAmtLength);
        lines[lines.length - 1] += ` # predicted ${predictedAmtValue} unaccounted ${diffAmtValue}`;
      }
      const transfers: Set<Transfer> = accountTransfers.get(currentMonth.toString()) ?? new Set();
      if (showTransfers) {
        for (const transfer of transfers) {
          lines.push(
            `    ${transfer.toMonth} ${transfer.fromAccount.name} --> ${transfer.toAccount.name} ${transfer.balance}`
          );
        }
      }
      // Find first date of next month transcation, our predicated balance cannot be older.
      const nextTransfers = accountTransfers.get(currentMonth.next().toString()) ?? new Set();
      const minDateNextMonthTransfer: Date | undefined = nextTransfers.size
        ? [...nextTransfers]
            .map((t) => t.balance.date)
            .reduce((prev, curr) => (prev.getTime() < curr.getTime() ? prev : curr))
        : undefined;
      if (withAnnualFlush && currentMonth.month === 0) {
        const firstMonth = currentMonth;
        const amtValue = (currentBalance.amount / 100).toFixed(2).padStart(padAmtLength);
        flushLines.push(
          `    - { grp: ${firstMonth}, date: ${
            firstMonth.year
          }-01-02, camt: ${amtValue}, desc: "Total for ${currentMonth.year - 1}" }`
        );
        currentBalance = new Balance(0, currentBalance.date, currentBalance.type);
      }
      predictedBalance = nextBalance(
        account,
        currentBalance,
        transfers,
        minDateNextMonthTransfer,
        useTransfers ? BalanceType.CONFIRMED : BalanceType.PROJECTED
      );
    }

    lines.reverse();
    for (const line of lines) {
      process.stdout.write(line + '\n');
    }
    flushLines.reverse();
    for (const line of flushLines) {
      process.stdout.write(line + '\n');
    }
    process.exit(0);
  },
};

function printBalanceLine(month: Month, balance: Balance, padAmtLength: number) {
  const amtPrefix = balance.type === BalanceType.PROJECTED ? 'pamt' : 'camt';
  const amtValue = (balance.amount / 100).toFixed(2).padStart(padAmtLength);
  const dateValue = balance.date.toISOString().slice(0, 10);
  return `  - { grp: ${month}, date: ${dateValue}, ${amtPrefix}: ${amtValue} }`;
}

function nextBalance(
  accountName: string,
  startBalance: Balance,
  transfers: Set<Transfer>,
  minDateNextMonthTransfer: Date | undefined,
  balanceType: BalanceType
): Balance | undefined {
  //if (transfers.size === 0) { return undefined; }
  // The balance date is set to max transfer date,
  // but make sure it is not lower then next month start by default.
  const nextDate = new Date(
    Date.UTC(
      startBalance.date.getUTCFullYear(),
      startBalance.date.getUTCMonth() + 1,
      startBalance.date.getUTCDate()
    )
  );
  let balance = new Balance(startBalance.amount, nextDate, balanceType);
  for (const transfer of transfers) {
    if (transfer.toAccount.name === accountName) {
      balance = Balance.add(balance, transfer.balance);
    } else {
      balance = Balance.subtract(balance, transfer.balance);
    }
  }
  // Make sure next balance start does not exceed its minimum transfer date.
  if (minDateNextMonthTransfer && balance.date.getTime() > minDateNextMonthTransfer.getTime()) {
    return new Balance(balance.amount, minDateNextMonthTransfer, balance.type);
  }
  return balance;
}
