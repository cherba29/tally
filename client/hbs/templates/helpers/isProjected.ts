import {HelperOptions} from 'handlebars';
import {Balance} from '../../js/base';

export default function(balance: Balance, options: HelperOptions) {
  const fnTrue = options.fn;
  const fnFalse = options.inverse;
  return (balance && ('type' in balance) && balance.type !== 'CONFIRMED')
      ? fnTrue(this) : fnFalse(this);
};