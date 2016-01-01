package tally.core;

public class MissDatedBalanceException extends BudgetException {
  private static final long serialVersionUID = 1L;

  public MissDatedBalanceException(String message) {
    super(message);
  }
}
