goog.provide('budget.summary');
goog.provide('budget.summary.Loader');


goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.json');
goog.require('goog.net.EventType');
goog.require('goog.net.XhrIo');

goog.scope(function() {

budget.summary.Loader = function(budgetDataBuilder) {
  this.budgetDataBuilder_ = budgetDataBuilder;
}
var Loader = budget.summary.Loader;


/**
 * Loads json content as specified application path.
 * @param {string} contentPath path to budget data served by backend.
 * @param {function()}
 */
Loader.prototype.load = function(contentPath, fn) {
  var request = new goog.net.XhrIo();
  goog.events.listen(request, goog.net.EventType.COMPLETE, function() {
    if (request.isSuccess()) {
      console.log("Success");
      var data = request.getResponseJson();
      console.log("response", data);
      fn(data);
    } else {
      console.log("response error");
    }
  });
  request.send(
      '/budget?dir=' + contentPath,
      'GET',
      goog.json.serialize({}), 
      {'content-type':'application/json'});
}

budget.summary.hello = function() {
  var newDiv = goog.dom.createDom('h1', {'style': 'background-color:#EEE'},
    'Hello world2!');
  goog.dom.appendChild(document.body, newDiv);
};

});  // goog.scope

goog.exportSymbol('budget.summary.hello', budget.summary.hello);
goog.exportSymbol('budget.summary.Loader', budget.summary.Loader);
