const Handlebars = (window as any).Handlebars;

Handlebars.registerHelper("incremented", function (index){
  return index + 1;
});

function formatCurrency(value) {
  if (value === null || value === undefined) {
    return "n/a";
  }
  return (value / 100.0).toFixed(2).replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,");
}

Handlebars.registerHelper('currency', formatCurrency);

Handlebars.registerHelper('isProjected', function(balance, options) {
  let fnTrue=options.fn, fnFalse=options.inverse;
  return (balance && ('type' in balance) && balance.type != 'CONFIRMED')
      ? fnTrue() : fnFalse();
});

Handlebars.registerHelper('eqname', function(a, b, options) {
  let fnTrue=options.fn, fnFalse=options.inverse;
  return (a == b) ? fnTrue() : fnFalse();
});

Handlebars.registerHelper('currencyitem', function(object, key) {
  let value = object[key];
  return value && formatCurrency(value) || "";
});

Handlebars.registerHelper(
    'isNotSymetric',
    function(aTransfers, bTransfers, aName, bName, options) {
      let fnTrue=options.fn, fnFalse=options.inverse;
      let aTransfer = aTransfers[bName];
      let bTransfer = bTransfers[aName];
      return (aTransfer && bTransfer && aTransfer != -bTransfer) ? fnTrue() : fnFalse();
    }
);
