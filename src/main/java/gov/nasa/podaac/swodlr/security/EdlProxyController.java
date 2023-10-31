package gov.nasa.podaac.swodlr.security;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("edl/oauth")
public class EdlProxyController {
  public static final String CODE_VERIFIER_SESSION_KEY = "edl-code-verifier";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final SwodlrSecurityProperties securityProperties;
  private final WebClient edlClient;

  public EdlProxyController(@Autowired SwodlrSecurityProperties securityProperties) {
    this.securityProperties = securityProperties;

    this.edlClient = WebClient.builder()
        .baseUrl(securityProperties.edlBaseUrl())
        .defaultHeaders((headers) -> {
          headers.setBasicAuth(
              securityProperties.edlClientId(),
              securityProperties.edlClientSecret()
          );
        })
        .build();
  }
 
  @GetMapping("authorize")
  public Mono<ResponseEntity<?>> getAuthorize(
      ServerWebExchange exchange,
      @NotNull @RequestParam("response_type") String responseType,
      @NotNull @RequestParam("client_id") String clientId,
      @NotNull @RequestParam("redirect_uri") String redirectUri,
      @NotNull @RequestParam("code_verifier") String codeVerifier) {
    return Mono.defer(() -> {
      if (!responseType.equals("code")) {
        return Mono.just(
            ResponseEntity.badRequest().body(Map.ofEntries(
                Map.entry("error", "invalid_request"),
                Map.entry("error_description", "response_type can only be 'code'"))));
      }

      if (!clientId.equals(securityProperties.edlClientId())) {
        return Mono.just(
            ResponseEntity.badRequest().body(Map.ofEntries(
                Map.entry("error", "invalid_request"),
                Map.entry("error_description", "client_id does not match"))));
      }

      return exchange.getSession().flatMap((session) -> {
        session.getAttributes().put(CODE_VERIFIER_SESSION_KEY, codeVerifier);
        session.save();

        URI edlAuthorizeUri = UriComponentsBuilder
            .fromUriString(securityProperties.edlBaseUrl())
            .replacePath("/oauth/authorize")
            .replaceQueryParams(CollectionUtils.toMultiValueMap(Map.ofEntries(
                Map.entry("response_type", List.of(responseType)),
                Map.entry("client_id", List.of(clientId)),
                Map.entry("redirect_uri", List.of(redirectUri)))))
            .build(false)
            .toUri()
            .normalize();

        return Mono.just(
            ResponseEntity.status(HttpStatus.FOUND).location(edlAuthorizeUri).build()
        );
      });
    });
  }

  @PostMapping("token")
  public Mono<ResponseEntity<Map<String, Object>>> postToken(
      ServerWebExchange exchange,
      @NotNull @RequestParam("grant_type") String grantType,
      @NotNull @RequestParam("code_challenge") String codeChallenge,
      @NotNull @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("code") String code,
      @RequestParam("refresh_token") String refreshToken
  ) {
    return exchange.getSession().flatMap((session) -> {
      // First verify PKCE
      String codeVerifier = session.getAttribute(CODE_VERIFIER_SESSION_KEY);

      // Hash the code challenge with SHA256
      String hash = DigestUtils.sha256Hex(codeChallenge);
      if (!hash.equals(codeVerifier)) {
        logger.debug(
            "Code challenge failed; verifier: %s, challenge: %s, hash: %s",
            codeVerifier, codeChallenge, hash
        );
        return Mono.just(
          ResponseEntity.badRequest().body(Map.ofEntries(
            Map.entry("error", "invalid_request"),
            Map.entry("error_details", "code challenge check failed")
          ))
        );
      }

      logger.debug("PKCE check passed");
      session.getAttributes().remove(CODE_VERIFIER_SESSION_KEY);
      session.save();

      return edlClient
          .post()
          .uri((uriBuilder) -> uriBuilder
              .path("/oauth/token")
              .queryParam("grant_type", grantType)
              .queryParam("redirect_uri", redirectUri)
              .queryParamIfPresent("code", Optional.ofNullable(code))
              .queryParamIfPresent("refresh_token", Optional.ofNullable(refreshToken))
              .build()
          )
          .retrieve()
          // Bypass DefaultWebClient's status error handler
          .onRawStatus((status) -> true, (response) -> Mono.empty())
          .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
    });
  }
}
