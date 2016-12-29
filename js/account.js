goog.provide('budget.Account');

goog.require('budget.Month');

goog.scope(function() {

/**
 * @final
 * @constructor
 */
budget.Account = function() {

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
