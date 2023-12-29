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
   * Load summary table data via gql client.
   * @return promise of query result.
   */
  loadTable(
    owner: string,
    startMonth: string,
    endMonth: string
  ): Promise<ApolloQueryResult<Query>> {
    console.log('### ', owner, startMonth, endMonth);
    return this.gqlClient.query<Query>({
      query: gql`
        query table($owner: String!, $startMonth: GqlMonth!, $endMonth: GqlMonth!) {
          table(owner: $owner, startMonth: $startMonth, endMonth: $endMonth) {
            currentOwner
            owners
            months
            rows {
              title
              account {
                name
                description
                path
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
    owner: string,
    accountType: string,
    startMonth: string | undefined,
    endMonth: string
  ): Promise<ApolloQueryResult<Query>> {
    return this.gqlClient.query<Query>({
      query: gql`
        query summary(
          $owner: String!
          $accountType: String!
          $startMonth: GqlMonth
          $endMonth: GqlMonth!
        ) {
          summary(
            owner: $owner
            accountType: $accountType
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
        owner,
        accountType,
        startMonth,
        endMonth,
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
        owner,
        account,
        month,
      },
    });
  }
}
