export interface InitData {
  name: string;
}

export class Account {
  readonly name: string;
  constructor(data: InitData) {
    this.name = data.name;
  }
}