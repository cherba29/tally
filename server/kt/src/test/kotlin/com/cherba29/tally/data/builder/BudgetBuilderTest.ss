╔═ two accounts with common owner and transfers ═╗
- __type: TransactionStatement
  __base:
    __type: Statement
    path:
    - john
    - external
    - test-account1
    months: Feb2020..Feb2020
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
    path:
    - john
    - external
    - test-account1
    months: Jan2020..Jan2020
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
    path:
    - john
    - external
    - test-account1
    months: Dec2019..Dec2019
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
    path:
    - john
    - external
    - test-account2
    balance:
      __type: Balance
      amount: -2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: EXPENSE
    balanceFromStart: -2990
  - __type: Transaction
    path:
    - john
    - external
    - test-account2
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
    path:
    - john
    - external
    - test-account2
    months: Feb2020..Feb2020
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    path:
    - john
    - external
    - test-account2
    months: Jan2020..Jan2020
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    path:
    - john
    - external
    - test-account2
    months: Dec2019..Dec2019
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
    path:
    - john
    - external
    - test-account1
    balance:
      __type: Balance
      amount: 2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: INCOME
  - __type: Transaction
    path:
    - john
    - external
    - test-account1
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
    path:
    - john
    - external
    - test-account1
    months: Feb2020..Feb2020
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
    path:
    - john
    - external
    - test-account1
    months: Jan2020..Jan2020
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
    path:
    - john
    - external
    - test-account1
    months: Dec2019..Dec2019
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
    path:
    - john
    - external
    - test-account2
    balance:
      __type: Balance
      amount: -2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: EXPENSE
    balanceFromStart: -2990
  - __type: Transaction
    path:
    - john
    - external
    - test-account2
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
    path:
    - john
    - external
    - test-account2
    months: Feb2020..Feb2020
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    path:
    - john
    - external
    - test-account2
    months: Jan2020..Jan2020
  coversPrevious: false
  coversProjectedPrevious: false
  hasProjectedTransfer: false
  isCovered: true
  isProjectedCovered: true
  isClosed: false
- __type: TransactionStatement
  __base:
    __type: Statement
    path:
    - john
    - external
    - test-account2
    months: Dec2019..Dec2019
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
    path:
    - john
    - external
    - test-account1
    balance:
      __type: Balance
      amount: 2000
      date: 2019-12-05
      type: PROJECTED
    description: First transfer
    type: INCOME
  - __type: Transaction
    path:
    - john
    - external
    - test-account1
    balance:
      __type: Balance
      amount: 1000
      date: 2019-12-05
      type: PROJECTED
    description: Second transfer
    type: INCOME

╔═ [end of file] ═╗
