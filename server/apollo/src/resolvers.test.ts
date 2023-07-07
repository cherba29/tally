import { Month } from '@tally/lib/core/month';
import { unwatchBudgetFiles } from '@tally/lib/data/loader';
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

describe('gqlTable', () => {
  beforeEach(() => {
    process.env.TALLY_FILES = 'server/apollo/src/testdata/tally';
  });

  test('load', async () => {
    const data = await resolvers.Query.table(undefined, { owner: 'john' });
    expect(data).toMatchSnapshot();
  });

  afterEach(() => {
    unwatchBudgetFiles();
    delete process.env.TALLY_FILES;
  });
});

describe('gqlSummary', () => {
  beforeEach(() => {
    process.env.TALLY_FILES = 'server/apollo/src/testdata/tally';
  });

  test('load', async () => {
    const data = await resolvers.Query.summary(undefined, {
      owner: 'john',
      month: 'Feb2015',
      accountType: 'BILL'
    });
    expect(data).toMatchSnapshot();
  });

  afterEach(() => {
    unwatchBudgetFiles();
    delete process.env.TALLY_FILES;
  });
});
