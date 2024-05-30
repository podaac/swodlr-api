package gov.nasa.podaac.swodlr.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

@Component
public class GraphQlRequestLogger extends SimpleInstrumentation {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
  
  @Override
  public InstrumentationContext<Object> beginFieldFetch(
      InstrumentationFieldFetchParameters parameters
  ) {
    return SimpleInstrumentationContext.whenDispatched((future) -> {
      try {
        logger.info(objectMapper.writeValueAsString(new GraphQlRequest(parameters)));
      } catch (JsonProcessingException jsonEx) {
        logger.error("Exception thrown while serializing GraphQL request", jsonEx);
      }
    });
  }
}
