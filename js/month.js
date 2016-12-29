goog.provide('budget.Month');

goog.scope(function() {

/**
 * @param {number=} year Year for the month.
 * @param {number=} month Month [0-11] for this month instance. 
 * @final
 * @constructor
 */
budget.Month = function(year, month) {

};
var Account = budget.Account;

/**
 * @type {string}
 * @private
 */
Account.name_ = null;

/**
 * @type {string}
 * @private
 */
Account.description_ = null;

/**
 * @type {string}
 * @private
 */
Account.number_ = null;
Account.owners_ = [];

/**
 * @type {Month}
 * @private
 */
Account.openedOn_ = null;

/**
 * @type {Month}
 * @private
 */
Account.closedOn_ = null;

/**
 * @type {string}
 * @private
 */
Account.url_ = null;

/**
 * @type {string}
 * @private
 */
Account.userName_ = null;

/**
 * @type {string}
 * @private
 */
Account.password_ = null;

/**
 * @type {string}
 * @private
 */
Account.phone_ = null;

/**
 * @type {string}
 * @private
 */
Account.address_ = null;

});  // goog.scope

goog.exportSymbol('budget.Account', budget.Account);
