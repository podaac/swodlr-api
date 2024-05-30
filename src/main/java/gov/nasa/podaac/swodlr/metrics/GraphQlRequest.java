package gov.nasa.podaac.swodlr.metrics;

import gov.nasa.podaac.swodlr.security.GraphQlUserInjector;
import gov.nasa.podaac.swodlr.user.UserReference;
import graphql.GraphQLContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;

public class GraphQlRequest {
  private final String username;
  private final String operation;
  private final String path;
  private final String executionId;
  private final Map<String, Object> arguments;

  public GraphQlRequest(InstrumentationFieldFetchParameters parameters) {
    ExecutionStepInfo execStepInfo = parameters.getExecutionStepInfo();
    DataFetchingEnvironment environment = parameters.getEnvironment();
    OperationDefinition opDef = environment.getOperationDefinition();
    GraphQLContext graphQlContext = environment.getGraphQlContext();

    UserReference userReference = graphQlContext.get(GraphQlUserInjector.USER_REFERENCE_KEY);
    if (userReference == null) {
      this.username = null;
    } else {
      this.username = userReference.fetch().getUsername();
    }

    this.operation = opDef.getOperation().name();
    this.path = execStepInfo.getPath().toString();
    this.executionId = environment.getExecutionId().toString();
    this.arguments = (environment.getArguments() == null)
      ? Map.copyOf(environment.getArguments()) : Map.of();
  }

  public Map<String, Object> getArguments() {
    return this.arguments;
  }

  public String getPath() {
    return this.path;
  }

  public String getUser() {
    return this.username;
  }

  public String getOperation() {
    return this.operation;
  }

  public String getExecutionId() {
    return this.executionId;
  }
}
