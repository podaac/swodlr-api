package gov.nasa.podaac.swodlr.security;

import gov.nasa.podaac.swodlr.user.User;
import gov.nasa.podaac.swodlr.user.UserReference;
import gov.nasa.podaac.swodlr.user.UserRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class UserBootstrapWebFilter implements WebFilter {
  public static final String SESSION_ATTRIBUTE_KEY = "user";

  private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

  @Autowired
  private SwodlrSecurityProperties securityProperties;

  @Autowired
  private UserRepository userRepository;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return Mono.defer(() -> {
      return exchange.getSession().publishOn(Schedulers.boundedElastic()).flatMap((session) -> {
        if (session.getAttributes().containsKey(SESSION_ATTRIBUTE_KEY)) {
          return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder
            .getContext()
            .map((securityContext) -> securityContext.getAuthentication())
            .filter((authentication) -> authentication != null)
            .cast(JwtAuthenticationToken.class)
            .flatMap((authenticationToken) -> retrieveUserInfo(session, authenticationToken))
            .then(chain.filter(exchange));
      });
    });
  }

  private Mono<Void> retrieveUserInfo(
      WebSession session,
      JwtAuthenticationToken authenticationToken
  ) {
    String tokenValue = authenticationToken.getToken().getTokenValue();
    String payload = new String(
        Base64.getDecoder().decode(tokenValue.split("\\.")[1]),
        StandardCharsets.UTF_8);

    Map<String, Object> parsedPayload = jsonParser.parseMap(payload);
    String uid = (String) parsedPayload.get("uid");
    URI userInfoUri = UriComponentsBuilder
        .fromHttpUrl(securityProperties.edlBaseUrl())
        .replacePath("/api/users/" + uid)
        .queryParam("client_id", securityProperties.edlClientId())
        .build().toUri();

    return WebClient.create()
        .get()
        .uri(userInfoUri)
        .headers(headers -> headers.setBearerAuth(tokenValue))
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .flatMap((body) -> processResponseBody(session, body));
  }

  private Mono<Void> processResponseBody(WebSession session, Map<String, Object> body) {
    String uid = (String) body.get("uid");
    String firstName = (String) body.get("first_name");
    String lastName = (String) body.get("last_name");
    String email = (String) body.get("email_address");

    Optional<User> userResult = userRepository.findByUsername(uid);
    User user;

    if (userResult.isPresent()) {
      user = userResult.get();

      user.firstName = firstName;
      user.lastName = lastName;
      user.email = email;
    } else {
      user = new User(uid, email, firstName, lastName);
    }

    userRepository.save(user);
    UserReference userReference = new UserReference(user);
    session.getAttributes().put(SESSION_ATTRIBUTE_KEY, userReference);
    session.save();

    return Mono.empty();
  }
}
