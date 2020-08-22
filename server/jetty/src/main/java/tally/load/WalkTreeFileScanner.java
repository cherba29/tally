package tally.load;

import tally.core.Budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Stopwatch;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class WalkTreeFileScanner implements FileScanner {
  private static final Logger logger = LoggerFactory.getLogger(WalkTreeFileScanner.class);

  private static final Yaml yaml = new Yaml();

  private final DataLoader loader;
  private final String fileSuffix;

  public WalkTreeFileScanner(DataLoader loader, String fileSuffix) {
    this.loader = loader;
    this.fileSuffix = fileSuffix;
  }

  @Override
  public void scan(Path path, final Budget.Builder builder) throws LoadException {
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.toString().endsWith(fileSuffix)) {
            FileInputStream dataFile = new FileInputStream(file.toFile());
            try {
              Stopwatch stopwatch = new Stopwatch().start();
              @SuppressWarnings("unchecked")
              Map<String,Object> data = (Map<String, Object>) yaml.load(dataFile);
              loader.load(data, builder);
              logger.info("Loaded {} {} ms", file.getFileName(), stopwatch.elapsedMillis());
            } catch (Exception e) {
              throw new IOException(e);
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      Throwable exception = e.getCause();
      if (exception instanceof LoadException) {
        throw (LoadException) exception;
      }
      throw new LoadException("Internal error", e);
    }
  }
}
