import {GqlAccount, GqlBudget, GqlStatement, GqlSummaryStatement} from './gql_types';
import {transformBudgetData, MatrixDataView} from './utils';

/**
 * Builds dataView structure with months, rows and popup cell data.
 *
 * @param data - Server returnred GqlBudget structure.
 * @return MatrixDataView dataview structure.
 */
export function transformGqlBudgetData(data: GqlBudget | undefined): MatrixDataView {
  const accountNameToAccount: {[accountName: string]: GqlAccount} = {};
  for (const gqlAccount of data?.accounts ?? []) {
    if (!gqlAccount) continue;
    accountNameToAccount[gqlAccount.name ?? ''] = gqlAccount;
  }
  // account name => month => statement map.
  const statements: {[accountName: string]: {[month: string]: GqlStatement}} = {};
  for (const stmt of data?.statements ?? []) {
    if (!stmt || !stmt.name) continue;
    const entry = statements[stmt.name] || (statements[stmt.name] = {});
    entry[stmt.month] = stmt;
  }
  // owner + accout type => month => summary statement map.
  const summaries: {
    [ownerAccountType: string]: {[month: string]: GqlSummaryStatement};
  } = {};
  for (const stmt of data?.summaries ?? []) {
    if (!stmt || !stmt.name) continue;
    const entry = summaries[stmt.name] || (summaries[stmt.name] = {});
    entry[stmt.month] = stmt;
  }
  return transformBudgetData(data?.months || [], accountNameToAccount, statements, summaries);
}
