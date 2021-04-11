import { Month } from '../core/month';
import { Statement } from './statement';

class TestStatement extends Statement {
  get isClosed() {
    return true;
  }
}

describe('Creation', () => {
  test('basic', () => {
    const stmt = new TestStatement('test-statement', Month.fromString('Mar2021'));
    expect(stmt).toEqual({
      name: 'test-statement',
      month: new Month(2021, 2),
      inFlows: 0,
      income: 0,
      outFlows: 0,
      totalPayments: 0,
      totalTransfers: 0
    });
  });
});
