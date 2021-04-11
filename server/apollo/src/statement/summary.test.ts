import { buildSummaryStatementTable } from './summary';

describe('Build', () => {
  test('empty', () => {
    const statements = Array.from(buildSummaryStatementTable([]));
    expect(statements).toEqual([]);
  });
});
