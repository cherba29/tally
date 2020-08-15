import './style.css';

import * as $ from 'jquery';
import * as account_tooltip from 'templates/account-tooltip.hbs';
import * as balance_summary_tooltip from 'templates/balance-summary-tooltip.hbs';
import * as balance_tooltip from 'templates/balance-tooltip.hbs';
import * as summary_template from 'templates/summary.hbs';

import {transformBudgetData, PopupData} from './utils';

function clearPopup() {
  $('#popup-content').html("");
}

// Function to execute to display summary details popup.
function createPopupFunc(popup: PopupData) {
  return function(e: JQuery.Event) {
    console.log("popup", popup);
    const popupElement = $('#popup-content');
    popupElement.offset({ top: e.pageY + 10, left: e.pageX });
    if ('summary' in popup) {
      popupElement.html(balance_summary_tooltip(popup));
    } else if ('account' in popup) {
      popupElement.html(account_tooltip(popup));
    } else {
      popupElement.html(balance_tooltip(popup));
    }
    $("#" + popup.id + "_close").click(clearPopup);
  };
}

// Loads json and re-renders the page.
function reload() {
  const path = "/budget?dir=" + window.location.hash.substring(1);
  console.log("Loading ", path);
  $.getJSON(path, function(response) {
    console.log("server response", response)
    if (!response.success) {
      const popupElement = $('#popup-content');
      popupElement.offset({ top: 40, left: 0 });
      popupElement.html("<pre>" + response.message + "</pre>");
      return;
    }
    clearPopup();

    // Convert data so it can be rendered.
    const data = response.data;
    const dataView = transformBudgetData(
        data.months, data.accountNameToAccount, data.statements, data.summaries);
    $('#content').html(summary_template(dataView));
    // Most cells have popups with details, activate these.
    for (let popup of dataView.popupCells) {
      $('#' + popup.id).click(createPopupFunc(popup));
    }
  }).fail(function(jqxhr, textStatus, error) {
    let err = textStatus + ", " + error;
    console.log( "Request Failed: " + err );
  });
}

$("#reload").on('click', reload);

// Trigger reload on first load.
reload();
