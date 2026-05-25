╔═ single open account with path ═╗
- __type: GqlTable
  currentOwner: john
  owners:
  - john
  months:
  - Mar2026
  rows:
  - __type: GqlTableRow
    title: john
    account:
      __type: GqlAccount
      name: ""
      external: false
      summary: false
      openedOn: Jan2010
      owners:
      - john
    indent: 0
    isSpace: false
    isTotal: true
    isNormal: false
    cells:
    - __type: GqlTableCell
      month: Mar2026
      isClosed: false
      addSub: 0
      isProjected: true
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
  - __type: GqlTableRow
    title: internal
    account:
      __type: GqlAccount
      name: internal
      external: false
      summary: false
      openedOn: Jan2010
      owners:
      - john
    indent: 1
    isSpace: false
    isTotal: true
    isNormal: false
    cells:
    - __type: GqlTableCell
      month: Mar2026
      isClosed: false
      addSub: 0
      isProjected: true
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
  - __type: GqlTableRow
    title: test-account
    account:
      __type: GqlAccount
      name: test-account
      path:
      - internal
      external: false
      summary: false
      openedOn: Jan2026
      owners:
      - john
    indent: 2
    isSpace: false
    isTotal: false
    isNormal: true
    cells:
    - __type: GqlTableCell
      month: Mar2026
      isClosed: false
      addSub: 0
      isProjected: false
      isCovered: true
      isProjectedCovered: true
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true

╔═ single open account without path ═╗
- __type: GqlTable
  currentOwner: john
  owners:
  - john
  months:
  - Mar2026
  rows:
  - __type: GqlTableRow
    title: john
    account:
      __type: GqlAccount
      name: ""
      external: false
      summary: false
      openedOn: Jan2010
      owners:
      - john
    indent: 0
    isSpace: false
    isTotal: true
    isNormal: false
    cells:
    - __type: GqlTableCell
      month: Mar2026
      isClosed: false
      addSub: 0
      isProjected: true
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
  - __type: GqlTableRow
    title: external
    account:
      __type: GqlAccount
      name: external
      external: false
      summary: false
      openedOn: Jan2010
      owners:
      - john
    indent: 1
    isSpace: false
    isTotal: true
    isNormal: false
    cells:
    - __type: GqlTableCell
      month: Mar2026
      isClosed: false
      addSub: 0
      isProjected: true
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
  - __type: GqlTableRow
    title: test-account
    account:
      __type: GqlAccount
      name: test-account
      path:
      - external
      external: true
      summary: false
      openedOn: Jan2026
      owners:
      - john
    indent: 2
    isSpace: false
    isTotal: false
    isNormal: true
    cells:
    - __type: GqlTableCell
      month: Mar2026
      isClosed: false
      addSub: 0
      isProjected: false
      isCovered: true
      isProjectedCovered: true
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true

╔═ [end of file] ═╗
