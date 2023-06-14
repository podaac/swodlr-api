package gov.nasa.podaac.swodlr.security.authentication;

import gov.nasa.podaac.swodlr.security.SwodlrSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RedirectingServerOauth2AuthorizationRequestResolver
    extends DefaultServerOAuth2AuthorizationRequestResolver {
  public static final String FRONTEND_REDIRECT_SESSION_KEY = "FRONTEND_REDIRECT";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private SwodlrSecurityProperties securityProperties;

  public RedirectingServerOauth2AuthorizationRequestResolver(
      ReactiveClientRegistrationRepository clientRegistrationRepository
  ) {
    super(clientRegistrationRepository);
  }

  @Override
  public Mono<OAuth2AuthorizationRequest> resolve(
      ServerWebExchange exchange,
      String clientRegistrationId
  ) {
    return super.resolve(exchange, clientRegistrationId)
        .flatMap((OAuth2AuthorizationRequest authReq) -> {
          String redirect = exchange.getRequest().getQueryParams().getFirst("redirect");
          if (
              redirect != null
              && securityProperties.frontendUriPattern().matcher(redirect).matches()
          ) {
            logger.debug("Saving redirect URI to session: {}", redirect);
            return exchange.getSession()
              .doOnNext((session) -> {
                session.getAttributes().put(FRONTEND_REDIRECT_SESSION_KEY, redirect);
              })
              .thenReturn(authReq);
          }

          logger.debug("No redirect URI found");
          return Mono.just(authReq);
        });
  }
}
