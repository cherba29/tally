import {describe, expect, it} from '@jest/globals';
import {Cell} from './cell';
import {GqlStatement} from './gql_types';

describe('constructor', function () {
  it('sets unnaccounted from undefined', function () {
    const stmt: GqlStatement = {};
    const cell = new Cell('owner1', 'acc1', 'Apr2022', stmt);
    expect(cell.unaccounted).toBeNull();
    expect(cell.balanced).toBeTruthy();
  });
  it('sets unnaccounted from set value', function () {
    const stmt: GqlStatement = {unaccounted: 100};
    const cell = new Cell('owner1', 'acc1', 'Apr2022', stmt);
    expect(cell.unaccounted).toBe(100);
    expect(cell.balanced).toBeFalsy();
  });
});
