import {ApolloClient, gql, DefaultOptions, ApolloQueryResult} from '@apollo/client/core';
import {InMemoryCache, NormalizedCacheObject} from '@apollo/client/cache';
import {Query} from './gql_types';

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
   * Load data via gql client.
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

  /**
   * Load summary table data via gql client.
   * @return promise of query result.
   */
  loadTable(owner: string): Promise<ApolloQueryResult<Query>> {
    return this.gqlClient.query<Query>({
      query: gql`
        query table($owner: String!) {
          table(owner: $owner) {
            currentOwner
            owners
            months
            rows {
              title
              account {
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
              isSpace
              isTotal
              isNormal
              cells {
                isClosed
                addSub
                balance
                isProjected
                isCovered
                isProjectedCovered
                hasProjectedTransfer
                percentChange
                unaccounted
                balanced
              }
            }
          }
        }
      `,
      variables: {
        owner,
      },
    });
  }

  /**
   * Load summary (popup) data data via gql client.
   * @return promise of query result.
   */
  loadSummaryData(
    owner: string,
    accountType: string,
    month: string
  ): Promise<ApolloQueryResult<Query>> {
    return this.gqlClient.query<Query>({
      query: gql`
        query summary($owner: String!, $accountType: String!, $month: GqlMonth!) {
          summary(owner: $owner, accountType: $accountType, month: $month) {
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
              accounts
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
        owner,
        accountType,
        month,
      },
    });
  }

  /**
   * Load data via gql client.
   * @return promise of query result.
   */
  loadStatement(owner: string, account: string, month: string): Promise<ApolloQueryResult<Query>> {
    return this.gqlClient.query<Query>({
      query: gql`
        query statement($owner: String!, $account: String!, $month: GqlMonth!) {
          statement(owner: $owner, account: $account, month: $month) {
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
              description
            }
          }
        }
      `,
      variables: {
        owner,
        account,
        month,
      },
    });
  }
}
