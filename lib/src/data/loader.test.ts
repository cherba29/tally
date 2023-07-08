import { afterEach, describe, expect, jest, test } from '@jest/globals';
import { listFiles, loadBudget, unwatchBudgetFiles } from './loader';
import mockfs from 'mock-fs';

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
    expect(listFiles()).toEqual(['subdir1/file2.yaml', 'subdir2/file2.yaml']);
  });
});

describe('loadBudget', () => {
  afterEach(() => {
    unwatchBudgetFiles();
    mockfs.restore();
    delete process.env.TALLY_FILES;
    jest.clearAllMocks();
  });

  test('fails if environment path is not set', async () => {
    await expect(loadBudget()).rejects.toThrow(
      new Error('Process environment variable "TALLY_FILES" has not been specified.')
    );
  });

  test('just account', async () => {
    jest.spyOn(console, 'log').mockImplementation(() => {});

    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        'file2.yaml': `
          name: test-account
          owner: [ someone ]
          type: external
          opened_on: Mar2019
        `.replace(/^ +/g, ''),
      },
    });
    const result = await loadBudget();
    expect(result).toMatchSnapshot();

    expect(console.log).toHaveBeenCalledTimes(3);
    expect(console.log).toHaveBeenCalledWith(expect.stringContaining('Done loading 1 file(s)'));
  });
});
