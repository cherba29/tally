package com.cherba29.tally.data

import com.cherba29.tally.core.Account

data class Profile(
  val account: Account? = null,
  /**
   * Nodes are divided into external and non-external (internal).
   * That is any node named "external" and all of its descendants are considered to be external.
   **/
  val isExternal: Boolean = false,
  val isInactive: Boolean = false,
)
