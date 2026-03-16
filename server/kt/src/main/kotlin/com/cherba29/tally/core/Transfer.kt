package com.cherba29.tally.core

data class Transfer(
  val fromAccount: Account,
  val toAccount: Account,
  val fromMonth: Month,
  val toMonth: Month,
  val description: String?,
  val balance: Balance,
)