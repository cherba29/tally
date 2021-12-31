import {HelperOptions} from 'handlebars';
import {Balance} from '../../js/base';

/**
 * Determines of balance is projected.
 * @param {Balance} balance
 * @param {HelperOptions} options
 * @return {Handlebars.TemplateDelegate<any>} Handlebars boolean.
 */
export default function(balance: Balance, options: HelperOptions) {
  const fnTrue = options.fn;
  const fnFalse = options.inverse;
  /* eslint-disable no-invalid-this */
  return (balance && ('type' in balance) && balance.type !== 'CONFIRMED') ?
      fnTrue(this) : fnFalse(this);
  /* eslint-enable no-invalid-this */
};
