╔═ closed accounts excluded ═╗
- statements:
  - __type: GqlStatement
    name: test-account2
    month: Mar2026
    isClosed: false
    isCovered: true
    isProjectedCovered: true
    hasProjectedTransfer: false
    startBalance:
      __type: GqlBalance
      amount: 200
      date: 2026-03-01
      type: CONFIRMED
    endBalance:
      __type: GqlBalance
      amount: 250
      date: 2026-04-01
      type: CONFIRMED
    change: 50
    percentChange: 25.0
    unaccounted: 50
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account2
    addSub: 0
    income: 0
    change: 50
    inFlows: 0
    outFlows: 0
    percentChange: 25.0
    annualizedPercentChange: 0.0
    totalPayments: 0
    totalTransfers: 0
    unaccounted: 50
    endBalance:
      __type: GqlBalance
      amount: 250
      date: 2026-04-01
      type: CONFIRMED
    startBalance:
      __type: GqlBalance
      amount: 200
      date: 2026-03-01
      type: CONFIRMED

╔═ multiple months ═╗
- statements:
  - __type: GqlStatement
    name: test-account1
    month: Mar2026
    isClosed: false
    isCovered: true
    isProjectedCovered: true
    hasProjectedTransfer: false
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-05-01
      type: PROJECTED
    inFlows: 75
    outFlows: -50
    income: 75
    totalPayments: -50
    change: -100
    addSub: 25
    percentChange: -100.0
    unaccounted: -125
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account1
    addSub: 25
    income: 75
    change: -100
    inFlows: 75
    outFlows: -50
    percentChange: -100.0
    annualizedPercentChange: 0.0
    totalPayments: -50
    totalTransfers: 0
    unaccounted: -125
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-05-01
      type: PROJECTED
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED

╔═ multiple months null start month ═╗
- statements:
  - __type: GqlStatement
    name: test-account1
    month: Mar2026
    isClosed: false
    isCovered: true
    isProjectedCovered: true
    hasProjectedTransfer: false
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED
    endBalance:
      __type: GqlBalance
      amount: 150
      date: 2026-04-01
      type: CONFIRMED
    outFlows: -50
    totalPayments: -50
    change: 50
    addSub: -50
    percentChange: 50.0
    unaccounted: 100
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account1
    addSub: -50
    income: 0
    change: 50
    inFlows: 0
    outFlows: -50
    percentChange: 50.0
    annualizedPercentChange: 0.0
    totalPayments: -50
    totalTransfers: 0
    unaccounted: 100
    endBalance:
      __type: GqlBalance
      amount: 150
      date: 2026-04-01
      type: CONFIRMED
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED

╔═ single ═╗
- statements:
  - __type: GqlStatement
    name: test-account1
    month: Mar2026
    isClosed: false
    isCovered: true
    isProjectedCovered: true
    hasProjectedTransfer: false
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-04-01
      type: PROJECTED
    change: -100
    percentChange: -100.0
    unaccounted: -100
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account1
    addSub: 0
    income: 0
    change: -100
    inFlows: 0
    outFlows: 0
    percentChange: -100.0
    annualizedPercentChange: 0.0
    totalPayments: 0
    totalTransfers: 0
    unaccounted: -100
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-04-01
      type: PROJECTED
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED

╔═ single with multiple transaction statement ═╗
- statements:
  - __type: GqlStatement
    name: test-account1
    month: Mar2026
    isClosed: false
    isCovered: true
    isProjectedCovered: true
    hasProjectedTransfer: false
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-04-01
      type: PROJECTED
    outFlows: -50
    totalPayments: -50
    change: -100
    addSub: -50
    percentChange: -100.0
    unaccounted: -50
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account1
    addSub: -50
    income: 0
    change: -100
    inFlows: 0
    outFlows: -50
    percentChange: -100.0
    annualizedPercentChange: 0.0
    totalPayments: -50
    totalTransfers: 0
    unaccounted: -50
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-04-01
      type: PROJECTED
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED

╔═ transfers summary multiple months ═╗
- months:
  - Mar2026
  - Feb2026
  data:
  - internalTransfers: 0
    externalTransfers: -50
    totalInternalTransfers: 0
    totalExternalTransfers: -50
  - internalTransfers: 0
    externalTransfers: 0
    totalInternalTransfers: 0
    totalExternalTransfers: 0

╔═ with single transaction statement ═╗
- months:
  - Mar2026
  data:
  - internalTransfers: -50
    externalTransfers: 0
    totalInternalTransfers: -50
    totalExternalTransfers: 0

╔═ with transaction statement ═╗
- statements:
  - __type: GqlStatement
    name: test-account1
    month: Mar2026
    isClosed: false
    isCovered: true
    isProjectedCovered: true
    hasProjectedTransfer: false
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-04-01
      type: PROJECTED
    outFlows: -50
    totalTransfers: -50
    change: -100
    addSub: -50
    percentChange: -100.0
    unaccounted: -50
  - __type: GqlStatement
    name: test-account2
    month: Mar2026
    isClosed: false
    isCovered: true
    isProjectedCovered: true
    hasProjectedTransfer: false
    startBalance:
      __type: GqlBalance
      amount: 200
      date: 2026-03-01
      type: CONFIRMED
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-04-01
      type: PROJECTED
    inFlows: 50
    totalTransfers: 50
    change: -200
    addSub: 50
    percentChange: -100.0
    unaccounted: -250
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account1
    - test-account2
    addSub: 0
    income: 0
    change: -300
    inFlows: 50
    outFlows: -50
    percentChange: -100.0
    annualizedPercentChange: 0.0
    totalPayments: 0
    totalTransfers: 0
    unaccounted: -300
    endBalance:
      __type: GqlBalance
      amount: 0
      date: 2026-04-01
      type: PROJECTED
    startBalance:
      __type: GqlBalance
      amount: 300
      date: 2026-03-01
      type: CONFIRMED

╔═ [end of file] ═╗
