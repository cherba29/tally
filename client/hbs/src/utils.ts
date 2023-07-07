import {Cell} from './cell';
import {Row} from './row';
import {GqlAccount, GqlStatement, GqlSummaryStatement} from './gql_types';

export interface PopupMonthSummaryData {
  id: string;
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
  id: string;
  accountName: string;
  month: string;
  stmt: GqlStatement;
}

export interface HeadingPopupData {
  id: string;
  account: GqlAccount;
}

export type PopupData = PopupMonthSummaryData | PopupMonthData | HeadingPopupData;

interface CellData {
  cells: Cell[];
  popups: PopupData[];
}

export type Rows = {
  [owner: string]: Row[];
};

export interface MatrixDataView {
  months: string[];
  rows: Rows;
  popupCells: PopupData[];
}
