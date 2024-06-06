package gov.nasa.podaac.swodlr.security.edl;

import gov.nasa.podaac.swodlr.security.SwodlrSecurityProperties;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.Size;
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
  public static final String CODE_CHALLENGE_SESSION_KEY = "edl-code-challenge";

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
      @RequestParam("response_type") String responseType,
      @RequestParam("client_id") String clientId,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("code_challenge") @Size(min = 64, max = 64) String codeChallenge,
      @RequestParam("code_challenge_method") String codeChallengeMethod
  ) {
    return Mono.defer(() -> {
      if (!codeChallengeMethod.equals("S256")) {
        return Mono.just(
          ResponseEntity.badRequest().body(Map.ofEntries(
            Map.entry("error", "invalid_request"),
            Map.entry("error_description", "code_challenge_method can only be 'S256'")
          ))
        );
      }

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
        session.getAttributes().put(CODE_CHALLENGE_SESSION_KEY, codeChallenge);
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
      @RequestParam("grant_type") String grantType,
      @RequestParam("code_verifier") String codeVerifier,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam(name = "code", required = false) String code,
      @RequestParam(name = "refresh_token", required = false) String refreshToken
  ) {
    return exchange.getSession().flatMap((session) -> {
      if (grantType.equals("authorization_code")) {
        // First verify PKCE
        String codeChallenge = session.getAttribute(CODE_CHALLENGE_SESSION_KEY);

        // Hash the code challenge with SHA256
        String hash = DigestUtils.sha256Hex(codeVerifier);
        if (!hash.equals(codeChallenge)) {
          logger.debug(
              "Code verification failed; verifier: %s, challenge: %s, hash: %s",
              codeVerifier, codeChallenge, hash
          );
          return Mono.just(
            ResponseEntity.badRequest().body(Map.ofEntries(
              Map.entry("error", "invalid_request"),
              Map.entry("error_details", "code verification failed")
            ))
          );
        }

        logger.debug("PKCE check passed");
        session.getAttributes().remove(CODE_CHALLENGE_SESSION_KEY);
        session.save();
      } else if (!grantType.equals("refresh_token")) {
        return Mono.just(
          ResponseEntity.badRequest().body(Map.ofEntries(
            Map.entry("error", "invalid_request"),
            Map.entry("error_details", "specified grant_type not supported")
          ))
        );
      }

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
