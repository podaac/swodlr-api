package gov.nasa.podaac.swodlr.security.authentication.handlers;


import gov.nasa.podaac.swodlr.security.authentication.RedirectingServerOauth2AuthorizationRequestResolver;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RedirectOrMessageAuthenticationSuccessHandler
    implements ServerAuthenticationSuccessHandler {
  public static final byte[] PLAIN_MESSAGE
      = "Authentication successful - please resend your request"
        .getBytes(StandardCharsets.UTF_8);

  @Override
  public Mono<Void> onAuthenticationSuccess(
      WebFilterExchange webFilterExchange,
      Authentication authentication
  ) {
    ServerWebExchange exchange = webFilterExchange.getExchange();
    ServerHttpRequest req = exchange.getRequest();
    ServerHttpResponse res = exchange.getResponse();

    return exchange
      .getSession()
      .flatMap((session) -> {
        String redirectUri = (String) session.getAttributes().remove(
            RedirectingServerOauth2AuthorizationRequestResolver.FRONTEND_REDIRECT_SESSION_KEY
        );

        if (redirectUri == null) {
          if (acceptsTextPlain(req.getHeaders().getAccept())) {
            DataBuffer buffer = res.bufferFactory().wrap(PLAIN_MESSAGE);
            res.getHeaders().setContentType(MediaType.TEXT_PLAIN);
            return res.writeWith(Mono.just(buffer));
          }
        } else {
          res.setStatusCode(HttpStatus.FOUND);
          res.getHeaders().setLocation(URI.create(redirectUri));
        }

        return Mono.empty();
      });
  }

  private boolean acceptsTextPlain(List<MediaType> accepts) {
    for (MediaType type : accepts) {
      if (type.includes(MediaType.TEXT_PLAIN)) {
        return true;
      }
    }

    return false;
  }
}
