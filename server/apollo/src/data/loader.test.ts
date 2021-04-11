import { listFiles, loadBudget } from './loader';

describe('listFiles', () => {
  test('fails if environment not set', () => {
    expect(listFiles).toThrow(
      new Error('Process environment variable "TALLY_FILES" has not been specified.')
    );
  });

  afterEach(() => {
    delete process.env.TALLY_FILES;
  });
});

describe('loadBudget', () => {
  test('fails if environment path is not set', () => {
    expect(loadBudget).toThrow(
      new Error('Process environment variable "TALLY_FILES" has not been specified.')
    );
  });

  afterEach(() => {
    delete process.env.TALLY_FILES;
  });
});
