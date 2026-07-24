import {
  type GqlAccount, 
  type GqlMonthTransferSummary,
  type GqlStatement, 
  type GqlSummaryStatement, 
  type GqlTableRow
} from './gql_types';

export interface PopupMonthSummaryData {
  accountName: string;
  month: string;
  summary?: GqlSummaryStatement;
  statements?: StatementEntry[];
}

export interface PopupTransferSummaryData {
  accountPath: string;
  months: string[];
  monthlyData: GqlMonthTransferSummary[];
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

export type PopupData = PopupMonthSummaryData | PopupTransferSummaryData | PopupMonthData | HeadingPopupData;

export type Rows = {
  [owner: string]: GqlTableRow[];
};
