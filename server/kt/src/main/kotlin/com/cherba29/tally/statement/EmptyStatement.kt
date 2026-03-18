package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month

class EmptyStatement(account: Account, month: Month) : Statement(account, month) {
  override val isClosed: Boolean get() = false
}
