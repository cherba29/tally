import { loadTallyConfig } from './config';
import mockfs from 'mock-fs';

describe('Loading config', () => {
  afterEach(() => {
    mockfs.restore();
    delete process.env.TALLY_FILES;
  });

  test('fails tally path is not set', () => {
    expect(loadTallyConfig).toThrow(
      new Error('Process environment variable "TALLY_FILES" has not been specified.')
    );
  });

  test('fails when config.yaml does not exits', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        /* empty directory */
      }
    });
    expect(loadTallyConfig).toThrow(
      new Error(`ENOENT: no such file or directory, open '${TALLY_PATH}/_config.yaml'`)
    );
  });

  test('fails when config.yaml is empty', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        '_config.yaml': ''
      }
    });
    expect(loadTallyConfig).toThrow(
      new Error(`Could not parse file at "${TALLY_PATH}/_config.yaml"`)
    );
  });

  test('fails when config.yaml does not specify budget_period', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        '_config.yaml': 'something_else: []'
      }
    });
    expect(loadTallyConfig).toThrow(
      new Error(`File "${TALLY_PATH}/_config.yaml" does not specify budget_period`)
    );
  });

  test('fails when config.yaml does not specify start', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        '_config.yaml': 'budget_period: {end: Feb2020}'
      }
    });
    expect(loadTallyConfig).toThrow(
      new Error(`File "${TALLY_PATH}/_config.yaml" does not specify start in budget_period`)
    );
  });

  test('fails when config.yaml does not specify end', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        '_config.yaml': 'budget_period: {start: Nov2019}'
      }
    });
    expect(loadTallyConfig).toThrow(
      new Error(`File "${TALLY_PATH}/_config.yaml" does not specify end in budget_period`)
    );
  });

  test('succeeds', () => {
    const TALLY_PATH = 'tally/files/path';
    process.env.TALLY_FILES = TALLY_PATH;
    mockfs({
      [TALLY_PATH]: {
        '_config.yaml': 'budget_period: {start: Nov2019, end: Feb2021}'
      }
    });
    expect(loadTallyConfig()).toEqual({
      budget_period: {
        start: 'Nov2019',
        end: 'Feb2021'
      }
    });
  });
});
