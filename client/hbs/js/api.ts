import {ApolloClient, gql, DefaultOptions, ApolloQueryResult} from '@apollo/client/core';
import {InMemoryCache, NormalizedCacheObject} from '@apollo/client/cache';
import {Query} from '@backend/types';

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

/**
 * Gql Backend Client.
 */
export class BackendClient {
  private readonly gqlCache: InMemoryCache = new InMemoryCache({});
  private readonly gqlClient: ApolloClient<NormalizedCacheObject> = new ApolloClient({
    cache: this.gqlCache,
    uri: 'http://localhost:4000/graphql',
    defaultOptions,
  });

  /**
   * Load data via gql cleint.
   * @return promise of query result.
   */
  loadData(): Promise<ApolloQueryResult<Query>> {
    return this.gqlClient.query<Query>({
      query: gql`
        query {
          budget {
            accounts {
              name
              description
              type
              openedOn
              closedOn
              number
              owners
              address
              external
              summary
              userName
              password
              phone
              url
            }
            months
            statements {
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
                balanceFromEnd
                description
              }
            }
            summaries {
              name
              month
              accounts
              addSub
              change
              inFlows
              outFlows
              percentChange
              income
              totalPayments
              totalTransfers
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
            }
          }
        }
      `,
    });
  }
}
