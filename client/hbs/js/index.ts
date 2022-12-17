import './style.css';

import * as $ from 'jquery';
/* eslint-disable camelcase */
import * as account_tooltip from 'templates/account-tooltip.hbs';
import * as balance_summary_tooltip from 'templates/balance-summary-tooltip.hbs';
import * as balance_tooltip from 'templates/balance-tooltip.hbs';
import * as summary_template from 'templates/summary.hbs';
/* eslint-enable camelcase */

import {BackendClient} from './api';
import {PopupData} from './utils';
import {JettyResponse, transformJettyBudgetData} from './jetty_utils';
import {transformGqlBudgetData} from './gql_utils';

/** Reset popup. */
function clearPopup(): void {
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
function reload(): void {
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
    console.log(`Request Failed: ${textStatus}, ${error}`);
  });
}

const backendClient = new BackendClient();

/** Loads graphql version of data and re-renders the page. */
function reloadGql(): void {
  console.log('Loading graphql');
  backendClient
      .loadData()
      .then((result) => {
        clearPopup();
        console.log(result);
        const dataView = transformGqlBudgetData(result.data.budget || undefined);
        $('#content').html(summary_template(dataView));
        // Most cells have popups with details, activate these.
        for (const popup of dataView.popupCells) {
          $('#' + popup.id).click(createPopupFunc(popup));
        }
      })
      .catch((error) => {
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
