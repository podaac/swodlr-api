package gov.nasa.podaac.swodlr.security;

import gov.nasa.podaac.swodlr.Environment;
import gov.nasa.podaac.swodlr.SwodlrProperties;
import gov.nasa.podaac.swodlr.security.authentication.client.JweCookieReactiveOauth2AuthorizedClientService;
import gov.nasa.podaac.swodlr.security.authentication.handlers.SuccessMessageAuthenticationSuccessHandler;
import gov.nasa.podaac.swodlr.security.authentication.handlers.UserBootstrapAuthenticationSuccessHandler;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.DelegatingServerAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@EnableWebFluxSecurity
@Profile({"!test"})
public class WebSecurityConfig {
  private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
  private final ReactiveOAuth2AuthorizedClientService authorizedClientService; 

  @Autowired
  private UserBootstrapAuthenticationSuccessHandler userBootstrapHandler;

  @Autowired
  private SuccessMessageAuthenticationSuccessHandler successHandler;

  @Autowired
  private SwodlrProperties swodlrProperties;

  public WebSecurityConfig(ReactiveClientRegistrationRepository clientRegistrationRepository) {
    authorizedClientService = new JweCookieReactiveOauth2AuthorizedClientService();
    authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
      clientRegistrationRepository, authorizedClientService
    );
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http
        .cors().and()
        .csrf().disable()
        .authorizeExchange(authorize -> {
          authorize.anyExchange().authenticated();
        })
        .oauth2Client().and()
        .oauth2Login((login) -> {
          var authenticationSuccessHandler = new DelegatingServerAuthenticationSuccessHandler(
              userBootstrapHandler, successHandler);

          login.authenticationSuccessHandler(authenticationSuccessHandler);
          login.authorizedClientService(authorizedClientService);
        });

    return http.build();
  }

  @Bean
  public ReactiveOAuth2AuthorizedClientManager authorizedClientManager() {
    return authorizedClientManager;
  }

  @Bean
  public ReactiveOAuth2AuthorizedClientService authorizedClientService() {
    return authorizedClientService;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    
    if (swodlrProperties.getEnv() == Environment.DEV) {
      config.addAllowedMethod(HttpMethod.GET);
      config.addAllowedMethod(HttpMethod.POST);
      config.addAllowedMethod(HttpMethod.HEAD);
      config.setAllowCredentials(true);
      config.setAllowedOriginPatterns(Collections.singletonList("*"));
    }

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
