package gov.nasa.podaac.swodlr.security.edl;


import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nasa.podaac.swodlr.security.SwodlrSecurityProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

public final class EdlReactiveAuthenticationManager implements ReactiveAuthenticationManager {
  private static final String ROLE_PREFIX = "ROLE_";
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final WebClient webClient = WebClient.create();
  private final ReactiveAuthenticationManager jwtAuthManager;
  private final SwodlrSecurityProperties securityProperties;

  public EdlReactiveAuthenticationManager(
      ReactiveJwtDecoder jwtDecoder,
      SwodlrSecurityProperties securityProperties
  ) {
    this.jwtAuthManager = new JwtReactiveAuthenticationManager(jwtDecoder);
    this.securityProperties = securityProperties;
  }

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    return this.jwtAuthManager
      .authenticate(authentication)
      .filter(auth -> auth instanceof JwtAuthenticationToken)
      .cast(JwtAuthenticationToken.class)
      .flatMap(auth -> populateUserGroups(auth))
      .cast(Authentication.class);
  }

  private Mono<Authentication> populateUserGroups(JwtAuthenticationToken auth) {
    return retrieveUserGroups(auth.getToken())
      .map(groups -> {
        List<GrantedAuthority> authorities = new ArrayList<>(auth.getAuthorities());

        for (String group : groups) {
          logger.debug("Adding user role: {}", group);
          authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + group));
        }

        return new JwtAuthenticationToken(auth.getToken(), authorities);
      })
      .cast(Authentication.class)
      .cache();
  }

  private Mono<List<String>> retrieveUserGroups(Jwt token) {
    return Mono.defer(() -> {
      String username = token.getClaim("uid");

      URI userGroupsUri = UriComponentsBuilder
          .fromHttpUrl(securityProperties.edlBaseUrl())
          .replacePath("/api/user_groups/groups_for_user/" + username)
          .queryParam("client_id", securityProperties.edlClientId())
          .build().toUri();

      logger.debug("Constructed user groups uri: {}", userGroupsUri.toString());

      return webClient
        .get()
        .uri(userGroupsUri)
        .headers(headers -> headers.setBearerAuth(token.getTokenValue()))
        .retrieve()
        .bodyToMono(EdlUserGroupsResponse.class)
        .flatMap(body -> processResponse(body));
    });
  }

  private Mono<List<String>> processResponse(EdlUserGroupsResponse response) {
    return Mono.defer(() -> {
      logger.trace("Processing user groups response");

      List<UserGroup> userGroups = response.userGroups();
      List<String> swodlrGroups = new ArrayList<>();

      for (UserGroup group : userGroups) {
        if (group.clientId().equals(securityProperties.edlClientId())) {
          swodlrGroups.add(group.name());
        } else {
          logger.debug(
              "Group client id {} does not match application client id {}",
              group.clientId(), securityProperties.edlClientId());
        }
      }

      return Mono.just(Collections.unmodifiableList(swodlrGroups));
    });
  }

  private record EdlUserGroupsResponse(
      @JsonProperty("user_groups") List<UserGroup> userGroups
  ) { }

  private record UserGroup(
      @JsonProperty("group_id") String groupId,
      @JsonProperty("name") String name,
      @JsonProperty("tag") String tag,
      @JsonProperty("shared_user_group") boolean sharedUserGroup,
      @JsonProperty("created_by") String createdBy,
      @JsonProperty("app_uid") String appUid,
      @JsonProperty("client_id") String clientId
  ) { }
}
