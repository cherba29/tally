╔═ two accounts with common owner and transfers ═╗
[
TransactionStatement {
          Statement {
      account = Account {
       name = test-account1
       description = null
       path = []
       type = CHECKING
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      month = Feb2020
      startBalance = Balance { amount: 0.30, date: 2020-02-01, type: PROJECTED }
      endBalance = null
      inFlows = 0
      outFlows = 0
      totalTransfers = 0
      totalPayments = 0
      income = 0
    } 
          coversPrevious = false
          coversProjectedPrevious = false
          hasProjectedTransfer = false
          isCovered = true
          isProjectedCovered = true
          isClosed = false
          transactions {
            
          }
        }, TransactionStatement {
          Statement {
      account = Account {
       name = test-account1
       description = null
       path = []
       type = CHECKING
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      month = Jan2020
      startBalance = Balance { amount: 0.20, date: 2020-01-01, type: CONFIRMED }
      endBalance = Balance { amount: 0.30, date: 2020-02-01, type: PROJECTED }
      inFlows = 0
      outFlows = 0
      totalTransfers = 0
      totalPayments = 0
      income = 0
    } 
          coversPrevious = false
          coversProjectedPrevious = false
          hasProjectedTransfer = false
          isCovered = true
          isProjectedCovered = true
          isClosed = false
          transactions {
            
          }
        }, TransactionStatement {
          Statement {
      account = Account {
       name = test-account1
       description = null
       path = []
       type = CHECKING
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      month = Dec2019
      startBalance = Balance { amount: 0.10, date: 2019-12-01, type: CONFIRMED }
      endBalance = Balance { amount: 0.20, date: 2020-01-01, type: CONFIRMED }
      inFlows = 0
      outFlows = -3000
      totalTransfers = -3000
      totalPayments = 0
      income = 0
    } 
          coversPrevious = false
          coversProjectedPrevious = false
          hasProjectedTransfer = true
          isCovered = true
          isProjectedCovered = true
          isClosed = false
          transactions {
            Transaction {
      account = Account {
       name = test-account2
       description = null
       path = []
       type = CREDIT
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      balance = Balance {
      amount = -2000
      date = 2019-12-05
      type = PROJECTED
    } 
      description = First transfer
      type = TRANSFER
      balanceFromStart = -2990
Transaction {
      account = Account {
       name = test-account2
       description = null
       path = []
       type = CREDIT
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      balance = Balance {
      amount = -1000
      date = 2019-12-05
      type = PROJECTED
    } 
      description = Second transfer
      type = TRANSFER
      balanceFromStart = -990
          }
        }, TransactionStatement {
          Statement {
      account = Account {
       name = test-account2
       description = null
       path = []
       type = CREDIT
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      month = Feb2020
      startBalance = null
      endBalance = null
      inFlows = 0
      outFlows = 0
      totalTransfers = 0
      totalPayments = 0
      income = 0
    } 
          coversPrevious = false
          coversProjectedPrevious = false
          hasProjectedTransfer = false
          isCovered = true
          isProjectedCovered = true
          isClosed = false
          transactions {
            
          }
        }, TransactionStatement {
          Statement {
      account = Account {
       name = test-account2
       description = null
       path = []
       type = CREDIT
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      month = Jan2020
      startBalance = null
      endBalance = null
      inFlows = 0
      outFlows = 0
      totalTransfers = 0
      totalPayments = 0
      income = 0
    } 
          coversPrevious = false
          coversProjectedPrevious = false
          hasProjectedTransfer = false
          isCovered = true
          isProjectedCovered = true
          isClosed = false
          transactions {
            
          }
        }, TransactionStatement {
          Statement {
      account = Account {
       name = test-account2
       description = null
       path = []
       type = CREDIT
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      month = Dec2019
      startBalance = null
      endBalance = null
      inFlows = 3000
      outFlows = 0
      totalTransfers = 3000
      totalPayments = 0
      income = 0
    } 
          coversPrevious = false
          coversProjectedPrevious = true
          hasProjectedTransfer = true
          isCovered = true
          isProjectedCovered = true
          isClosed = false
          transactions {
            Transaction {
      account = Account {
       name = test-account1
       description = null
       path = []
       type = CHECKING
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      balance = Balance {
      amount = 2000
      date = 2019-12-05
      type = PROJECTED
    } 
      description = First transfer
      type = TRANSFER
      balanceFromStart = null
Transaction {
      account = Account {
       name = test-account1
       description = null
       path = []
       type = CHECKING
       number = null
       openedOn = null
       closedOn = null
       owners = [john]
       url = null
       address = null
       phone = null
       userName = null
       password = null
     }
      balance = Balance {
      amount = 1000
      date = 2019-12-05
      type = PROJECTED
    } 
      description = Second transfer
      type = TRANSFER
      balanceFromStart = null
          }
        }
]

╔═ [end of file] ═╗
