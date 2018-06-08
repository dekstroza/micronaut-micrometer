package io.micronaut.configuration.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.configuration.metrics.aggregator.MeterRegistryConfigurer;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;

import javax.inject.Singleton;
import java.util.stream.Stream;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_ENABLED;


/**
 * Startup event listener that will add a simple meter registry
 * if composite is present but no other registries have been created.
 *
 * @author Christian Oestreich
 * @since 1.0
 */
@Singleton
@Requires(property = MICRONAUT_METRICS_ENABLED, value = "true", defaultValue = "true")
public class SimpleMeterRegistryEventListener implements ApplicationEventListener<StartupEvent> {

    /**
     * Application event method.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(StartupEvent event) {
        BeanContext applicationContext = event.getSource();

        Stream<MeterRegistry> meterRegistries = applicationContext.streamOfType(MeterRegistry.class);
        CompositeMeterRegistry compositeMeterRegistry = applicationContext
                .findBean(CompositeMeterRegistry.class)
                .orElse(null);

        if (compositeMeterRegistry != null && doesRequiresSimpleMeterRegistry(meterRegistries)) {
            SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
            applicationContext.streamOfType(MeterRegistryConfigurer.class)
                    .forEach(meterRegistryConfigurer -> {
                        if (meterRegistryConfigurer.supports(simpleMeterRegistry)) {
                            meterRegistryConfigurer.configure(simpleMeterRegistry);
                        }
                    });
            compositeMeterRegistry.add(simpleMeterRegistry);
            applicationContext.registerSingleton(simpleMeterRegistry);
        }
    }

    /**
     * Check if there are any non-composite meter registries, in which case we do not require
     * a simple meter registry.
     *
     * @param meterRegistries Stream of meter registries to check
     * @return boolean of whether or not simple meter registry is required.
     */
    private boolean doesRequiresSimpleMeterRegistry(Stream<MeterRegistry> meterRegistries) {
        return meterRegistries != null &&
                !meterRegistries.anyMatch(meterRegistry -> !(meterRegistry instanceof CompositeMeterRegistry));
    }
}
