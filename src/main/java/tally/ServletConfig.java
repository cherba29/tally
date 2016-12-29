package tally;

import tally.load.DataLoaderModule;
import tally.load.FileScannerModule;
import tally.render.ResponseRenderer;
import tally.render.jackson.JacksonRenderer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class ServletConfig extends GuiceServletContextListener {
  @Override
  protected Injector getInjector() {
    return Guice.createInjector(new JerseyServletModule() {
      @Override
      protected void configureServlets() {
        install(new DataLoaderModule());
        install(new FileScannerModule());
        bind(ResponseRenderer.class).to(JacksonRenderer.class);
        bind(Processor.class);
        bind(BudgetHandler.class);

        // Route all requests through GuiceContainer.
        serve("/*").with(GuiceContainer.class);
      }
    });
  }

}
