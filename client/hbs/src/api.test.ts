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
}

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
});
