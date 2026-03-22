package com.cherba29.tally.core

data class TransferData(
  val toAccount: String,
  val toMonth: Month,
  val fromAccount: String,
  val fromMonth: Month,
  val balance: Balance,
  val description: String?,
)
