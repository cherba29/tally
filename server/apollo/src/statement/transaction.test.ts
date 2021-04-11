import { BudgetBuilder } from '../core/budget';
import { Month } from '../core/month';
import { buildTransactionStatementTable } from './transaction';

describe('Build', () => {
  test('empty', () => {
    const builder = new BudgetBuilder();
    builder.setPeriod(Month.fromString('Nov2019'), Month.fromString('Feb2020'));
    const table = buildTransactionStatementTable(builder.build());
    expect(table).toEqual([]);
  });
});
