╔═ closed account within path - summary also closed ═╗
- __type: GqlTable
  currentOwner: john
  owners:
  - john
  months:
  - Jan2026
  - Dec2025
  rows:
  - __type: GqlTableRow
    id: john
    title: john
    account:
      __type: GqlAccount
      name: ""
      path:
      - ""
      external: false
      summary: true
      openedOn: Jan2010
      owners:
      - john
    indent: 0
    isSpace: false
    isTotal: true
    isNormal: false
    cells:
    - __type: GqlTableCell
      month: Jan2026
      isClosed: false
      addSub: 0
      isProjected: true
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
    - __type: GqlTableCell
      month: Dec2025
      isClosed: false
      addSub: 0
      balance: 100
      isProjected: false
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      unaccounted: 0
      balanced: true
  - __type: GqlTableRow
    id: john/external
    title: external
    account:
      __type: GqlAccount
      name: external
      path:
      - ""
      external: true
      summary: true
      openedOn: Jan2010
      owners:
      - john
    indent: 1
    isSpace: false
    isTotal: true
    isNormal: false
    cells:
    - __type: GqlTableCell
      month: Jan2026
      isClosed: false
      addSub: 0
      isProjected: true
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
    - __type: GqlTableCell
      month: Dec2025
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
    id: john/external/test-account2
    title: test-account2
    account:
      __type: GqlAccount
      name: test-account2
      path:
      - external
      external: true
      summary: false
      openedOn: Dec2025
      owners:
      - john
    indent: 2
    isSpace: false
    isTotal: false
    isNormal: true
    cells:
    - __type: GqlTableCell
      month: Jan2026
      isClosed: false
      addSub: 0
      isProjected: false
      isCovered: true
      isProjectedCovered: true
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
    - __type: GqlTableCell
      month: Dec2025
      isClosed: false
      addSub: 0
      isProjected: false
      isCovered: true
      isProjectedCovered: true
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
  - __type: GqlTableRow
    id: john/internal
    title: internal
    account:
      __type: GqlAccount
      name: internal
      path:
      - ""
      external: false
      summary: true
      openedOn: Jan2010
      owners:
      - john
    indent: 1
    isSpace: false
    isTotal: true
    isNormal: false
    cells:
    - __type: GqlTableCell
      month: Jan2026
      isClosed: false
      addSub: 0
      isProjected: true
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
    - __type: GqlTableCell
      month: Dec2025
      isClosed: true
      addSub: 0
      balance: 100
      isProjected: false
      isCovered: false
      isProjectedCovered: false
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
  - __type: GqlTableRow
    id: john/internal/test-account1
    title: test-account1
    account:
      __type: GqlAccount
      name: test-account1
      path:
      - internal
      external: false
      summary: false
      openedOn: Jan2026
      closedOn: Jan2026
      owners:
      - john
    indent: 2
    isSpace: false
    isTotal: false
    isNormal: true
    cells:
    - __type: GqlTableCell
      month: Jan2026
      isClosed: false
      addSub: 0
      isProjected: false
      isCovered: true
      isProjectedCovered: true
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true
    - __type: GqlTableCell
      month: Dec2025
      isClosed: true
      addSub: 0
      balance: 100
      isProjected: false
      isCovered: true
      isProjectedCovered: true
      hasProjectedTransfer: false
      percentChange: 0.0
      annualizedPercentChange: 0.0
      balanced: true

╔═ empty ═╗
- __type: GqlTable
  currentOwner: john
  owners:
  - john
  months:
  - Mar2026
  rows:
  - __type: GqlTableRow
    id: john
    title: john
    account:
      __type: GqlAccount
      name: ""
      path:
      - ""
      external: false
      summary: true
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
    id: john/internal
    title: internal
    account:
      __type: GqlAccount
      name: internal
      path:
      - ""
      external: false
      summary: true
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
    id: john/internal/test-account1
    title: test-account1
    account:
      __type: GqlAccount
      name: test-account
      path:
      - internal
      external: false
      summary: false
      openedOn: Mar2026
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

╔═ single open account with path ═╗
- __type: GqlTable
  currentOwner: john
  owners:
  - john
  months:
  - Mar2026
  rows:
  - __type: GqlTableRow
    id: john
    title: john
    account:
      __type: GqlAccount
      name: ""
      path:
      - ""
      external: false
      summary: true
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
    id: john/internal
    title: internal
    account:
      __type: GqlAccount
      name: internal
      path:
      - ""
      external: false
      summary: true
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
    id: john/internal/test-account
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
    id: john
    title: john
    account:
      __type: GqlAccount
      name: ""
      path:
      - ""
      external: false
      summary: true
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
    id: john/external
    title: external
    account:
      __type: GqlAccount
      name: external
      path:
      - ""
      external: true
      summary: true
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
    id: john/external/test-account
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
