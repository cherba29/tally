import {
  type GqlAccount, 
  type GqlMonthTransferSummary,
  type GqlStatement, 
  type GqlSummaryStatement, 
  type GqlTableRow
} from './gql_types';
import { Month } from '@tally/lib/core/month';

export interface PopupMonthSummaryData {
  accountName: string;
  startMonth: Month;
  endMonth: Month;
  summary?: GqlSummaryStatement;
  statements?: StatementEntry[];
}

export interface PopupTransferSummaryData {
  accountPath: string;
  months: Month[];
  monthlyData: GqlMonthTransferSummary[];
}

export interface StatementEntry {
  name: string;
  stmt: GqlStatement;
}

export interface PopupMonthData {
  accountName: string;
  month: Month;
  stmt: GqlStatement;
}

export interface HeadingPopupData {
  account: GqlAccount;
}

export type PopupData = PopupMonthSummaryData | PopupTransferSummaryData | PopupMonthData | HeadingPopupData;

export type Rows = {
  [owner: string]: GqlTableRow[];
};
