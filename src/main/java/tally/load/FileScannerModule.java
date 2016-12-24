package tally.load;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class FileScannerModule extends AbstractModule {

  @Override
  protected void configure() {
  }

  @Provides
  FileScanner provideFileScanner(DataLoader dataLoader) {
    //return new RecursiveFileScanner(dataLoader, ".yaml");
    //return new ThreadedFileScanner(dataLoader, ".yaml");
    return new WalkTreeFileScanner(dataLoader, ".yaml");
  }
}
