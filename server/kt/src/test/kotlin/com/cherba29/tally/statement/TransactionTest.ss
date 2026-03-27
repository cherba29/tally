╔═ single account with transfers ═╗
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Feb2020
    startBalance:
      __type: Balance
      amount: 30
      date: 2020-02-01
      type: PROJECTED
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Jan2020
    startBalance:
      __type: Balance
      amount: 20
      date: 2020-01-01
      type: CONFIRMED
    endBalance:
      __type: Balance
      amount: 30
      date: 2020-02-01
      type: PROJECTED
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Dec2019
    startBalance:
      __type: Balance
      amount: 10
      date: 2019-12-01
      type: CONFIRMED
    endBalance:
      __type: Balance
      amount: 20
      date: 2020-01-01
      type: CONFIRMED
    inFlows: 4000
    income: 4000
  coversPrevious: false
  coversProjectedPrevious: true
  hasProjectedTransfer: true
  isCovered: true
  isProjectedCovered: true
  isClosed: false
  transactions:
  - __type: Transaction
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    balance:
      __type: Balance
      amount: 2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: INCOME
    balanceFromStart: 4010
  - __type: Transaction
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    balance:
      __type: Balance
      amount: 2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: INCOME
    balanceFromStart: 2010

╔═ two accounts with common owner and transfers ═╗
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Feb2020
    startBalance:
      __type: Balance
      amount: 30
      date: 2020-02-01
      type: PROJECTED
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Jan2020
    startBalance:
      __type: Balance
      amount: 20
      date: 2020-01-01
      type: CONFIRMED
    endBalance:
      __type: Balance
      amount: 30
      date: 2020-02-01
      type: PROJECTED
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Dec2019
    startBalance:
      __type: Balance
      amount: 10
      date: 2019-12-01
      type: CONFIRMED
    endBalance:
      __type: Balance
      amount: 20
      date: 2020-01-01
      type: CONFIRMED
    outFlows: -3000
    totalPayments: -3000
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: true
  isCovered: true
  isProjectedCovered: true
  isClosed: false
  transactions:
  - __type: Transaction
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    balance:
      __type: Balance
      amount: -2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: EXPENSE
    balanceFromStart: -2990
  - __type: Transaction
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    balance:
      __type: Balance
      amount: -1000
      date: 2019-12-05
      type: PROJECTED
    description: Second transfer
    type: EXPENSE
    balanceFromStart: -990
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Feb2020
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Jan2020
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Dec2019
    inFlows: 3000
    income: 3000
  coversPrevious: false
  coversProjectedPrevious: true
  hasProjectedTransfer: true
  isCovered: true
  isProjectedCovered: true
  isClosed: false
  transactions:
  - __type: Transaction
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    balance:
      __type: Balance
      amount: 2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: INCOME
  - __type: Transaction
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    balance:
      __type: Balance
      amount: 1000
      date: 2019-12-05
      type: PROJECTED
    description: Second transfer
    type: INCOME

╔═ two accounts with external transfer ═╗
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
    month: Feb2020
    startBalance:
      __type: Balance
      amount: 30
      date: 2020-02-01
      type: PROJECTED
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
    month: Jan2020
    startBalance:
      __type: Balance
      amount: 20
      date: 2020-01-01
      type: CONFIRMED
    endBalance:
      __type: Balance
      amount: 30
      date: 2020-02-01
      type: PROJECTED
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
    month: Dec2019
    startBalance:
      __type: Balance
      amount: 10
      date: 2019-12-01
      type: CONFIRMED
    endBalance:
      __type: Balance
      amount: 20
      date: 2020-01-01
      type: CONFIRMED
    outFlows: -3000
    totalPayments: -3000
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: true
  isCovered: true
  isProjectedCovered: true
  isClosed: false
  transactions:
  - __type: Transaction
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    balance:
      __type: Balance
      amount: -2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: EXPENSE
    balanceFromStart: -2990
  - __type: Transaction
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    balance:
      __type: Balance
      amount: -1000
      date: 2019-12-05
      type: PROJECTED
    description: Second transfer
    type: EXPENSE
    balanceFromStart: -990
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Feb2020
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Jan2020
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    account:
      __type: Account
      name: test-account2
      path:
      - external
      openedOn: Dec2019
      owner:
      - john
    month: Dec2019
    inFlows: 3000
    income: 3000
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: true
  isCovered: true
  isProjectedCovered: true
  isClosed: false
  transactions:
  - __type: Transaction
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
    balance:
      __type: Balance
      amount: 2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: INCOME
  - __type: Transaction
    account:
      __type: Account
      name: test-account1
      path:
      - external
      openedOn: Dec2019
    balance:
      __type: Balance
      amount: 1000
      date: 2019-12-05
      type: PROJECTED
    description: Second transfer
    type: INCOME

╔═ [end of file] ═╗
