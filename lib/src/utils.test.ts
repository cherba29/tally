import { describe, expect, test } from '@jest/globals';
import { Map2, Map3 } from './utils';

describe('Map2', () => {
  test('get and set', () => {
    const map = new Map2<number>();
    map.set('key1', 'value1', 1);
    map.set('key2', 'value2', 2);
    expect(map.get('key1', 'value1')).toBe(1);
    expect(map.get('key2', 'value2')).toBe(2);
    expect(map.size).toBe(2);
  });

  test('get default', () => {
    const map = new Map2<number>();
    map.set('key1', 'value1', 1);
    map.set('key2', 'value2', 2);
    expect(map.getDefault('key1', 'value1', () => 3)).toBe(3);
    expect(map.size).toBe(3);
  });

  test('clear', () => {
    const map = new Map2<number>();
    map.set('key1', 'value1', 1);
    map.set('key2', 'value2', 2);
    map.clear();
    expect(map.size).toBe(0);
  });

  test('iterator', () => {
    const map = new Map2<number>();
    map.set('key1', 'value1', 1);
    map.set('key2', 'value2', 2);
    const result = Array.from(map);
    expect(result).toEqual([
        ['key1', 'value1', 1],
        ['key2', 'value2', 2],
    ]);
  });
});

describe('Map3', () => {
  test('get and set', () => {
    const map = new Map3<number>();
    map.set('key1', 'key2', 'key3', 1);
    expect(map.get('key1', 'key2', 'key3')).toBe(1);
    expect(map.size).toBe(1);
  });
  test('get default', () => {
    const map = new Map3<number>();
    map.set('key1', 'key2', 'key3', 1);
    expect(map.getDefault('key1', 'key2', 'key3', () => 3)).toBe(3);
    expect(map.size).toBe(1);
  });

  test('clear', () => {
    const map = new Map3<number>();
    map.set('key1', 'key2', 'key3', 1);
    map.clear();
    expect(map.size).toBe(0);
  });

  test('isEmpty', () => {
    const map = new Map3<number>();
    expect(map.isEmpty()).toBe(true);
    map.set('key1', 'key2', 'key3', 1);
    expect(map.isEmpty()).toBe(false);
  });

    test('get2', () => {
        const map = new Map3<number>();
        map.set('key1', 'key2', 'key3', 1);
        const map2 = map.get2('key1', 'key2');
        expect(map2?.size).toBe(1);
        expect(map.get2('key1', 'key2')?.get('key3')).toBe(1);
    });

  test('iterator', () => {
    const map = new Map3<number>();
    map.set('key1', 'key2', 'key3', 1);
    map.set('key4', 'key5', 'key6', 2);
    const result = Array.from(map);
    expect(result).toEqual([
        ['key1', 'key2', 'key3', 1],
        ['key4', 'key5', 'key6', 2],
    ]);
  });

    test('merge', () => {
        const map1 = new Map3<number>();
        map1.set('key1', 'key2', 'key3', 1);
        const map2 = new Map3<number>();
        map2.set('key4', 'key5', 'key6', 2);
        map1.merge(map2);
        expect(map1.size).toBe(2);
        expect(map2.size).toBe(2);
        const result = Array.from(map1);
        expect(result).toEqual([
            ['key1', 'key2', 'key3', 1],
            ['key4', 'key5', 'key6', 2],
        ]);
    });
});
