import {
  type GqlAccount, 
  type GqlMonthTransferSummary,
  type GqlStatement, 
  type GqlSummaryStatement, 
  type GqlTableRow
} from './gql_types';
import { Month } from './month';

export interface PopupMonthSummaryData {
  kind: 'summary';
  accountName: string;
  startMonth: Month;
  endMonth: Month;
  summary?: GqlSummaryStatement;
  statements?: StatementEntry[];
}

export interface PopupTransferSummaryData {
  kind: 'transfers-summary';
  accountPath: string;
  months: Month[];
  monthlyData: GqlMonthTransferSummary[];
}

export interface StatementEntry {
  name: string;
  stmt: GqlStatement;
}

export interface PopupMonthData {
  kind: 'statement';
  accountName: string;
  month: Month;
  stmt: GqlStatement;
}

export interface HeadingPopupData {
  kind: 'account';
  account: GqlAccount;
}

export type PopupData = PopupMonthSummaryData | PopupTransferSummaryData | PopupMonthData | HeadingPopupData;

export type Rows = {
  [owner: string]: GqlTableRow[];
};
