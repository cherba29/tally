
export class Map2<ValueType> implements Iterable<[string, string, ValueType]> {
  private readonly store = new Map<string, Map<string, ValueType>>();

  get(key1: string, key2: string): ValueType|undefined {
    return this.store.get(key1)?.get(key2);
  }

  getDefault(key1: string, key2: string, defaultFactory: ()=>ValueType): ValueType {
    let value = this.store.get(key1)?.get(key2);
    if (!value) {
      value = defaultFactory();
      this.set(key1, key2, value);
    }
    return value;
  }

  set(key1: string, key2: string, value: ValueType): Map2<ValueType> {
    let subStore = this.store.get(key1);
    if (!subStore) {
      subStore = new Map<string, ValueType>();
      this.store.set(key1, subStore);
    }
    subStore.set(key2, value);
   return this; 
  }

  clear() {
    this.store.clear();
  }

  get size() {
    let totalSize = 0;
    for (const subStore of this.store.values()) {
      totalSize += subStore.size;
    }
    return totalSize;
  }

  *[Symbol.iterator](): IterableIterator<[string, string, ValueType]> {
    for (const [key1, subStore] of this.store) {
      for (const [key2, value] of subStore) {
        yield [key1, key2, value];
      }
    }
  }
}

export class Map3<ValueType> implements Iterable<[string, string, string, ValueType]> {
  private readonly store = new Map<string, Map<string, Map<string, ValueType>>>();

  get(key1: string, key2: string, key3: string): ValueType|undefined {
    return this.store.get(key1)?.get(key2)?.get(key3);
  }

  get2(key1: string, key2: string): Map<string, ValueType>|undefined {
    return this.store.get(key1)?.get(key2);
  }

  getDefault(key1: string, key2: string, key3: string, defaultFactory: ()=>ValueType): ValueType {
    let value = this.store.get(key1)?.get(key2)?.get(key3);
    if (!value) {
      value = defaultFactory();
      this.set(key1, key2, key3, value);
    }
    return value;
  }


  set(key1: string, key2: string, key3: string, value: ValueType): Map3<ValueType> {
    let subStore1 = this.store.get(key1);
    if (!subStore1) {
      subStore1 = new Map<string, Map<string, ValueType>>();
      this.store.set(key1, subStore1);
    }
    let subStore2 = subStore1.get(key2);
    if (!subStore2) {
      subStore2 = new Map<string, ValueType>();
      subStore1.set(key2, subStore2);
    }
    subStore2.set(key3, value);
   return this; 
  }

  get size() {
    let totalSize = 0;
    for (const subStore1 of this.store.values()) {
      for (const subStore2 of subStore1.values()) {
        totalSize += subStore2.size;
      }
    }
    return totalSize;
  }

  isEmpty() {
    return this.store.size === 0;
  }

  clear() {
    this.store.clear();
  }

  merge(otherMap3: Map3<ValueType>) {
    for (const [key1, key2, key3, value] of otherMap3) {
      this.set(key1, key2, key3, value);
    }
  }

  *[Symbol.iterator](): IterableIterator<[string, string, string, ValueType]> {
    for (const [key1, subStore1] of this.store) {
      for (const [key2, subStore2] of subStore1) {
        for (const [key3, value] of subStore2) {
          yield [key1, key2, key3, value];
        }
      }
    }
  }
}
