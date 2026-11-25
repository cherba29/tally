import {type Query} from './gql_types';

export type ConnectionState = 'connected' | 'reconnecting' | 'disconnected';

export interface GraphQLErrorInfo {
  message: string;
}

export interface QueryResult<T> {
  data?: T;
  error?: GraphQLErrorInfo;
}

interface GraphQLResponse<T> {
  data?: T;
  errors?: Array<{message?: string}>;
}

const getGraphqlUrl = (): string => {
  if (typeof window !== 'undefined' && window.location && window.location.origin !== 'null') {
    return `${window.location.origin}/graphql`;
  }
  return 'http://localhost:8080/graphql';
};

const getSubscriptionUrl = (): string => {
  if (typeof window !== 'undefined' && window.location && window.location.host) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/subscriptions`;
  }
  return 'ws://localhost:8080/subscriptions';
};

/**
 * GraphQL Backend Client.
 */
export class BackendClient {
  private async executeQuery<T>(
    operationName: string,
    query: string,
    variables: Record<string, unknown>
  ): Promise<QueryResult<T>> {
    const response = await fetch(getGraphqlUrl(), {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        accept: 'application/json',
      },
      body: JSON.stringify({operationName, query, variables}),
    });

    if (!response.ok) {
      let message = `GraphQL request failed (${response.status} ${response.statusText})`;
      try {
        const errorBody = (await response.json()) as GraphQLResponse<T>;
        if (errorBody.errors && errorBody.errors.length > 0) {
          message = errorBody.errors.map((e) => e.message ?? 'GraphQL error').join('\n');
        }
      } catch {
        // Non-JSON error response; keep HTTP status message.
      }
      throw new Error(message);
    }

    const payload = (await response.json()) as GraphQLResponse<T>;
    if (payload.errors && payload.errors.length > 0) {
      return {
        data: payload.data,
        error: {
          message: payload.errors.map((e) => e.message ?? 'GraphQL error').join('\n'),
        },
      };
    }
    return {data: payload.data};
  }

  /**
   * Load summary table data via GraphQL.
   * @return promise of query result.
   */
  loadTable(
    owner: string,
    startMonth: string,
    endMonth: string
  ): Promise<QueryResult<Query>> {
    return this.executeQuery<Query>(
      'table',
      `
        query table($owner: String!, $startMonth: GqlMonth!, $endMonth: GqlMonth!) {
          table(owner: $owner, startMonth: $startMonth, endMonth: $endMonth) {
            currentOwner
            owners
            months
            rows {
              id
              title
              account {
                name
                description
                path
                openedOn
                closedOn
                number
                owners
                address
                external
                summary
                userName
                password
                statementCloseDay
                phone
                url
              }
              indent
              isSpace
              isTotal
              isNormal
              isInactive
              cells {
                month
                isClosed
                addSub
                balance
                isProjected
                isCovered
                isProjectedCovered
                hasProjectedTransfer
                percentChange
                annualizedPercentChange
                unaccounted
                balanced
              }
            }
          }
        }
      `,
      {
        owner,
        startMonth,
        endMonth,
      }
    );
  }

  /**
   * Load summary (popup) data via GraphQL.
   * @return promise of query result.
   */
  loadSummaryData(
    accountPath: string,
    startMonth: string | undefined,
    endMonth: string
  ): Promise<QueryResult<Query>> {
    return this.executeQuery<Query>(
      'summary',
      `
        query summary(
          $accountPath: String!
          $startMonth: GqlMonth = null
          $endMonth: GqlMonth!
        ) {
          summary(
            accountPath: $accountPath
            startMonth: $startMonth
            endMonth: $endMonth
          ) {
            statements {
              addSub
              change
              endBalance {
                amount
                date
                type
              }
              hasProjectedTransfer
              inFlows
              income
              isClosed
              isCovered
              isProjectedCovered
              month
              name
              outFlows
              percentChange
              annualizedPercentChange
              startBalance {
                amount
                date
                type
              }
              totalPayments
              totalTransfers
              unaccounted
            }
            total {
              addSub
              change
              endBalance {
                amount
                date
                type
              }
              inFlows
              income
              month
              name
              outFlows
              percentChange
              annualizedPercentChange
              startBalance {
                amount
                date
                type
              }
              totalPayments
              totalTransfers
              unaccounted
            }
          }
        }
      `,
      {
        accountPath,
        startMonth: startMonth ?? null,
        endMonth,
      }
    );
  }

  /**
   * Load transfer summary (popup) data via GraphQL.
   * @return promise of query result.
   */
  loadTransferSummaryData(
    accountPath: string,
    startMonth: string | undefined,
    endMonth: string
  ): Promise<QueryResult<Query>> {
    return this.executeQuery<Query>(
      'transfersSummary',
      `
        query transfersSummary(
          $accountPath: String!
          $startMonth: GqlMonth = null
          $endMonth: GqlMonth!
        ) {
          transfersSummary(
            accountPath: $accountPath
            startMonth: $startMonth
            endMonth: $endMonth
          ) {
            months
            data {
              internalTransfers
              externalTransfers
              totalMonthTransfers
              totalInternalTransfers
              totalInternalTransfersPrct
              totalInternalTransfersAnnualPrct
              weightedInternalAge
              totalExternalTransfers
              totalExternalTransfersPrct
              totalExternalTransfersAnnualPrct
              weightedExternalAge
              totalTransfers
              totalAnnualPrct
              weightedAge
              unaccounted
            }
          }
        }
      `,
      {
        accountPath,
        startMonth: startMonth ?? null,
        endMonth,
      }
    );
  }

  /**
   * Load statement data via GraphQL.
   * @return promise of query result.
   */
  loadStatement(accountPath: string, month: string): Promise<QueryResult<Query>> {
    return this.executeQuery<Query>(
      'statement',
      `
        query statement($accountPath: String!, $month: GqlMonth!) {
          statement(accountPath: $accountPath, month: $month) {
            name
            month
            inFlows
            outFlows
            income
            totalPayments
            totalTransfers
            isClosed
            isCovered
            isProjectedCovered
            hasProjectedTransfer
            change
            addSub
            percentChange
            annualizedPercentChange
            unaccounted
            startBalance {
              amount
              date
              type
            }
            endBalance {
              amount
              date
              type
            }
            transactions {
              toAccountName
              isIncome
              isExpense
              balance {
                amount
                date
                type
              }
              balanceFromStart
              description
              recurringGroupId
              recurringCount
              recurringCadence
            }
          }
        }
      `,
      {
        accountPath,
        month,
      }
    );
  }

  /**
   * Load all occurrences and summary metadata for a detected recurring transaction group.
   * @return promise of query result.
   */
  loadRecurringTransactions(
    groupId: string,
    accountPath?: string
  ): Promise<QueryResult<Query>> {
    return this.executeQuery<Query>(
      'recurringTransactions',
      `
        query recurringTransactions($groupId: String!, $accountPath: String = null) {
          recurringTransactions(groupId: $groupId, accountPath: $accountPath) {
            id
            label
            cadence
            occurrenceCount
            averageAmount
            minAmount
            maxAmount
            totalAmount
            isFixedAmount
            typicalDayOfMonth
            transactions {
              month
              fromAccountName
              fromAccountPath
              toAccountName
              toAccountPath
              counterpartyAccountName
              isIncome
              isExpense
              isCurrentAccount
              description
              balance {
                amount
                date
                type
              }
            }
          }
        }
      `,
      {
        groupId,
        accountPath: accountPath ?? null,
      }
    );
  }

  /**
   * Subscribes to table updates from the GraphQL backend.
   * Calls onReload callback whenever data is reloaded, and onStateChange when WebSocket
   * connection state changes. Returns an unsubscribe function.
   */
  subscribeToUpdates(
    onReload: () => void,
    onStateChange?: (state: ConnectionState) => void
  ): () => void {
    if (typeof WebSocket === 'undefined') {
      onStateChange?.('disconnected');
      return () => {};
    }

    let isClosed = false;
    let ws: WebSocket | null = null;
    let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
    let resubscribeTimeout: ReturnType<typeof setTimeout> | null = null;
    let subId = 1;
    let activeSubId: string | null = null;
    let connectionState: ConnectionState | null = null;

    const setConnectionState = (next: ConnectionState) => {
      if (next !== connectionState) {
        connectionState = next;
        onStateChange?.(next);
      }
    };

    const clearResubscribeTimeout = () => {
      if (resubscribeTimeout) {
        clearTimeout(resubscribeTimeout);
        resubscribeTimeout = null;
      }
    };

    const sendSubscribe = (socket: WebSocket) => {
      const id = String(subId++);
      activeSubId = id;
      socket.send(
        JSON.stringify({
          id,
          type: 'subscribe',
          payload: {
            query: 'subscription { tableUpdates }',
          },
        })
      );
    };

    const sendComplete = () => {
      if (activeSubId && ws && ws.readyState === WebSocket.OPEN) {
        const id = activeSubId;
        activeSubId = null;
        try {
          ws.send(JSON.stringify({id, type: 'complete'}));
        } catch {
          // Socket may already be closing during page unload
        }
      }
    };

    const closeConnection = () => {
      isClosed = true;
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
        reconnectTimeout = null;
      }
      clearResubscribeTimeout();
      sendComplete();
      if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        ws.close();
      }
      setConnectionState('disconnected');
    };

    const connect = () => {
      if (isClosed) return;
      try {
        const url = getSubscriptionUrl();
        const socket = new WebSocket(url, 'graphql-transport-ws');
        ws = socket;

        socket.onopen = () => {
          if (isClosed) {
            socket.close();
            return;
          }
          socket.send(JSON.stringify({type: 'connection_init'}));
        };

        socket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'connection_ack') {
              sendSubscribe(socket);
              setConnectionState('connected');
            } else if (data.type === 'next') {
              onReload();
            } else if (data.type === 'complete') {
              if (data.id === activeSubId) {
                activeSubId = null;
              }
            } else if (data.type === 'error') {
              console.warn('GraphQL subscription error:', data.payload);
              if (data.id === activeSubId) {
                activeSubId = null;
              }
              onReload();
              if (!isClosed && ws === socket && socket.readyState === WebSocket.OPEN) {
                clearResubscribeTimeout();
                resubscribeTimeout = setTimeout(() => {
                  resubscribeTimeout = null;
                  if (!isClosed && ws === socket && socket.readyState === WebSocket.OPEN) {
                    sendSubscribe(socket);
                  }
                }, 1000);
              }
            }
          } catch (e) {
            console.error('Error handling WebSocket message', e);
          }
        };

        socket.onclose = () => {
          if (ws === socket) {
            activeSubId = null;
            clearResubscribeTimeout();
            if (!isClosed) {
              setConnectionState('reconnecting');
              reconnectTimeout = setTimeout(connect, 3000);
            }
          }
        };

        socket.onerror = () => {
          // Handled via onclose
        };
      } catch {
        if (!isClosed) {
          setConnectionState('reconnecting');
          reconnectTimeout = setTimeout(connect, 3000);
        }
      }
    };

    if (typeof window !== 'undefined') {
      window.addEventListener('beforeunload', closeConnection);
      window.addEventListener('pagehide', closeConnection);
    }

    connect();

    return () => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('beforeunload', closeConnection);
        window.removeEventListener('pagehide', closeConnection);
      }
      closeConnection();
    };
  }
}
