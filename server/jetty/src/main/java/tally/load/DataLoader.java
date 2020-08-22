package tally.load;

import tally.core.Budget;

import java.util.Map;

public interface DataLoader {
    /**
     * Converts associative map into {@link Budget} object.
     */
    void load(Map<String, Object> data, Budget.Builder budgetBuilder) throws LoadException;
}
