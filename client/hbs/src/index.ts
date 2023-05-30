import './style.css';

import * as $ from 'jquery';

import {BackendClient} from './api';
import {MatrixDataView, PopupData} from './utils';
import {JettyResponse, transformJettyBudgetData} from './jetty_utils';
import {transformGqlBudgetData} from './gql_utils';
import {AccountTooltip} from './account-tooltip';
import {BalanceTooltip} from './balance-tooltip';
import {BalanceSummaryTooltip} from './balance-summary-tooltip';
import {SummaryTable} from './summary-table';

/** Reset popup. */
function clearPopup(): void {
  $('#popup-content').html('').css('display', 'none');
}

function clearErrorContent(): void {
  $('#error-content').html('');
}

function createPopup(xOffset: number, yOffset: number, content: PopupData) {
  console.log('popup content', content);
  const popupElement = $('#popup-content');
  popupElement.html('').css('display', 'inline');
  popupElement.offset({top: yOffset + 10, left: xOffset});
  if ('summary' in content) {
    const balanceSummaryTooltip = new BalanceSummaryTooltip();
    balanceSummaryTooltip.accountName = content.accountName;
    balanceSummaryTooltip.month = content.month;
    balanceSummaryTooltip.statementEntries = content.statements || [];
    balanceSummaryTooltip.summary = content.summary;
    balanceSummaryTooltip.onCloseButton = clearPopup;
    popupElement.append(balanceSummaryTooltip);
  } else if ('account' in content) {
    const accountTooltip = new AccountTooltip();
    accountTooltip.account = content.account;
    accountTooltip.onCloseButton = clearPopup;
    popupElement.append(accountTooltip);
  } else if ('stmt' in content) {
    const balanceTooltip = new BalanceTooltip();
    balanceTooltip.accountName = content.accountName;
    balanceTooltip.month = content.month;
    balanceTooltip.stmt = content.stmt;
    balanceTooltip.onCloseButton = clearPopup;
    popupElement.append(balanceTooltip);
  }
}

let lastReloadTimestamp = new Date();

function renderSummaryTable(dataView: MatrixDataView) {
  const popupMap = new Map(dataView.popupCells.map((c) => [c.id, c]));
  const summaryTable = new SummaryTable();
  summaryTable.months = dataView.months;
  summaryTable.rows = dataView.rows;
  summaryTable.onCellClick = (e: MouseEvent, id: string) => {
    const popupContent = popupMap.get(id);
    if (popupContent) {
      createPopup(e.clientX, e.clientY, popupContent);
    } else {
      throw new Error(`Popup data for ${id} not found.`);
    }
  };
  $('#content').html('').append(summaryTable);
}

/** Loads json and re-renders the page. */
function reload(): void {
  const path = '/budget?dir=' + window.location.hash.substring(1);
  console.log('Loading ', path);
  $.getJSON(path, (response: JettyResponse): void => {
    lastReloadTimestamp = new Date();
    console.log('server response', response);
    if (!response.success) {
      const popupElement = $('#error-content');
      popupElement.offset({top: 40, left: 0});
      popupElement.html('<pre>' + response.message + '</pre>');
      return;
    }
    clearPopup();
    clearErrorContent();

    // Convert data so it can be rendered.
    const dataView = transformJettyBudgetData(response.data);
    renderSummaryTable(dataView);
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
      lastReloadTimestamp = new Date();
      console.log(result);
      if (result.errors) {
        const popupElement = $('#error-content');
        popupElement.offset({top: 40, left: 0});
        for (const error of result.errors) {
          popupElement.html('<pre>' + error.message + '</pre>');
        }
        return;
      }
      clearPopup();
      clearErrorContent();

      const dataView = transformGqlBudgetData(result.data.budget || undefined);
      renderSummaryTable(dataView);
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

function pad(n: number) {
  return n > 9 ? n.toString() : '0' + n.toString();
}

// Show how long ago page was reloaded.
setInterval(() => {
  const minutesElement = $('#minutes');
  const secondsElement = $('#seconds');
  const diffTimeSec = Math.round((new Date().getTime() - lastReloadTimestamp.getTime()) / 1000);
  secondsElement.html(pad(diffTimeSec % 60));
  minutesElement.html(pad(Math.floor(diffTimeSec / 60)));
}, 1000);
