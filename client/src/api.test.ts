import {expect} from '@esm-bundle/chai';
import {BackendClient} from './api';

class MockWebSocket {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSING = 2;
  static readonly CLOSED = 3;

  static instances: MockWebSocket[] = [];

  readonly url: string;
  readonly protocol: string;
  readyState: number = MockWebSocket.CONNECTING;
  sent: string[] = [];
  closeCalls = 0;

  onopen: ((ev: Event) => void) | null = null;
  onmessage: ((ev: MessageEvent) => void) | null = null;
  onclose: ((ev: CloseEvent) => void) | null = null;
  onerror: ((ev: Event) => void) | null = null;

  constructor(url: string, protocol: string) {
    this.url = url;
    this.protocol = protocol;
    MockWebSocket.instances.push(this);
  }

  send(data: string) {
    this.sent.push(data);
  }

  close() {
    this.closeCalls++;
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.(new CloseEvent('close'));
  }

  emitOpen() {
    this.readyState = MockWebSocket.OPEN;
    this.onopen?.(new Event('open'));
  }

  emitMessage(payload: unknown) {
    this.onmessage?.(new MessageEvent('message', {data: JSON.stringify(payload)}));
  }

  emitRawMessage(raw: string) {
    this.onmessage?.(new MessageEvent('message', {data: raw}));
  }

  emitError() {
    this.onerror?.(new Event('error'));
  }
}

describe('BackendClient GraphQL queries', () => {
  const originalFetch = globalThis.fetch;
  let capturedRequests: Array<{url: string; body: {operationName: string; variables: Record<string, unknown>}}>;
  let nextResponseData: Record<string, unknown>;

  beforeEach(() => {
    capturedRequests = [];
    nextResponseData = {};
    globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
      const body = JSON.parse(String(init?.body ?? '{}'));
      capturedRequests.push({url, body});
      return new Response(JSON.stringify({data: nextResponseData}), {
        status: 200,
        headers: {'content-type': 'application/json'},
      });
    }) as typeof fetch;
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('loadTable sends table query with expected variables', async () => {
    nextResponseData = {
      table: {currentOwner: 'alex', owners: ['alex'], months: ['Nov2026'], rows: []},
    };
    const client = new BackendClient();
    const result = await client.loadTable('alex', 'Jul2026', 'Nov2026');

    expect(capturedRequests).to.have.lengthOf(1);
    expect(capturedRequests[0]!.url).to.match(/\/graphql$/);
    expect(capturedRequests[0]!.body.operationName).to.equal('table');
    expect(capturedRequests[0]!.body.variables).to.deep.equal({
      owner: 'alex',
      startMonth: 'Jul2026',
      endMonth: 'Nov2026',
    });
    expect(result.data?.table?.currentOwner).to.equal('alex');
  });

  it('loadSummaryData sends summary query with expected variables', async () => {
    nextResponseData = {summary: {statements: [], total: null}};
    const client = new BackendClient();
    const result = await client.loadSummaryData('alex/internal', 'Jul2026', 'Nov2026');

    expect(capturedRequests).to.have.lengthOf(1);
    expect(capturedRequests[0]!.body.operationName).to.equal('summary');
    expect(capturedRequests[0]!.body.variables).to.deep.equal({
      accountPath: 'alex/internal',
      startMonth: 'Jul2026',
      endMonth: 'Nov2026',
    });
    expect(result.data?.summary?.statements).to.deep.equal([]);
  });

  it('loadTransferSummaryData sends transfersSummary query with expected variables', async () => {
    nextResponseData = {transfersSummary: {months: ['Nov2026'], data: []}};
    const client = new BackendClient();
    const result = await client.loadTransferSummaryData('alex/internal', undefined, 'Nov2026');

    expect(capturedRequests).to.have.lengthOf(1);
    expect(capturedRequests[0]!.body.operationName).to.equal('transfersSummary');
    expect(capturedRequests[0]!.body.variables).to.deep.equal({
      accountPath: 'alex/internal',
      startMonth: null,
      endMonth: 'Nov2026',
    });
    expect(result.data?.transfersSummary?.months).to.deep.equal(['Nov2026']);
  });

  it('loadStatement sends statement query with expected variables', async () => {
    nextResponseData = {
      statement: {
        name: 'checking',
        month: 'Nov2026',
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
        startBalance: null,
        endBalance: null,
        transactions: [],
      },
    };
    const client = new BackendClient();
    const result = await client.loadStatement('alex/internal/checking', 'Nov2026');

    expect(capturedRequests).to.have.lengthOf(1);
    expect(capturedRequests[0]!.body.operationName).to.equal('statement');
    expect(capturedRequests[0]!.body.variables).to.deep.equal({
      accountPath: 'alex/internal/checking',
      month: 'Nov2026',
    });
    expect(result.data?.statement?.name).to.equal('checking');
  });
});

describe('BackendClient.subscribeToUpdates', () => {
  const originalWebSocket = globalThis.WebSocket;

  beforeEach(() => {
    MockWebSocket.instances = [];
    (globalThis as unknown as {WebSocket: typeof MockWebSocket}).WebSocket = MockWebSocket;
  });

  afterEach(() => {
    globalThis.WebSocket = originalWebSocket;
  });

  it('sends complete before closing on unsubscribe', () => {
    const client = new BackendClient();
    let reloadCount = 0;
    const unsubscribe = client.subscribeToUpdates(() => {
      reloadCount++;
    });

    expect(MockWebSocket.instances).to.have.lengthOf(1);
    const ws = MockWebSocket.instances[0]!;
    expect(ws.protocol).to.equal('graphql-transport-ws');
    const expectedProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    expect(ws.url).to.equal(`${expectedProtocol}//${window.location.host}/subscriptions`);

    ws.emitOpen();
    expect(ws.sent.map((m) => JSON.parse(m))).to.deep.equal([{type: 'connection_init'}]);

    ws.emitMessage({type: 'connection_ack'});
    expect(ws.sent.map((m) => JSON.parse(m))).to.deep.equal([
      {type: 'connection_init'},
      {id: '1', type: 'subscribe', payload: {query: 'subscription { tableUpdates }'}},
    ]);

    ws.emitMessage({id: '1', type: 'next', payload: {data: {tableUpdates: true}}});
    expect(reloadCount).to.equal(1);

    unsubscribe();
    expect(ws.sent.map((m) => JSON.parse(m))).to.deep.equal([
      {type: 'connection_init'},
      {id: '1', type: 'subscribe', payload: {query: 'subscription { tableUpdates }'}},
      {id: '1', type: 'complete'},
    ]);
    expect(ws.closeCalls).to.equal(1);
  });

  it('sends complete once and closes on pagehide/beforeunload', () => {
    const client = new BackendClient();
    const unsubscribe = client.subscribeToUpdates(() => {});

    const ws = MockWebSocket.instances[0]!;
    ws.emitOpen();
    ws.emitMessage({type: 'connection_ack'});

    window.dispatchEvent(new Event('pagehide'));
    window.dispatchEvent(new Event('beforeunload'));

    const completeMessages = ws.sent
      .map((m) => JSON.parse(m))
      .filter((m) => m.type === 'complete');
    expect(completeMessages).to.deep.equal([{id: '1', type: 'complete'}]);
    expect(ws.closeCalls).to.equal(1);

    unsubscribe();
    expect(ws.closeCalls).to.equal(1);
  });

  it('does not send complete for an already-errored operation', () => {
    const client = new BackendClient();
    let reloadCount = 0;
    const unsubscribe = client.subscribeToUpdates(() => {
      reloadCount++;
    });

    const ws = MockWebSocket.instances[0]!;
    ws.emitOpen();
    ws.emitMessage({type: 'connection_ack'});
    ws.emitMessage({id: '1', type: 'error', payload: [{message: 'YAML error'}]});
    expect(reloadCount).to.equal(1);

    unsubscribe();
    const completeMessages = ws.sent
      .map((m) => JSON.parse(m))
      .filter((m) => m.type === 'complete');
    expect(completeMessages).to.have.lengthOf(0);
    expect(ws.closeCalls).to.equal(1);
  });

  it('returns a no-op unsubscribe when WebSocket is unavailable', () => {
    (globalThis as unknown as {WebSocket: unknown}).WebSocket = undefined;
    const client = new BackendClient();
    const unsubscribe = client.subscribeToUpdates(() => {});
    expect(() => unsubscribe()).to.not.throw();
    expect(MockWebSocket.instances).to.have.lengthOf(0);
  });

  it('handles server complete, malformed JSON, send errors on complete, and late onopen after close', () => {
    const client = new BackendClient();
    const unsubscribe = client.subscribeToUpdates(() => {});
    const ws = MockWebSocket.instances[0]!;
    ws.emitOpen();
    ws.emitMessage({type: 'connection_ack'});

    // Server sends complete for active subscription
    ws.emitMessage({id: '1', type: 'complete'});
    // Malformed JSON is caught without throwing
    ws.emitRawMessage('not-valid-json');
    // onerror handler does not throw
    ws.emitError();

    unsubscribe();
    const completeMessages = ws.sent
      .map((m) => JSON.parse(m))
      .filter((m) => m.type === 'complete');
    expect(completeMessages).to.have.lengthOf(0);

    // sendComplete catches errors from ws.send
    const unsubscribe2 = client.subscribeToUpdates(() => {});
    const ws2 = MockWebSocket.instances[1]!;
    ws2.emitOpen();
    ws2.emitMessage({type: 'connection_ack'});
    ws2.send = () => {
      throw new Error('closing');
    };
    expect(() => unsubscribe2()).to.not.throw();
    expect(ws2.closeCalls).to.equal(1);

    // Late onopen after unsubscribe closes socket without sending connection_init
    const unsubscribe3 = client.subscribeToUpdates(() => {});
    const ws3 = MockWebSocket.instances[2]!;
    unsubscribe3();
    ws3.emitOpen();
    expect(ws3.sent).to.have.lengthOf(0);
    expect(ws3.closeCalls).to.equal(2);
  });

  it('resubscribes after error, reconnects on unexpected close, cancels reconnect on unsubscribe, and retries constructor errors', () => {
    const timers = new Map<number, {cb: () => void; delay: number}>();
    let nextId = 1;
    const origSetTimeout = globalThis.setTimeout;
    const origClearTimeout = globalThis.clearTimeout;
    globalThis.setTimeout = ((cb: () => void, delay = 0) => {
      const id = nextId++;
      timers.set(id, {cb, delay});
      return id as unknown as ReturnType<typeof setTimeout>;
    }) as typeof setTimeout;
    globalThis.clearTimeout = ((id: ReturnType<typeof setTimeout>) => {
      timers.delete(id as unknown as number);
    }) as typeof clearTimeout;

    try {
      const client = new BackendClient();
      let reloads = 0;
      const unsubscribe = client.subscribeToUpdates(() => {
        reloads++;
      });

      const ws1 = MockWebSocket.instances[0]!;
      ws1.emitOpen();
      ws1.emitMessage({type: 'connection_ack'});

      // Error triggers reload and schedules 1000ms resubscribe
      ws1.emitMessage({id: '1', type: 'error', payload: [{message: 'err'}]});
      expect(reloads).to.equal(1);
      expect(timers.size).to.equal(1);
      const resubEntry = Array.from(timers.entries())[0]!;
      expect(resubEntry[1].delay).to.equal(1000);
      timers.delete(resubEntry[0]);
      resubEntry[1].cb();

      expect(ws1.sent.map((m) => JSON.parse(m)).pop()).to.deep.equal({
        id: '2',
        type: 'subscribe',
        payload: {query: 'subscription { tableUpdates }'},
      });

      // Unexpected close schedules 3000ms reconnect
      ws1.readyState = MockWebSocket.CLOSED;
      ws1.onclose?.(new CloseEvent('close'));
      expect(timers.size).to.equal(1);
      const reconnEntry = Array.from(timers.entries())[0]!;
      expect(reconnEntry[1].delay).to.equal(3000);
      timers.delete(reconnEntry[0]);
      reconnEntry[1].cb();
      expect(MockWebSocket.instances).to.have.lengthOf(2);

      // Unexpected close followed by unsubscribe clears pending reconnect timeout
      const ws2 = MockWebSocket.instances[1]!;
      ws2.readyState = MockWebSocket.CLOSED;
      ws2.onclose?.(new CloseEvent('close'));
      expect(timers.size).to.equal(1);
      unsubscribe();
      expect(timers.size).to.equal(0);

      // WebSocket constructor throwing schedules 3000ms reconnect
      class ThrowingWebSocket {
        static readonly OPEN = 1;
        static readonly CONNECTING = 0;
        constructor() {
          throw new Error('ws init failed');
        }
      }
      (globalThis as unknown as {WebSocket: typeof ThrowingWebSocket}).WebSocket = ThrowingWebSocket;
      const unsubscribeThrowing = client.subscribeToUpdates(() => {});
      expect(timers.size).to.equal(1);
      expect(Array.from(timers.values())[0]!.delay).to.equal(3000);
      unsubscribeThrowing();
      expect(timers.size).to.equal(0);
    } finally {
      globalThis.setTimeout = origSetTimeout;
      globalThis.clearTimeout = origClearTimeout;
    }
  });
});
