import {HelperOptions} from 'handlebars';
import {Balance, Type as BalanceType} from '@tally/lib/core/balance';

/**
 * Determines of balance is projected.
 * @param {Balance} balance - balance to render.
 * @param {HelperOptions} options - handlebar options.
 * @return Rendered string.
 */
export default function(balance: Balance, options: HelperOptions): string {
  const fnTrue: Handlebars.TemplateDelegate<any> = options.fn;
  const fnFalse: Handlebars.TemplateDelegate<any> = options.inverse;
  /* eslint-disable no-invalid-this */
  // @ts-ignore
  const context = this;
  /* eslint-enable no-invalid-this */
  return ((balance?.type ?? BalanceType.UNKNOWN) !== BalanceType.CONFIRMED) ?
      fnTrue(context) : fnFalse(context);
};
