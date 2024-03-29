package gov.nasa.podaac.swodlr.security;

import gov.nasa.podaac.swodlr.user.User;
import gov.nasa.podaac.swodlr.user.UserReference;
import gov.nasa.podaac.swodlr.user.UserRepository;
import java.util.Collections;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Profile("test")
public class MockGraphQlUserInjector implements WebGraphQlInterceptor {
  private static final UUID MOCK_USER_ID = UUID.fromString("fee1dc78-0604-4fa6-adae-0b4b55440e7d");

  private User mockUser;
  private UserReference mockUserRef;

  public MockGraphQlUserInjector(@Autowired UserRepository userRepository) {
    mockUser = userRepository.findById(MOCK_USER_ID).get();
    mockUserRef = new UserReference(mockUser);
  }

  @Override
  public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
    return Mono.defer(() -> {
      request.configureExecutionInput((executionInput, builder) -> {
        builder.graphQLContext(Collections.singletonMap("userRef", mockUserRef));
        return builder.build();
      });
      return chain.next(request);
    });
  }

  @Bean
  User mockUser() {
    return mockUser;
  }
}
