package tally.load;

import java.nio.file.Path;

import tally.core.Budget;

public interface FileScanner {
  /**
   * Scans given file path for data files and adds them to Budget.
   */
  void scan(Path path, Budget.Builder builder) throws LoadException;
}
