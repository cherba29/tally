import {expect} from '@esm-bundle/chai';
import './tally-app';
import {type TallyApp} from './tally-app';
import {type BackendClient} from '../api';
import {type CellClickEventData} from './summary-table';
import {type MonthRangeChange} from './balance-summary-tooltip';
import {type TransfersSummaryMonthRangeChange} from './transfers-summary-tooltip';
import {Month} from '../month';
import {
  type GqlAccount,
  type GqlMonthTransferSummary,
  type GqlStatement,
  type GqlTableRow,
} from '../gql_types';

const makeAccount = (name: string, description: string): GqlAccount => ({
  name,
  description,
  path: ['owner1', 'checking', name],
  openedOn: 'Jan2024',
  closedOn: null,
  number: '1234',
  owners: ['owner1'],
  address: '',
  external: false,
  summary: false,
  userName: '',
  password: '',
  statementCloseDay: '1',
  phone: '',
  url: '',
});

const makeRow = (id: string, account: GqlAccount): GqlTableRow => ({
  id,
  title: account.name,
  account,
  indent: 0,
  isSpace: false,
  isTotal: false,
  isNormal: true,
  isInactive: false,
  cells: [],
});

const makeStatement = (name: string, month: string, amount: number): GqlStatement => ({
  name,
  month,
  inFlows: 0,
  outFlows: 0,
  income: 0,
  totalPayments: 0,
  totalTransfers: 0,
  isClosed: false,
  isCovered: true,
  isProjectedCovered: true,
  hasProjectedTransfer: false,
  change: 0,
  addSub: 0,
  percentChange: 0,
  annualizedPercentChange: 0,
  unaccounted: 0,
  startBalance: {amount, date: '2025-01-01', desc: '', type: 'CONFIRMED'},
  endBalance: {amount, date: '2025-01-31', desc: '', type: 'CONFIRMED'},
  transactions: [],
});

const makeTransferSummary = (totalTransfers: number): GqlMonthTransferSummary => ({
  internalTransfers: 0,
  externalTransfers: 0,
  totalMonthTransfers: 0,
  totalInternalTransfers: 0,
  totalInternalTransfersPrct: 0,
  totalInternalTransfersAnnualPrct: 0,
  weightedInternalAge: 0,
  totalExternalTransfers: 0,
  totalExternalTransfersPrct: 0,
  totalExternalTransfersAnnualPrct: 0,
  weightedExternalAge: 0,
  totalTransfers,
  totalAnnualPrct: 0,
  weightedAge: 0,
  unaccounted: 0,
});

const flush = async (el: TallyApp) => {
  await Promise.resolve();
  await Promise.resolve();
  await el.updateComplete;
};

describe('TallyApp popup refresh on reload', () => {
  let el: TallyApp;
  let onSubscriptionReload: (() => void) | undefined;
  let tableRows: GqlTableRow[];
  let tableError: {message: string} | undefined;
  let statementAmount: number;
  let summaryAmount: number;
  let transferTotal: number;
  let loadStatementCalls: Array<[string, string]>;
  let loadSummaryCalls: Array<[string, string | undefined, string]>;
  let loadTransferCalls: Array<[string, string | undefined, string]>;
  let loadTableCalls: Array<[string, string, string]>;

  const createMockClient = (): BackendClient =>
    ({
      loadTable: (owner: string, startMonth: string, endMonth: string) => {
        loadTableCalls.push([owner, startMonth, endMonth]);
        if (tableError) {
          return Promise.resolve({error: tableError});
        }
        return Promise.resolve({
          data: {
            table: {
              currentOwner: owner || 'owner1',
              owners: ['owner1', 'owner2'],
              months: ['Jan2025', 'Feb2025'],
              rows: tableRows,
            },
          },
        });
      },
      loadStatement: (accountPath: string, month: string) => {
        loadStatementCalls.push([accountPath, month]);
        return Promise.resolve({
          data: {
            statement: makeStatement(accountPath, month, statementAmount),
          },
        });
      },
      loadSummaryData: (accountPath: string, startMonth: string | undefined, endMonth: string) => {
        loadSummaryCalls.push([accountPath, startMonth, endMonth]);
        return Promise.resolve({
          data: {
            summary: {
              statements: [makeStatement(accountPath, endMonth, summaryAmount)],
              total: {
                name: accountPath,
                month: endMonth,
                inFlows: 0,
                outFlows: 0,
                income: 0,
                totalPayments: 0,
                totalTransfers: 0,
                change: 0,
                addSub: 0,
                percentChange: 0,
                annualizedPercentChange: 0,
                unaccounted: 0,
                startBalance: {amount: summaryAmount, date: '2025-01-01', desc: '', type: 'CONFIRMED'},
                endBalance: {amount: summaryAmount, date: '2025-01-31', desc: '', type: 'CONFIRMED'},
              },
            },
          },
        });
      },
      loadTransferSummaryData: (
        accountPath: string,
        startMonth: string | undefined,
        endMonth: string
      ) => {
        loadTransferCalls.push([accountPath, startMonth, endMonth]);
        return Promise.resolve({
          data: {
            transfersSummary: {
              months: [endMonth],
              data: [makeTransferSummary(transferTotal)],
            },
          },
        });
      },
      subscribeToUpdates: (cb: () => void) => {
        onSubscriptionReload = cb;
        return () => {
          onSubscriptionReload = undefined;
        };
      },
    }) as unknown as BackendClient;

  beforeEach(async () => {
    tableError = undefined;
    statementAmount = 10000;
    summaryAmount = 20000;
    transferTotal = 30000;
    loadStatementCalls = [];
    loadSummaryCalls = [];
    loadTransferCalls = [];
    loadTableCalls = [];
    tableRows = [makeRow('owner1/checking/acct1', makeAccount('acct1', 'Initial desc'))];

    el = document.createElement('tally-app') as TallyApp;
    el.backendClient = createMockClient();
    document.body.appendChild(el);
    await flush(el);
  });

  afterEach(() => {
    el.remove();
  });

  const getSummaryTable = () => el.shadowRoot!.querySelector('summary-table')!;
  const getReloadButton = () =>
    Array.from(el.shadowRoot!.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Reload Table'
    )!;

  it('refreshes an open statement popup with fresh data on subscription and button reload', async () => {
    const month = Month.fromString('Jan2025');
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking/acct1',
          month,
          isSummary: false,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);

    let tooltip = el.shadowRoot!.querySelector('balance-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.stmt?.endBalance?.amount).to.equal(10000);

    // Trigger subscription reload with updated statement amount
    statementAmount = 15500;
    onSubscriptionReload!();
    await flush(el);

    tooltip = el.shadowRoot!.querySelector('balance-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.stmt?.endBalance?.amount).to.equal(15500);

    // Trigger manual Reload Table button with another updated amount
    statementAmount = 18800;
    getReloadButton().click();
    await flush(el);

    tooltip = el.shadowRoot!.querySelector('balance-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.stmt?.endBalance?.amount).to.equal(18800);
    expect(loadStatementCalls).to.deep.equal([
      ['owner1/checking/acct1', 'Jan2025'],
      ['owner1/checking/acct1', 'Jan2025'],
      ['owner1/checking/acct1', 'Jan2025'],
    ]);
  });

  it('refreshes an open summary popup preserving its selected month range', async () => {
    const endMonth = Month.fromString('Feb2025');
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month: endMonth,
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);

    let tooltip = el.shadowRoot!.querySelector('balance-summary-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.summary?.endBalance?.amount).to.equal(20000);

    // User changes period to "All" (startMonth: undefined)
    tooltip!.dispatchEvent(
      new CustomEvent<MonthRangeChange>('month-range-change', {
        detail: {
          accountName: 'owner1/checking',
          startMonth: undefined,
          endMonth,
        },
      })
    );
    await flush(el);

    summaryAmount = 25000;
    onSubscriptionReload!();
    await flush(el);

    tooltip = el.shadowRoot!.querySelector('balance-summary-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.summary?.endBalance?.amount).to.equal(25000);
    expect(loadSummaryCalls).to.deep.equal([
      ['owner1/checking', 'Feb2025', 'Feb2025'],
      ['owner1/checking', undefined, 'Feb2025'],
      ['owner1/checking', undefined, 'Feb2025'],
    ]);
  });

  it('refreshes an open transfers summary popup preserving its selected range', async () => {
    const endMonth = Month.fromString('Feb2025');
    const customStart = Month.fromString('Jan2024');
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('addSubCellClick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month: endMonth,
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);

    let tooltip = el.shadowRoot!.querySelector('transfers-summary-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.monthlyData[0]?.totalTransfers).to.equal(30000);

    tooltip!.dispatchEvent(
      new CustomEvent<TransfersSummaryMonthRangeChange>('month-range-change', {
        detail: {
          accountPath: 'owner1/checking',
          startMonth: customStart,
          endMonth,
        },
      })
    );
    await flush(el);

    transferTotal = 42000;
    onSubscriptionReload!();
    await flush(el);

    tooltip = el.shadowRoot!.querySelector('transfers-summary-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.monthlyData[0]?.totalTransfers).to.equal(42000);
    expect(loadTransferCalls[loadTransferCalls.length - 1]).to.deep.equal([
      'owner1/checking',
      'Jan2024',
      'Feb2025',
    ]);
  });

  it('refreshes an open account popup from table rows and closes it if account is removed', async () => {
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking/acct1',
          account: tableRows[0]!.account,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);

    let tooltip = el.shadowRoot!.querySelector('account-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.account?.description).to.equal('Initial desc');

    // Reload with updated account metadata
    tableRows = [makeRow('owner1/checking/acct1', makeAccount('acct1', 'Updated desc'))];
    onSubscriptionReload!();
    await flush(el);

    tooltip = el.shadowRoot!.querySelector('account-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.account?.description).to.equal('Updated desc');

    // Reload where account no longer exists
    tableRows = [];
    onSubscriptionReload!();
    await flush(el);

    expect(el.shadowRoot!.querySelector('account-tooltip')).to.equal(null);
  });

  it('does not reopen a popup after the user closes it', async () => {
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking/acct1',
          month: Month.fromString('Jan2025'),
          isSummary: false,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);

    const tooltip = el.shadowRoot!.querySelector('balance-tooltip')!;
    tooltip.dispatchEvent(new CustomEvent('close'));
    await flush(el);

    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.equal(null);

    onSubscriptionReload!();
    await flush(el);

    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.equal(null);
    expect(loadStatementCalls).to.have.lengthOf(1);
  });

  it('hides popup on table load error and reopens with fresh data once error resolves', async () => {
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking/acct1',
          month: Month.fromString('Jan2025'),
          isSummary: false,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.not.equal(null);

    // YAML syntax error during reload hides stale popup
    tableError = {message: 'YAML parse error'};
    onSubscriptionReload!();
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.equal(null);

    // YAML fixed -> reload succeeds and reopens popup with fresh data
    tableError = undefined;
    statementAmount = 99900;
    onSubscriptionReload!();
    await flush(el);

    const tooltip = el.shadowRoot!.querySelector('balance-tooltip');
    expect(tooltip).to.not.equal(null);
    expect(tooltip!.stmt?.endBalance?.amount).to.equal(99900);
  });

  it('ignores an in-flight popup refresh if the user closes the popup before it resolves', async () => {
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking/acct1',
          month: Month.fromString('Jan2025'),
          isSummary: false,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);

    // Hold the next loadStatement promise
    let resolveInFlight!: (value: unknown) => void;
    const origLoadStatement = el.backendClient.loadStatement;
    el.backendClient.loadStatement = (accountPath: string, month: string) => {
      loadStatementCalls.push([accountPath, month]);
      return new Promise<unknown>((resolve) => {
        resolveInFlight = resolve;
      }) as ReturnType<BackendClient['loadStatement']>;
    };

    onSubscriptionReload!();
    await flush(el);

    // User closes popup while refresh is still in flight
    el.shadowRoot!.querySelector('balance-tooltip')!.dispatchEvent(new CustomEvent('close'));
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.equal(null);

    // Stale refresh resolves — popup must remain closed
    resolveInFlight({
      data: {statement: makeStatement('owner1/checking/acct1', 'Jan2025', 77700)},
    });
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.equal(null);

    el.backendClient.loadStatement = origLoadStatement;
  });

  it('updates month range via toolbar buttons and reloads table', async () => {
    const toolbarDiv = el.shadowRoot!.querySelector('div')!;
    const buttons = Array.from(toolbarDiv.querySelectorAll('button'));
    // 12 navigation buttons + Reload Table
    expect(buttons).to.have.lengthOf(13);

    const initialCall = loadTableCalls[0]!;
    const initialStart = Month.fromString(initialCall[1]);
    const initialEnd = Month.fromString(initialCall[2]);

    // Click all 12 navigation buttons: +12/-12/+1/-1 on end, start, and both (net delta 0)
    const expectedDeltas: Array<[number, number]> = [
      [0, 12],
      [0, 1],
      [0, -1],
      [0, -12],
      [12, 0],
      [1, 0],
      [-1, 0],
      [-12, 0],
      [12, 12],
      [1, 1],
      [-1, -1],
      [-12, -12],
    ];
    let curStart = initialStart;
    let curEnd = initialEnd;
    for (let i = 0; i < 12; i++) {
      const [sd, ed] = expectedDeltas[i]!;
      curStart = curStart.next(sd);
      curEnd = curEnd.next(ed);
      buttons[i]!.click();
      await flush(el);
      const lastCall = loadTableCalls[loadTableCalls.length - 1]!;
      expect(lastCall[1]).to.equal(curStart.toString());
      expect(lastCall[2]).to.equal(curEnd.toString());
    }
  });

  it('switches owner tabs, closes popup on owner change, and keeps popup on same-owner click', async () => {
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking/acct1',
          month: Month.fromString('Jan2025'),
          isSummary: false,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.not.equal(null);

    const divs = el.shadowRoot!.querySelectorAll('div');
    const ownerButtons = Array.from(divs[3]!.querySelectorAll('button'));
    expect(ownerButtons).to.have.lengthOf(2);

    // Clicking the already-active owner keeps the popup open
    ownerButtons[0]!.click();
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.not.equal(null);

    // Clicking owner2 closes the popup and reloads table for owner2
    ownerButtons[1]!.click();
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.equal(null);
    const lastCall = loadTableCalls[loadTableCalls.length - 1]!;
    expect(lastCall[0]).to.equal('owner2');
  });

  it('clears popup when statement, summary, or transfer-summary requests error or reject', async () => {
    // 1. Statement error & rejection
    el.backendClient.loadStatement = () =>
      Promise.resolve({error: {message: 'not found'}}) as unknown as ReturnType<BackendClient['loadStatement']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking/acct1',
          month: Month.fromString('Jan2025'),
          isSummary: false,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.equal(null);

    el.backendClient.loadStatement = () =>
      Promise.reject(new Error('network')) as unknown as ReturnType<BackendClient['loadStatement']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking/acct1',
          month: Month.fromString('Jan2025'),
          isSummary: false,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-tooltip')).to.equal(null);

    // 2. Summary error, rejection, and empty fields
    el.backendClient.loadSummaryData = () =>
      Promise.resolve({error: {message: 'summary err'}}) as unknown as ReturnType<BackendClient['loadSummaryData']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month: Month.fromString('Feb2025'),
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-summary-tooltip')).to.equal(null);

    el.backendClient.loadSummaryData = () =>
      Promise.reject(new Error('summary network')) as unknown as ReturnType<BackendClient['loadSummaryData']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month: Month.fromString('Feb2025'),
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-summary-tooltip')).to.equal(null);

    // 3. Transfers summary error & rejection
    el.backendClient.loadTransferSummaryData = () =>
      Promise.resolve({error: {message: 'transfers err'}}) as unknown as ReturnType<
        BackendClient['loadTransferSummaryData']
      >;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('addSubCellClick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month: Month.fromString('Feb2025'),
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('transfers-summary-tooltip')).to.equal(null);

    el.backendClient.loadTransferSummaryData = () =>
      Promise.reject(new Error('transfers network')) as unknown as ReturnType<
        BackendClient['loadTransferSummaryData']
      >;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('addSubCellClick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month: Month.fromString('Feb2025'),
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('transfers-summary-tooltip')).to.equal(null);
  });

  it('ignores stale popup responses and rejections after closing, and handles null/empty fields', async () => {
    const month = Month.fromString('Feb2025');

    // 1. Stale statement rejection ignored after popup closed
    let rejectStmt!: (err: Error) => void;
    el.backendClient.loadStatement = () =>
      new Promise((_, reject) => {
        rejectStmt = reject;
      }) as ReturnType<BackendClient['loadStatement']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: undefined as unknown as string,
          month,
          isSummary: false,
        },
        bubbles: true,
        composed: true,
      })
    );
    // Supersede by clicking another cell before previous settles, then reject stale promise
    let resolveSummary!: (val: unknown) => void;
    el.backendClient.loadSummaryData = () =>
      new Promise((resolve) => {
        resolveSummary = resolve;
      }) as ReturnType<BackendClient['loadSummaryData']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: undefined as unknown as string,
          month,
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    rejectStmt(new Error('stale stmt err'));
    await flush(el);

    // 2. Stale summary resolve & reject ignored after supersede
    let rejectSummary!: (err: Error) => void;
    el.backendClient.loadSummaryData = () =>
      new Promise((_, reject) => {
        rejectSummary = reject;
      }) as ReturnType<BackendClient['loadSummaryData']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month,
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    resolveSummary({data: {summary: {statements: null, total: null}}});
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-summary-tooltip')).to.equal(null);

    // Trigger valid summary with null total, null statements, and unnamed statement
    el.backendClient.loadSummaryData = () =>
      Promise.resolve({
        data: {
          summary: {
            statements: [{...makeStatement('', 'Feb2025', 500), name: undefined}],
            total: null,
          },
        },
      }) as unknown as ReturnType<BackendClient['loadSummaryData']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month,
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    rejectSummary(new Error('stale summary err'));
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-summary-tooltip')).to.not.equal(null);

    // Summary with null statements array
    el.backendClient.loadSummaryData = () =>
      Promise.resolve({
        data: {summary: {statements: null, total: null}},
      }) as unknown as ReturnType<BackendClient['loadSummaryData']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('cellclick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month,
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    await flush(el);
    expect(el.shadowRoot!.querySelector('balance-summary-tooltip')).to.not.equal(null);

    // 3. Stale transfers-summary resolve & reject ignored after supersede
    let resolveTransfers!: (val: unknown) => void;
    el.backendClient.loadTransferSummaryData = () =>
      new Promise((resolve) => {
        resolveTransfers = resolve;
      }) as ReturnType<BackendClient['loadTransferSummaryData']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('addSubCellClick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month,
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );

    let rejectTransfers!: (err: Error) => void;
    el.backendClient.loadTransferSummaryData = () =>
      new Promise((_, reject) => {
        rejectTransfers = reject;
      }) as ReturnType<BackendClient['loadTransferSummaryData']>;
    getSummaryTable().dispatchEvent(
      new CustomEvent<CellClickEventData>('addSubCellClick', {
        detail: {
          mouseEvent: new MouseEvent('click'),
          rowId: 'owner1/checking',
          month,
          isSummary: true,
        },
        bubbles: true,
        composed: true,
      })
    );
    resolveTransfers({data: {transfersSummary: {months: ['Feb2025'], data: []}}});
    // Close popup while second transfers request is in flight, then reject it
    el.shadowRoot!.querySelector('balance-summary-tooltip')?.dispatchEvent(new CustomEvent('close'));
    rejectTransfers(new Error('stale transfers err'));
    await flush(el);
    expect(el.shadowRoot!.querySelector('transfers-summary-tooltip')).to.equal(null);

    // 4. Null/empty table payload fields
    el.backendClient.loadTable = () =>
      Promise.resolve({
        data: {
          table: {
            currentOwner: null,
            owners: null,
            months: null,
            rows: null,
          },
        },
      }) as unknown as ReturnType<BackendClient['loadTable']>;
    onSubscriptionReload!();
    await flush(el);
    expect(el.shadowRoot!.querySelector('summary-table')).to.not.equal(null);
  });
});
