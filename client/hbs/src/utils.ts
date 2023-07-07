import {GqlAccount, GqlStatement, GqlSummaryStatement, GqlTableRow} from './gql_types';

export interface PopupMonthSummaryData {
  accountName: string;
  month: string;
  summary?: GqlSummaryStatement;
  statements?: StatementEntry[];
}

export interface StatementEntry {
  name: string;
  stmt: GqlStatement;
}

export interface PopupMonthData {
  accountName: string;
  month: string;
  stmt: GqlStatement;
}

export interface HeadingPopupData {
  account: GqlAccount;
}

export type PopupData = PopupMonthSummaryData | PopupMonthData | HeadingPopupData;

export type Rows = {
  [owner: string]: GqlTableRow[];
};
