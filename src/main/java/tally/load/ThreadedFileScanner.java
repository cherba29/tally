package tally.load;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Stopwatch;

import tally.core.Budget;

public class ThreadedFileScanner implements FileScanner {
  private static final Logger logger = LoggerFactory.getLogger(ThreadedFileScanner.class);

  private static ThreadLocal<Yaml> yaml = new ThreadLocal<Yaml>() {
    @Override protected Yaml initialValue() {
      return new Yaml();
    }
  };

  private final DataLoader loader;
  private final String fileSuffix;
  private final ExecutorService executor = Executors.newFixedThreadPool(4);

  public ThreadedFileScanner(DataLoader loader, String fileSuffix) {
    this.loader = loader;
    this.fileSuffix = fileSuffix;
  }

  @Override
  public void scan(Path path, Budget.Builder builder) throws LoadException {
    final List<Future<Budget.Builder>> datas = new ArrayList<>();
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (file.toString().endsWith(fileSuffix)) {
            datas.add(executor.submit(new Task(file, loader)));
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new LoadException("Internal Error", e);
    }
    for (Future<Budget.Builder> future : datas) {
      try {
        Budget.Builder budgetBuilder = future.get();
        builder.mergeIn(budgetBuilder);
      } catch (InterruptedException e) {
        logger.error(e.toString());
        throw new LoadException("Got InterruptedException", e);
      } catch (ExecutionException e) {
        Throwable exception = e.getCause();
        logger.error(exception.toString());
        if (exception instanceof LoadException) {
          throw (LoadException) exception;
        }
        throw new LoadException("Unknown error", exception);
      }
    }
    executor.shutdown();
  }

  private static class Task implements Callable<Budget.Builder> {
    private final Path path;
    private final DataLoader loader;

    public Task(Path path, DataLoader loader) {
      this.path = path;
      this.loader = loader;
    }

    private static String readFile(Path path) throws IOException {
      byte[] encoded = Files.readAllBytes(path);
      return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).asReadOnlyBuffer().toString();
    }

    @Override
    public Budget.Builder call() throws Exception {
      Stopwatch stopwatch = new Stopwatch().start();
      String yamlText = readFile(path);
      @SuppressWarnings("unchecked")
      Map<String,Object> data = (Map<String, Object>) yaml.get().load(yamlText);
      Budget.Builder budgetBuilder = new Budget.Builder();
      loader.load(data, budgetBuilder);
      logger.info("Loaded {} {} ms", path.getFileName(), stopwatch.elapsedMillis());
      return budgetBuilder;
    }
  }
}
