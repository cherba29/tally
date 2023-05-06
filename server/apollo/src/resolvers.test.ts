import { Month } from '@tally/lib/core/month';
import resolvers from './resolvers';

describe('date', () => {
  test('parseValue', () => {
    expect(resolvers.Date.parseValue('2021-03-05')).toEqual(new Date('2021-03-05'));
  });
});

describe('GqlMonth', () => {
  test('parseValue', () => {
    expect(resolvers.GqlMonth.parseValue('Mar2021')).toEqual(new Month(2021, 2));
  });
});

describe('query', () => {
  test('buildBudget', () => {
    expect(resolvers.Query.budget).toThrow(
      new Error('Process environment variable "TALLY_FILES" has not been specified.')
    );
  });

  afterEach(() => {
    delete process.env.TALLY_FILES;
  });
});
