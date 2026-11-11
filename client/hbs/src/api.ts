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

const link = ApolloLink.from([
  new HttpLink({ uri: "http://localhost:8080/graphql" }),
]);

const getSubscriptionUrl = (): string => {
  if (typeof window !== 'undefined' && window.location) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const hostname = window.location.hostname || 'localhost';
    const port =
      window.location.port && window.location.port !== '9000' && window.location.port !== '8000'
        ? window.location.port
        : '8080';
    return `${protocol}//${hostname}:${port}/subscriptions`;
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
    let subId = 1;

    const connect = () => {
      if (isClosed) return;
      try {
        const url = getSubscriptionUrl();
        ws = new WebSocket(url, 'graphql-transport-ws');

        ws.onopen = () => {
          if (isClosed) {
            ws?.close();
            return;
          }
          ws?.send(JSON.stringify({type: 'connection_init'}));
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'connection_ack') {
              ws?.send(
                JSON.stringify({
                  id: String(subId++),
                  type: 'subscribe',
                  payload: {
                    query: 'subscription { tableUpdates }',
                  },
                })
              );
            } else if (data.type === 'next') {
              onReload();
            } else if (data.type === 'error') {
              console.warn('GraphQL subscription error:', data.payload);
              onReload();
              if (!isClosed && ws && ws.readyState === WebSocket.OPEN) {
                setTimeout(() => {
                  if (!isClosed && ws && ws.readyState === WebSocket.OPEN) {
                    ws.send(
                      JSON.stringify({
                        id: String(subId++),
                        type: 'subscribe',
                        payload: {
                          query: 'subscription { tableUpdates }',
                        },
                      })
                    );
                  }
                }, 1000);
              }
            }
          } catch (e) {
            console.error('Error handling WebSocket message', e);
          }
        };

        ws.onclose = () => {
          if (!isClosed) {
            reconnectTimeout = setTimeout(connect, 3000);
          }
        };

        ws.onerror = () => {
          // Handled via onclose
        };
      } catch (err) {
        if (!isClosed) {
          reconnectTimeout = setTimeout(connect, 3000);
        }
      }
    };

    connect();

    return () => {
      isClosed = true;
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
      if (ws) {
        ws.close();
      }
    };
  }
}
