import { loadTallyConfig } from './config';

describe('Creation', () => {
  test('basic', () => {
    expect(loadTallyConfig).toThrow(
      new Error('Process environment variable "TALLY_FILES" has not been specified.')
    );
  });

  afterEach(() => {
    delete process.env.TALLY_FILES;
  });
});
