package gov.nasa.podaac.swodlr.security.config;

import gov.nasa.podaac.swodlr.security.SwodlrSecurityProperties;
import gov.nasa.podaac.swodlr.security.UserBootstrapWebFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@Profile({"!test"})
public class WebSecurityConfig {
  @Autowired
  private SwodlrSecurityProperties securityProperties;

  @Autowired
  private UserBootstrapWebFilter userBootstrapWebFilter;

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http
        .cors().and()
        .csrf().disable()
        .oauth2ResourceServer((resourceServer) -> {
          resourceServer.jwt((jwt) -> {
            jwt.jwkSetUri(securityProperties.edlBaseUrl() + "/export_edl_jwks");
          });
        })
        .addFilterAfter(userBootstrapWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);

    return http.build();
  }
}
