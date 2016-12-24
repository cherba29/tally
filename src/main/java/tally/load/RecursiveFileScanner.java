package tally.load;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Stopwatch;

import tally.core.Budget;

public class RecursiveFileScanner implements FileScanner {
  private static final Logger logger = LoggerFactory.getLogger(RecursiveFileScanner.class);

  private static final Yaml yaml = new Yaml();

  private final DataLoader loader;
  private final FileFilter fileFilter;

  public RecursiveFileScanner(DataLoader loader, final String fileSuffix) {
    this.loader = loader;
    this.fileFilter = new FileFilter() {
      @Override
      public boolean accept(File pathName) {
        return pathName.isDirectory() || pathName.getName().endsWith(fileSuffix);
      }
    };
  }

  @Override
  public void scan(Path dir, Budget.Builder builder) throws LoadException {
    File[] files = dir.toFile().listFiles(fileFilter);
    for (File file : files) {
      if (file.isDirectory()) {
        scan(file.toPath(), builder);
      } else {
        FileInputStream dataFile = null;
        try {
          dataFile = new FileInputStream(file);
        } catch (FileNotFoundException e) {
          throw new LoadException("Internal error", e);
        }

        Stopwatch stopwatch = new Stopwatch().start();
        Map<String,Object> data;
        try {
          @SuppressWarnings("unchecked")
          Map<String,Object> loadedData = (Map<String, Object>) yaml.load(dataFile);
          data = loadedData;
        } catch (Exception e) {
          throw new LoadException("Failed to load", e);
        }
        loader.load(data, builder);
        logger.info("Loaded {} in {}ms", file.getName(), stopwatch.elapsedMillis());
      }
    }
  }
}
