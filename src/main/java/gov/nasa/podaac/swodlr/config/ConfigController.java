package gov.nasa.podaac.swodlr.config;

import java.util.Map;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

@RestController
@RequestMapping("/config")
public class ConfigController {
  private ConfigResponse response;

  public ConfigController() {
    var authorizationUri = new UriTemplate(
        DefaultServerOAuth2AuthorizationRequestResolver.DEFAULT_AUTHORIZATION_REQUEST_PATTERN
    ).expand(Map.ofEntries(
        Map.entry("registrationId", "edl")
    )).toString();
    response = new ConfigResponse(authorizationUri);
  }

  @GetMapping
  public ConfigResponse getConfig() {
    return response;
  }
}
