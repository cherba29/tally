this["jbudget"] = this["jbudget"] || {};
this["jbudget"]["templates"] = this["jbudget"]["templates"] || {};

this["jbudget"]["templates"]["account-tooltip"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, functionType="function", escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n     <a href=\""
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.url)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "\" target=\"_blank\">"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.name)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</a>\n   ";
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n     "
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.name)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "\n   ";
  return buffer;
  }

  buffer += "<div class=\"toolTip\">\n<span id=\"";
  if (helper = helpers.id) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.id); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "_close\">XXX</span>\n<table>\n<tr>\n  <td>Account</td>\n  <td>\n   ";
  stack1 = helpers['if'].call(depth0, ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.url), {hash:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n   </td>\n</tr>\n<tr>\n  <td>Description</td>\n  <td>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.description)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n</tr>\n<tr>\n  <td>Number</td>\n  <td>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.number)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n</tr>\n<tr>\n  <td>User Name</td>   \n  <td>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.userName)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n</tr>\n<tr>\n  <td>Password</td>\n  <td>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.password)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n</tr>\n<tr>\n  <td>Opened On</td>\n  <td>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.openedOn)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n</tr>\n<tr>\n  <td>Phone</td>\n  <td>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.phone)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n</tr>\n<tr>\n  <td>Address</td>\n  <td>"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.account)),stack1 == null || stack1 === false ? stack1 : stack1.address)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n</tr>\n</table>\n";
  return buffer;
  });

this["jbudget"]["templates"]["balance-summary-tooltip"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, functionType="function", escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n    <tr class=\"highlight\">\n      <td>"
    + escapeExpression(((stack1 = (data == null || data === false ? data : data.index)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + " ";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</td>\n      <td align=\"middle\">"
    + escapeExpression(((stack1 = ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.date)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n      <td align=\"right\"";
  stack1 = (helper = helpers.isProjected || (depth0 && depth0.isProjected),options={hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance), options) : helperMissing.call(depth0, "isProjected", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance), options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += ">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options) : helperMissing.call(depth0, "currency", ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options)))
    + "</td>\n      <td align=\"middle\">"
    + escapeExpression(((stack1 = ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.date)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n      <td align=\"right\"";
  stack1 = (helper = helpers.isProjected || (depth0 && depth0.isProjected),options={hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance), options) : helperMissing.call(depth0, "isProjected", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance), options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += ">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options) : helperMissing.call(depth0, "currency", ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.change), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.change), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.percentChange)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.inFlows), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.inFlows), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.outFlows), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.outFlows), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.addSub), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.addSub), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.income), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.income), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.totalPayments), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.totalPayments), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.totalTransfers), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.totalTransfers), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.unaccounted), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.unaccounted), options)))
    + "</td>\n    </tr>\n    ";
  return buffer;
  }
function program2(depth0,data) {
  
  
  return " class=\"projected\"";
  }

  buffer += "<div class=\"toolTip\">\n<span id=\"";
  if (helper = helpers.id) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.id); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "_close\">XXX</span>\n<span id=\"";
  if (helper = helpers.id) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.id); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "_transfer_matrix\">Transfer Matrix</span>\n<span style=\"float:right;\">";
  if (helper = helpers.accountName) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.accountName); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + " - ";
  if (helper = helpers.month) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.month); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</span>\n<table>\n  <thead>\n    <th style=\"min-width:170px\">Account</th>\n    <th style=\"min-width:60px\">Start Date</th>\n    <th style=\"min-width:50px\">Start<br>Balance</th>\n    <th style=\"min-width:60px\">End Date</th>\n    <th style=\"min-width:50px\">End<br>Balance</th>\n    <th style=\"min-width:50px\">Change</th>\n    <th style=\"min-width:50px\">Prct<br>Change</th>\n    <th style=\"min-width:50px\">Inflows</th>\n    <th style=\"min-width:50px\">OutFlows</th>\n    <th style=\"min-width:50px\">AddSub</th>\n    <th style=\"min-width:50px\">Income</th>\n    <th style=\"min-width:50px\">Payments</th>\n    <th style=\"min-width:50px\">Transfers</th>\n    <th style=\"min-width:50px\">Unaccounted</th>\n  </thead>\n  <tbody>\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.statements), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    <tr>\n      <td><b>Total</b></td>\n      <td align=\"middle\">"
    + escapeExpression(((stack1 = ((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.date)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n      <td align=\"right\"";
  stack1 = helpers['if'].call(depth0, ((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.projected), {hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += ">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options) : helperMissing.call(depth0, "currency", ((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options)))
    + "</td>\n      <td align=\"middle\">"
    + escapeExpression(((stack1 = ((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.date)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n      <td align=\"right\"";
  stack1 = helpers['if'].call(depth0, ((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.projected), {hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += ">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options) : helperMissing.call(depth0, "currency", ((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.change), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.change), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.percentChange)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.inFlows), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.inFlows), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.outFlows), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.outFlows), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.addSub), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.addSub), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.income), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.income), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.totalPayments), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.totalPayments), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.totalTransfers), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.totalTransfers), options)))
    + "</td>\n      <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.unaccounted), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.summary)),stack1 == null || stack1 === false ? stack1 : stack1.unaccounted), options)))
    + "</td>\n    </tr>\n  </tbody>\n</table>\n</div>\n";
  return buffer;
  });

this["jbudget"]["templates"]["balance-tooltip"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, self=this, functionType="function", escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n  <tr class=\"highlight\">\n    <td style=\"min-width:175px\">"
    + escapeExpression(((stack1 = (data == null || data === false ? data : data.index)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + " ";
  stack1 = (helper = helpers.isProjected || (depth0 && depth0.isProjected),options={hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data},helper ? helper.call(depth0, (depth0 && depth0.balance), options) : helperMissing.call(depth0, "isProjected", (depth0 && depth0.balance), options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  if (helper = helpers.toAccountName) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.toAccountName); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\n    <span style=\"font-size:80%\">";
  if (helper = helpers.description) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.description); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</span>\n    </td>\n    <td align=\"middle\" style=\"";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isExpense), {hash:{},inverse:self.program(6, program6, data),fn:self.program(4, program4, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\">";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isExpense), {hash:{},inverse:self.program(11, program11, data),fn:self.program(9, program9, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</td>\n    <td align=\"right\" style=\"min-width:40px\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.balance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.balance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options)))
    + "</td>\n    <td align=\"middle\" style=\"min-width:60px\">"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.balance)),stack1 == null || stack1 === false ? stack1 : stack1.date)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</td>\n    <td align=\"right\" style=\"min-width:50px\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.balanceFromEnd), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.balanceFromEnd), options)))
    + "</td>\n    <td align=\"right\" style=\"min-width:50px\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.balanceFromStart), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.balanceFromStart), options)))
    + "</td>\n  </tr>\n  ";
  return buffer;
  }
function program2(depth0,data) {
  
  
  return "**";
  }

function program4(depth0,data) {
  
  
  return "background-color:#caa\n      ";
  }

function program6(depth0,data) {
  
  var stack1;
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isIncome), {hash:{},inverse:self.noop,fn:self.program(7, program7, data),data:data});
  if(stack1 || stack1 === 0) { return stack1; }
  else { return ''; }
  }
function program7(depth0,data) {
  
  
  return "background-color:#aca\n      ";
  }

function program9(depth0,data) {
  
  
  return "E";
  }

function program11(depth0,data) {
  
  var stack1;
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isIncome), {hash:{},inverse:self.program(14, program14, data),fn:self.program(12, program12, data),data:data});
  if(stack1 || stack1 === 0) { return stack1; }
  else { return ''; }
  }
function program12(depth0,data) {
  
  
  return "I";
  }

function program14(depth0,data) {
  
  
  return "T";
  }

  buffer += "<div class=\"toolTip\">\n<span id=\"";
  if (helper = helpers.id) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.id); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "_close\">XXX</span>\n<span style=\"float:right;\">";
  if (helper = helpers.accountName) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.accountName); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + " - ";
  if (helper = helpers.month) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.month); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</span>\n<table>\n  <tr>\n    <th align=\"left\">Ending Balance</th>\n    <th>Type</th>\n    <th></th>\n    <th>"
    + escapeExpression(((stack1 = ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.date)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</th>\n    <th align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options) : helperMissing.call(depth0, "currency", ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.endBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options)))
    + "</th>\n    <th></th>\n  </tr>\n  ";
  stack1 = helpers.each.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.transactions), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  <tr>\n    <th align=\"left\">Starting Balance</td>\n    <th></th>\n    <th></th>\n    <th>"
    + escapeExpression(((stack1 = ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.date)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</th>\n    <th></th>\n    <th align=\"right\" style=\"width:50px\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options) : helperMissing.call(depth0, "currency", ((stack1 = ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.startBalance)),stack1 == null || stack1 === false ? stack1 : stack1.amount), options)))
    + "</th>\n  </tr>\n  <tr>\n    <td align=\"right\"><b>Income</b></td>\n    <td></td>\n    <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.income), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.income), options)))
    + "</td>\n    <td></td>\n    <td></td>\n    <td></td>\n  </tr>\n  <tr>\n    <td align=\"right\"><b>Payments</b></td>\n    <td></td>\n    <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.totalPayments), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.totalPayments), options)))
    + "</td>\n    <td></td>\n    <td></td>\n    <td></td>\n  </tr>\n  <tr>\n    <td align=\"right\"><b>Transfers</b></td>\n    <td></td>\n    <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.totalTransfers), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.totalTransfers), options)))
    + "</td>\n    <td></td>\n    <td></td>\n    <td></td>\n  </tr>\n  <tr>\n    <td align=\"right\"><b>Inflows</b></td>\n    <td></td>\n    <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.inFlows), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.inFlows), options)))
    + "</td>\n    <td></td>\n    <td></td>\n    <td></td>\n  </tr>\n  <tr>\n    <td align=\"right\"><b>Outflows</b></td>\n    <td></td>\n    <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.outFlows), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.outFlows), options)))
    + "</td>\n    <td></td>\n    <td></td>\n    <td></td>\n  </tr>\n  <tr>\n    <td align=\"right\"><b>Total</b></td>\n    <td></td>\n    <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.addSub), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.addSub), options)))
    + "</td>\n    <td></td>\n    <td></td>\n    <td></td>\n  </tr>\n  <tr>\n    <td align=\"right\"><b>Unaccounted</b></td>\n    <td></td>\n    <td align=\"right\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.unaccounted), options) : helperMissing.call(depth0, "currency", ((stack1 = (depth0 && depth0.stmt)),stack1 == null || stack1 === false ? stack1 : stack1.unaccounted), options)))
    + "</td>\n    <td></td>\n    <td></td>\n    <td></td>\n  </tr>\n</table>\n</div>\n";
  return buffer;
  });

this["jbudget"]["templates"]["summary"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, functionType="function", escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = "";
  buffer += "\n    <th colspan=\"4\">"
    + escapeExpression((typeof depth0 === functionType ? depth0.apply(depth0) : depth0))
    + "</th>\n    ";
  return buffer;
  }

function program3(depth0,data) {
  
  
  return "\n    <th>+/-<th>$Bal<th>Chg%<th>?</th>\n    ";
  }

function program5(depth0,data,depth1) {
  
  var buffer = "", stack1;
  buffer += "\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isSpace), {hash:{},inverse:self.noop,fn:self.programWithDepth(6, program6, data, depth1),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "  \n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isTotal), {hash:{},inverse:self.noop,fn:self.program(9, program9, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isNormal), {hash:{},inverse:self.noop,fn:self.program(20, program20, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  ";
  return buffer;
  }
function program6(depth0,data,depth2) {
  
  var buffer = "", stack1, helper;
  buffer += "\n  <tr>\n    <td class=\"account_type\">";
  if (helper = helpers.title) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.title); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</td>\n    ";
  stack1 = helpers.each.call(depth0, (depth2 && depth2.months), {hash:{},inverse:self.noop,fn:self.program(7, program7, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </tr>\n  ";
  return buffer;
  }
function program7(depth0,data) {
  
  
  return "\n    <td colspan=\"4\" style=\"border-right:2px double #a00\"></td>\n    ";
  }

function program9(depth0,data) {
  
  var buffer = "", stack1, helper;
  buffer += "\n  <tr>\n    <td><b>";
  if (helper = helpers.title) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.title); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "/Total</b></td>\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.cells), {hash:{},inverse:self.noop,fn:self.program(10, program10, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </tr>\n  ";
  return buffer;
  }
function program10(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n      ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isClosed), {hash:{},inverse:self.program(13, program13, data),fn:self.program(11, program11, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    ";
  return buffer;
  }
function program11(depth0,data) {
  
  
  return "\n      <td colspan=\"4\" class=\"closed\" style=\"border-right:2px double #a00\"></td>\n      ";
  }

function program13(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n      <td class=\"add_sub\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.addSub), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.addSub), options)))
    + "</td>\n      <td id=\"";
  if (helper = helpers.id) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.id); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\" style=\"font-weight:700;font-size:75%;\"\n          class=\"balance ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isProjected), {hash:{},inverse:self.noop,fn:self.program(14, program14, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.balance), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.balance), options)))
    + "</td>\n      <td class=\"change\">";
  if (helper = helpers.percentChange) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.percentChange); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</td>\n      <td class=\"";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.balanced), {hash:{},inverse:self.program(18, program18, data),fn:self.program(16, program16, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\"\n          style=\"border-right:2px double #a00\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.unaccounted), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.unaccounted), options)))
    + "</td>\n      ";
  return buffer;
  }
function program14(depth0,data) {
  
  
  return "projected";
  }

function program16(depth0,data) {
  
  
  return "accounted";
  }

function program18(depth0,data) {
  
  
  return "unaccounted";
  }

function program20(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n  <tr>\n    <td id=\""
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.title)),stack1 == null || stack1 === false ? stack1 : stack1.name)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "\">";
  stack1 = helpers['if'].call(depth0, ((stack1 = (depth0 && depth0.title)),stack1 == null || stack1 === false ? stack1 : stack1.url), {hash:{},inverse:self.program(23, program23, data),fn:self.program(21, program21, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "</td>\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.cells), {hash:{},inverse:self.noop,fn:self.program(25, program25, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </tr>\n  ";
  return buffer;
  }
function program21(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "<a href=\""
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.title)),stack1 == null || stack1 === false ? stack1 : stack1.url)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "\" target=\"_blank\">"
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.title)),stack1 == null || stack1 === false ? stack1 : stack1.name)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</a>\n    ";
  return buffer;
  }

function program23(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n      "
    + escapeExpression(((stack1 = ((stack1 = (depth0 && depth0.title)),stack1 == null || stack1 === false ? stack1 : stack1.name)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "\n    ";
  return buffer;
  }

function program25(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n      ";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isClosed), {hash:{},inverse:self.program(26, program26, data),fn:self.program(11, program11, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    ";
  return buffer;
  }
function program26(depth0,data) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n      <td class=\"add_sub\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.addSub), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.addSub), options)))
    + "</td>\n      <td id=\"";
  if (helper = helpers.id) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.id); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "\" class=\"balance";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isProjected), {hash:{},inverse:self.noop,fn:self.program(27, program27, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\">";
  stack1 = helpers.unless.call(depth0, (depth0 && depth0.isCovered), {hash:{},inverse:self.noop,fn:self.program(29, program29, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.hasProjectedTransfer), {hash:{},inverse:self.noop,fn:self.program(34, program34, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.balance), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.balance), options)));
  stack1 = helpers.unless.call(depth0, (depth0 && depth0.isCovered), {hash:{},inverse:self.noop,fn:self.program(36, program36, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      </td>\n      <td class=\"change\">";
  if (helper = helpers.percentChange) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.percentChange); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</td>\n      <td class=\"";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.balanced), {hash:{},inverse:self.program(18, program18, data),fn:self.program(16, program16, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\"\n          style=\"border-right:2px double #a00\">"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.unaccounted), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.unaccounted), options)))
    + "</td>\n      ";
  return buffer;
  }
function program27(depth0,data) {
  
  
  return " projected";
  }

function program29(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "<span style=\"color:";
  stack1 = helpers['if'].call(depth0, (depth0 && depth0.isProjectedCovered), {hash:{},inverse:self.program(32, program32, data),fn:self.program(30, program30, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += ";font-weight:700;\">";
  return buffer;
  }
function program30(depth0,data) {
  
  
  return "#fa0";
  }

function program32(depth0,data) {
  
  
  return "#f00";
  }

function program34(depth0,data) {
  
  
  return "**";
  }

function program36(depth0,data) {
  
  
  return "</span>";
  }

  buffer += "<table class=\"main\">\n<thead>\n  <tr>\n    <th>Account</th>\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.months), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </tr>\n  <tr>\n    <th></th>\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.months), {hash:{},inverse:self.noop,fn:self.program(3, program3, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n  </tr>\n</thead>\n<tbody>\n  ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.rows), {hash:{},inverse:self.noop,fn:self.programWithDepth(5, program5, data, depth0),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n</tbody>\n</table>\n";
  return buffer;
  });

this["jbudget"]["templates"]["transfer-matrix"] = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, helper, options, functionType="function", escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = "", stack1;
  buffer += "\n    <th>"
    + escapeExpression(((stack1 = (data == null || data === false ? data : data.index)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</th>\n    ";
  return buffer;
  }

function program3(depth0,data,depth1) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n    <tr class=\"highlight\">\n      <td>"
    + escapeExpression(((stack1 = (data == null || data === false ? data : data.index)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + " ";
  if (helper = helpers.name) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.name); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "</td>\n      ";
  stack1 = helpers.each.call(depth0, (depth1 && depth1.accounts), {hash:{},inverse:self.noop,fn:self.programWithDepth(4, program4, data, depth0),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n      <td>"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.total), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.total), options)))
    + "</td>\n   </tr>\n   ";
  return buffer;
  }
function program4(depth0,data,depth1) {
  
  var buffer = "", stack1, helper, options;
  buffer += "\n      <td ";
  stack1 = (helper = helpers.eqname || (depth1 && depth1.eqname),options={hash:{},inverse:self.noop,fn:self.program(5, program5, data),data:data},helper ? helper.call(depth0, (depth1 && depth1.name), (depth0 && depth0.name), options) : helperMissing.call(depth0, "eqname", (depth1 && depth1.name), (depth0 && depth0.name), options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  stack1 = (helper = helpers.isNotSymetric || (depth1 && depth1.isNotSymetric),options={hash:{},inverse:self.noop,fn:self.program(7, program7, data),data:data},helper ? helper.call(depth0, (depth1 && depth1.transfers), (depth0 && depth0.transfers), (depth1 && depth1.name), (depth0 && depth0.name), options) : helperMissing.call(depth0, "isNotSymetric", (depth1 && depth1.transfers), (depth0 && depth0.transfers), (depth1 && depth1.name), (depth0 && depth0.name), options));
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += ">"
    + escapeExpression((helper = helpers.currencyitem || (depth1 && depth1.currencyitem),options={hash:{},data:data},helper ? helper.call(depth0, (depth1 && depth1.transfers), (depth0 && depth0.name), options) : helperMissing.call(depth0, "currencyitem", (depth1 && depth1.transfers), (depth0 && depth0.name), options)))
    + "</td>\n      ";
  return buffer;
  }
function program5(depth0,data) {
  
  
  return "style=\"background:#ccc\"";
  }

function program7(depth0,data) {
  
  
  return "style=\"background:#c88\"";
  }

function program9(depth0,data) {
  
  var buffer = "", helper, options;
  buffer += "\n      <td>"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.total), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.total), options)))
    + "</td>\n    ";
  return buffer;
  }

  buffer += "<div class=\"toolTip\">\n<span id=\"";
  if (helper = helpers.id) { stack1 = helper.call(depth0, {hash:{},data:data}); }
  else { helper = (depth0 && depth0.id); stack1 = typeof helper === functionType ? helper.call(depth0, {hash:{},data:data}) : helper; }
  buffer += escapeExpression(stack1)
    + "_close\">XXX</span>\n<table>\n  <thead>\n    <th>Accounts</th>\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.accounts), {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    <th>Totals</th>\n  </thead>\n  <tbody>\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.accounts), {hash:{},inverse:self.noop,fn:self.programWithDepth(3, program3, data, depth0),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n   <tr>\n     <td>Totals</td>\n    ";
  stack1 = helpers.each.call(depth0, (depth0 && depth0.accounts), {hash:{},inverse:self.noop,fn:self.program(9, program9, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n    <td>"
    + escapeExpression((helper = helpers.currency || (depth0 && depth0.currency),options={hash:{},data:data},helper ? helper.call(depth0, (depth0 && depth0.total), options) : helperMissing.call(depth0, "currency", (depth0 && depth0.total), options)))
    + "</td>\n   </tr>\n</table>\n</div>\n";
  return buffer;
  });