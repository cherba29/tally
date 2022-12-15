import './style.css';

import {ApolloClient, gql, DefaultOptions} from '@apollo/client/core';
import {InMemoryCache, NormalizedCacheObject} from '@apollo/client/cache';
import {Query} from '@backend/types';
import * as $ from 'jquery';
/* eslint-disable camelcase */
import * as account_tooltip from 'templates/account-tooltip.hbs';
import * as balance_summary_tooltip
  from 'templates/balance-summary-tooltip.hbs';
import * as balance_tooltip from 'templates/balance-tooltip.hbs';
import * as summary_template from 'templates/summary.hbs';
/* eslint-enable camelcase */

import {transformGqlBudgetData, PopupData} from './utils';
import { JettyResponse, transformJettyBudgetData } from './jetty_utils';

/** Reset popup. */
function clearPopup() {
  $('#popup-content').html('');
}

/** Function to execute to display summary details popup.
 * @param {PopupData} popup data to display
 * @return {function(JQuery.Event): void} popup rendering function.
 */
function createPopupFunc(popup: PopupData) {
  return (e: JQuery.Event): void => {
    console.log('popup', popup);
    const popupElement = $('#popup-content');
    popupElement.offset({top: (e.pageY ?? 0) + 10, left: e.pageX});
    if ('summary' in popup) {
      popupElement.html(balance_summary_tooltip(popup));
    } else if ('account' in popup) {
      popupElement.html(account_tooltip(popup));
    } else {
      popupElement.html(balance_tooltip(popup));
    }
    $('#' + popup.id + '_close').click(clearPopup);
  };
}

/** Loads json and re-renders the page. */
function reload() {
  const path = '/budget?dir=' + window.location.hash.substring(1);
  console.log('Loading ', path);
  $.getJSON(path, (response: JettyResponse): void => {
    console.log('server response', response);
    if (!response.success) {
      const popupElement = $('#popup-content');
      popupElement.offset({top: 40, left: 0});
      popupElement.html('<pre>' + response.message + '</pre>');
      return;
    }
    clearPopup();

    // Convert data so it can be rendered.
    const dataView = transformJettyBudgetData(response.data);
    $('#content').html(summary_template(dataView));
    // Most cells have popups with details, activate these.
    for (const popup of dataView.popupCells) {
      $('#' + popup.id).click(createPopupFunc(popup));
    }
  }).fail((jqxhr, textStatus, error): void => {
    console.log( `Request Failed: ${textStatus}, ${error}`);
  });
}

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

const gqlCache: InMemoryCache = new InMemoryCache({});
const gqlClient: ApolloClient<NormalizedCacheObject> = new ApolloClient({
  cache: gqlCache,
  uri: 'http://localhost:4000/graphql',
  defaultOptions,
});

// function JSONstringifyOrder(obj: {}, space: number) {
//   var allKeys: Array<string> = [];
//   var seen: {[key:string]: null} = {};
//   JSON.stringify(obj, function (key, value) {
//     if (!(key in seen)) {
//       allKeys.push(key);
//       seen[key] = null;
//     }
//     return value;
//   });
//   allKeys.sort();
//   return JSON.stringify(obj, allKeys, space);
// }

/** Loads graphql version of data and re-renders the page. */
function reloadGql() {
  console.log('Loading graphql');
  gqlClient
      .query<Query>({
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
          }`,
      }).then((result) => {
        clearPopup();
        console.log(result);
        // const newData = transformGqlBudgetData(result.data.budget);
        // const path = '/budget?dir=' + window.location.hash.substring(1);
        // $.getJSON(path, (response): void => {
        //   console.log('server response', response)
        //   if (!response.success) {
        //     const popupElement = $('#popup-content');
        //     popupElement.offset({ top: 40, left: 0 });
        //     popupElement.html('<pre>' + response.message + '</pre>');
        //     return;
        //   }
        //   const oldData = response.data;
        //   console.log('OLD', JSONstringifyOrder(oldData, 2));
        //   console.log('NEW', JSONstringifyOrder(newData, 2));
        // });
        const dataView = transformGqlBudgetData(result.data.budget || undefined);
        $('#content').html(summary_template(dataView));
        // Most cells have popups with details, activate these.
        for (const popup of dataView.popupCells) {
          $('#' + popup.id).click(createPopupFunc(popup));
        }
      }).catch((error) => {
        const popupElement = $('#popup-content');
        popupElement.offset({top: 40, left: 0});
        popupElement.html('<pre>' + error.message + '</pre>');
        throw error;
      });
}

$('#reload').on('click', reload);
$('#reload-gql').on('click', reloadGql);

// Trigger reload on first load.
reloadGql();
