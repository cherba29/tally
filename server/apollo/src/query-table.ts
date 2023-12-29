import { Month } from '@tally/lib/core/month';
import { loadBudget } from '@tally/lib/data/loader';
import {
  GqlTable,
  GqlTableCell,
  GqlTableRow,
  QueryTableArgs,
} from './types';
import { Account } from '@tally/lib/core/account';
import { 
  toGqlAccount,
  toGqlTableCellFromStatement,
  toGqlTableCellFromTransactionStatement,
} from './to-gql';


export async function buildGqlTable(_: any, args: QueryTableArgs): Promise<GqlTable> {
  const startTimeMs: number = Date.now();
  const startMonth = args.startMonth.previous();
  const endMonth = args.endMonth.next();
  const payload = await loadBudget();
  const months = payload.budget.months
    .filter((m) => m.isLess(endMonth) && startMonth.isLess(m))
    .sort((a: Month, b: Month) => -a.compareTo(b));
  const activeAccounts = payload.budget.findActiveAccounts();
  const owners = [...new Set(activeAccounts.map((account) => account.owners).flat())].sort();
  const owner = args.owner || owners[0];

  const rows: GqlTableRow[] = [];
  // Insert Total summary row
  {
    const summaryMonthMap = payload.summaries.get(owner + ' SUMMARY');
    if (!summaryMonthMap) {
      throw new Error(`Not able to find total summary ${owner + ' SUMMARY'}`);
    }
    const cells: GqlTableCell[] = months.map(
      (month) => toGqlTableCellFromStatement(summaryMonthMap?.get(month.toString()))
    );
    rows.push({ title: owner, isTotal: true, cells });
  }

  // Group accounts per type
  const accounts = activeAccounts.filter((a) => a.owners.includes(owner));
  const accountTypesToAccounts = new Map<string, Account[]>();
  for (const account of accounts) {
    const accountTypeId = account.typeIdName;
    let accountsOfType = accountTypesToAccounts.get(accountTypeId);
    if (!accountsOfType) {
      accountsOfType = [account];
      accountTypesToAccounts.set(accountTypeId, accountsOfType);
    } else {
      accountsOfType.push(account);
    }
  }
  for (const accountType of [...accountTypesToAccounts.keys()].sort()) {
    rows.push({ title: accountType + ' accounts', isSpace: true, cells: [] });
    const groupedAccounts =
      accountTypesToAccounts.get(accountType)?.sort((a, b) => (a.name > b.name ? 1 : -1)) ?? [];
    for (const account of groupedAccounts) {
      const cells: GqlTableCell[] = [];
      let isClosed = true;
      for (const month of months) {
        const stmt = payload.statements.get(account.name)?.get(month.toString());
        isClosed = isClosed && (stmt?.isClosed ?? false);
        cells.push(toGqlTableCellFromTransactionStatement(stmt));
      }
      if (!isClosed) {
        // Dont add accounts which are closed over selected timeframe.
        rows.push({ title: account.name, account: toGqlAccount(account), isNormal: true, cells });
      }
    }
    // Summary for each account type.
    const summaryMonthMap = payload.summaries.get(owner + ' ' + accountType);
    const cells: GqlTableCell[] = [];
    let isClosed = true;
    for (const month of months) {
      const stmt = summaryMonthMap?.get(month.toString());
      isClosed = isClosed && (stmt?.isClosed ?? false);
      cells.push(toGqlTableCellFromStatement(stmt));
    }
    if (!isClosed) {
      // Dont add accounts which are closed over selected timeframe.
      rows.push({ title: accountType, isTotal: true, cells });
    }
  }
  console.log(`gql table ${args.endMonth}--${args.startMonth} in ${Date.now() - startTimeMs}ms`);
  return {
    currentOwner: owner,
    owners,
    months,
    rows
  };
}
