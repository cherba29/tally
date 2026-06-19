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
      date: 2026-04-01
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
      date: 2026-04-01
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
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account1
    addSub: 0
    income: 0
    change: 0
    inFlows: 0
    outFlows: 0
    percentChange: 0.0
    annualizedPercentChange: 0.0
    totalPayments: 0
    totalTransfers: 0
    unaccounted: 0
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
    outFlows: -50
    totalPayments: -50
    addSub: -50
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account1
    addSub: -50
    income: 0
    change: 0
    inFlows: 0
    outFlows: -50
    percentChange: 0.0
    annualizedPercentChange: 0.0
    totalPayments: -50
    totalTransfers: 0
    unaccounted: 0
    startBalance:
      __type: GqlBalance
      amount: 100
      date: 2026-03-01
      type: CONFIRMED

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
    outFlows: -50
    totalTransfers: -50
    addSub: -50
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
    inFlows: 50
    totalTransfers: 50
    addSub: 50
  total:
    __type: GqlSummaryStatement
    name: internal
    month: Mar2026
    accounts:
    - test-account1
    - test-account2
    addSub: 0
    income: 0
    change: 0
    inFlows: 50
    outFlows: -50
    percentChange: 0.0
    annualizedPercentChange: 0.0
    totalPayments: 0
    totalTransfers: 0
    unaccounted: 0
    startBalance:
      __type: GqlBalance
      amount: 300
      date: 2026-03-01
      type: CONFIRMED

╔═ [end of file] ═╗
