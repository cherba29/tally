╔═ two accounts with common owner and transfers/table1 ═╗
- Dec2019:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Dec2019..Dec2019
      isClosed: false
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
    transactions:
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
    balanceFromStart:
    - -2990
    - -1990
  Jan2020:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Jan2020..Jan2020
      isClosed: false
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
  Feb2020:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Feb2020..Feb2020
      isClosed: false
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

╔═ two accounts with common owner and transfers/table2 ═╗
- Dec2019:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Dec2019..Dec2019
      isClosed: false
      inFlows: 3000
      income: 3000
    coversPrevious: false
    coversProjectedPrevious: true
    hasProjectedTransfer: true
    isCovered: true
    isProjectedCovered: true
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
    balanceFromStart:
    - null
    - null
  Jan2020:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Jan2020..Jan2020
      isClosed: false
    coversPrevious: false
    coversProjectedPrevious: false
    hasProjectedTransfer: false
    isCovered: true
    isProjectedCovered: true
  Feb2020:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Feb2020..Feb2020
      isClosed: false
    coversPrevious: false
    coversProjectedPrevious: false
    hasProjectedTransfer: false
    isCovered: true
    isProjectedCovered: true

╔═ two accounts with external transfer/table1 ═╗
- Dec2019:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Dec2019..Dec2019
      isClosed: false
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
    transactions:
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
    balanceFromStart:
    - -2990
    - -1990
  Jan2020:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Jan2020..Jan2020
      isClosed: false
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
  Feb2020:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Feb2020..Feb2020
      isClosed: false
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

╔═ two accounts with external transfer/table2 ═╗
- Dec2019:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Dec2019..Dec2019
      isClosed: false
      inFlows: 3000
      income: 3000
    coversPrevious: false
    coversProjectedPrevious: true
    hasProjectedTransfer: true
    isCovered: true
    isProjectedCovered: true
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
    balanceFromStart:
    - null
    - null
  Jan2020:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Jan2020..Jan2020
      isClosed: false
    coversPrevious: false
    coversProjectedPrevious: false
    hasProjectedTransfer: false
    isCovered: true
    isProjectedCovered: true
  Feb2020:
    __type: TransactionStatement
    __base:
      __type: Statement
      months: Feb2020..Feb2020
      isClosed: false
    coversPrevious: false
    coversProjectedPrevious: false
    hasProjectedTransfer: false
    isCovered: true
    isProjectedCovered: true

╔═ [end of file] ═╗
