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

interface RowEntry {
  title: string;
  id: string;
  isTotal: boolean;
  account?: Account;
  depth: number;
}

/** Sequence for presentation summaries and accounts based on their names. */
function sequenceStatements(owner: string, accounts: Account[]): RowEntry[] {
  // Build a tree based on paths.
  const tree = new Map<string, Set<string>>();
  for (const account of accounts) {
    if (!account.path.length) continue;
    const path = account.path;
    let entry = '/' + [...path,  account.name].join('/');
    for (let sub = path.length; sub >= 0 ; sub--) {
      const subPath = path.slice(0, sub);
      const subPathId = '/' + subPath.join('/');
      let subTreeEntry = tree.get(subPathId);
      if (!subTreeEntry) {
        subTreeEntry = new Set<string>();
        tree.set('/' + subPath.join('/'), subTreeEntry);
      }
      subTreeEntry.add(entry);
      entry = subPathId;
    }
  }
  // Iterate over tree in depth-first fashion to sequence rows representing the tree.
  const nameToAccount = new Map<string, Account>();
  for (const account of accounts) {
    nameToAccount.set(account.name, account);
  }
  const entries: RowEntry[] = [];
  const nodesToProcess = ['/'];
  while (nodesToProcess.length) {
    const subTreeId = nodesToProcess.shift()!;
    const children = tree.get(subTreeId);
    const subPath = subTreeId.split('/').filter(v=>!!v);
    entries.push({
      title: subPath.length ? subPath[subPath.length-1] : owner,
      id: subTreeId,
      account: children ? undefined : nameToAccount.get(subPath[subPath.length-1]),
      isTotal: !!children,
      depth: subPath.length,
    });
    if (children) { 
      // console.log('### adding', children);
      // Add in front as we want to process children next, in dept-first fashion.
      nodesToProcess.unshift(...children); 
    }
  }
  return entries;
}

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
  const accounts = activeAccounts.filter((a) => a.owners.includes(owner));

  const rows: GqlTableRow[] = [];
  
  const ordering = sequenceStatements(owner, accounts);
  for (const entry of ordering) {
    if (entry.isTotal) {
      const summaryMonthMap = payload.summaries.get2(owner, entry.id);
      if (summaryMonthMap) {
        const cells: GqlTableCell[] = months.map(
          (month) => toGqlTableCellFromStatement(summaryMonthMap?.get(month.toString()))
        );
        if (cells.some((c)=>!c.isClosed)) {
          const account = summaryMonthMap.values().next().value.account; 
          rows.push({ title: entry.title, indent: entry.depth, account: toGqlAccount(account), isTotal: true, cells });
        }
      } else {
        throw new Error(`Did not find summary statement for ${owner} "${entry.id}"`);
      }
    } else {
      const cells: GqlTableCell[] = [];
      const account = entry.account!;
      let isClosed = true;
      for (const month of months) {
        const stmt = payload.statements.get(account.name)?.get(month.toString());
        isClosed = isClosed && (stmt?.isClosed ?? false);
        cells.push(toGqlTableCellFromTransactionStatement(stmt));
      }
      if (!isClosed) {
        // Dont add accounts which are closed over selected timeframe.
        rows.push({ title: entry.title, indent: entry.depth, account: toGqlAccount(account), isNormal: true, cells });
      }
    }
  }
  rows.push({ title: 'Old BREAKDOWN', isSpace: true, cells: [] });
  // Insert Total summary row
  {
    const summaryMonthMap = payload.summaries.get2(owner, owner + ' SUMMARY');
    if (!summaryMonthMap) {
      throw new Error(`Not able to find total summary ${owner + ' SUMMARY'}`);
    }
    const cells: GqlTableCell[] = months.map(
      (month) => toGqlTableCellFromStatement(summaryMonthMap?.get(month.toString()))
    );
    rows.push({ title: owner, isTotal: true, cells });
  }

  // Group accounts per type
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
    const summaryMonthMap = payload.summaries.get2(owner, owner + ' ' + accountType);
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
