package tally.load;

import tally.load.yaml.YamlDataLoader;

import com.google.inject.AbstractModule;

public class DataLoaderModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DataLoader.class).to(YamlDataLoader.class);
    }
}
