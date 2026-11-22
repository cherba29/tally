import {ApolloClient, gql, type DefaultOptions, HttpLink, ApolloLink} from '@apollo/client/core';
import {InMemoryCache} from '@apollo/client/cache';
import { type Query} from './gql_types';

const defaultOptions: DefaultOptions = {
  watchQuery: {
    fetchPolicy: 'no-cache',
    errorPolicy: 'ignore',
  },
  query: {
    fetchPolicy: 'no-cache',
    errorPolicy: 'all',
  },
};

const getGraphqlUrl = (): string => {
  if (typeof window !== 'undefined' && window.location && window.location.origin !== 'null') {
    return `${window.location.origin}/graphql`;
  }
  return 'http://localhost:8080/graphql';
};

const link = ApolloLink.from([
  new HttpLink({uri: getGraphqlUrl()}),
]);

const getSubscriptionUrl = (): string => {
  if (typeof window !== 'undefined' && window.location && window.location.host) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/subscriptions`;
  }
  return 'ws://localhost:8080/subscriptions';
};

/**
 * Gql Backend Client.
 */
export class BackendClient {
  private readonly gqlCache: InMemoryCache = new InMemoryCache({});
  private readonly gqlClient = new ApolloClient({
    cache: this.gqlCache,
    link,
    defaultOptions,
  });

  /**
   * Load summary table data via gql client.
   * @return promise of query result.
   */
  loadTable(
    owner: string,
    startMonth: string,
    endMonth: string
  ): Promise<ApolloClient.QueryResult<Query>> {
    console.log(`### loadTable owner=${owner} ${startMonth}-${endMonth}`);
    return this.gqlClient.query<Query>({
      query: gql`
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
      variables: {
        owner,
        startMonth,
        endMonth,
      },
    });
  }

  /**
   * Load summary (popup) data data via gql client.
   * @return promise of query result.
   */
  loadSummaryData(
    accountPath: string,
    startMonth: string | undefined,
    endMonth: string
  ): Promise<ApolloClient.QueryResult<Query>> {
    console.log(`### loadSummaryData owner=${accountPath} ${startMonth}-${endMonth}`);
    return this.gqlClient.query<Query>({
      query: gql`
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
      variables: {
        accountPath,
        startMonth,
        endMonth,
      },
    });
  }

  /**
   * Load summary (popup) data data via gql client.
   * @return promise of query result.
   */
  loadTransferSummaryData(
    accountPath: string,
    startMonth: string | undefined,
    endMonth: string
  ): Promise<ApolloClient.QueryResult<Query>> {
    return this.gqlClient.query<Query>({
      query: gql`
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
      variables: {
        accountPath,
        startMonth,
        endMonth,
      },
    });
  }

  /**
   * Load data via gql client.
   * @return promise of query result.
   */
  loadStatement(accountPath: string, month: string): Promise<ApolloClient.QueryResult<Query>> {
    return this.gqlClient.query<Query>({
      query: gql`
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
            }
          }
        }
      `,
      variables: {
        accountPath,
        month,
      },
    });
  }

  /**
   * Subscribes to table updates from the GraphQL backend.
   * Calls onReload callback whenever data is reloaded.
   * Returns an unsubscribe function.
   */
  subscribeToUpdates(onReload: () => void): () => void {
    if (typeof WebSocket === 'undefined') {
      return () => {};
    }

    let isClosed = false;
    let ws: WebSocket | null = null;
    let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
    let resubscribeTimeout: ReturnType<typeof setTimeout> | null = null;
    let subId = 1;
    let activeSubId: string | null = null;

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
          }
          if (!isClosed) {
            reconnectTimeout = setTimeout(connect, 3000);
          }
        };

        socket.onerror = () => {
          // Handled via onclose
        };
      } catch {
        if (!isClosed) {
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
