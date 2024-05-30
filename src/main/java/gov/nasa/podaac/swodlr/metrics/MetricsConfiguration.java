package gov.nasa.podaac.swodlr.metrics;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {
  @Autowired
  private GraphQlRequestLogger graphQlRequestLogger;

  @Bean
  public GraphQlSourceBuilderCustomizer graphQlSourceBuilderCustomizer() {
    return customizer -> {
      customizer.instrumentation(List.of(graphQlRequestLogger));
    };
  }
}
