import {HelperOptions} from 'handlebars';
import {Balance} from './base';

const Handlebars = (window as any).Handlebars;

Handlebars.registerHelper("incremented", function (index: number){
  return index + 1;
});

function formatCurrency(value: number) {
  if (value === null || value === undefined) {
    return "n/a";
  }
  return (value / 100.0).toFixed(2).replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,");
}

Handlebars.registerHelper('currency', formatCurrency);

Handlebars.registerHelper('isProjected', function(balance: Balance, options: HelperOptions) {
  let fnTrue=options.fn, fnFalse=options.inverse;
  return (balance && ('type' in balance) && balance.type != 'CONFIRMED')
      ? fnTrue(this) : fnFalse(this);
});

Handlebars.registerHelper('eqname', function(a: string, b: string, options: HelperOptions) {
  let fnTrue=options.fn, fnFalse=options.inverse;
  return (a == b) ? fnTrue(this) : fnFalse(this);
});
