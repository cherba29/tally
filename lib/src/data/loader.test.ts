import { afterEach, describe, expect, jest, test } from '@jest/globals';
import { listFiles, loadBudget } from './loader';
import mockfs from 'mock-fs';
import { Month } from '../core/month';

describe('listFiles', () => {
  afterEach(() => {
    mockfs.restore();
    delete process.env.TALLY_FILES;
  });

  test('fails if environment not set', () => {
    expect(listFiles).toThrow(
      new Error('Process environment variable "TALLY_FILES" has not been specified.')
    );
  });

  test('returns empty on empty directory', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        /* empty directory */
      },
    });
    expect(listFiles()).toEqual([]);
  });

  test('returns recursively yaml file entries', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        '_config.yaml': '',
        subdir1: {
          'file1.json': '',
          'file2.yaml': '',
        },
        subdir2: {
          'file1.json': '',
          'file2.yaml': '',
        },
      },
    });
    expect(listFiles()).toEqual(['_config.yaml', 'subdir1/file2.yaml', 'subdir2/file2.yaml']);
  });
});

describe('loadBudget', () => {
  afterEach(() => {
    mockfs.restore();
    delete process.env.TALLY_FILES;
    jest.clearAllMocks();
  });

  test('fails if environment path is not set', () => {
    expect(loadBudget).toThrow(
      new Error('Process environment variable "TALLY_FILES" has not been specified.')
    );
  });

  test('fails without config', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        /* empty directory */
      },
    });
    expect(loadBudget).toThrow(
      new Error("ENOENT: no such file or directory, open 'tally/files/path/_config.yaml'")
    );
  });

  test('empty', () => {
    jest.spyOn(console, 'log').mockImplementation(() => {});

    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        '_config.yaml': 'budget_period: {start: Nov2019, end: Feb2020}',
      },
    });
    expect(loadBudget()).toEqual({
      accounts: new Map(),
      balances: new Map(),
      months: [new Month(2019, 10), new Month(2019, 11), new Month(2020, 0), new Month(2020, 1)],
      transfers: new Map(),
    });

    expect(console.log).toHaveBeenCalledTimes(2);
    expect(console.log).toHaveBeenCalledWith('Loading _config.yaml');
    expect(console.log).toHaveBeenCalledWith('Done loading 1 file(s)');
  });
});
