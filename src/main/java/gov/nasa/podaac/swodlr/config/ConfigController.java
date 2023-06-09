package gov.nasa.podaac.swodlr.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class ConfigController {
  private ConfigResponse response;

  public ConfigController() {
    response = new ConfigResponse("/oauth2/authorization/edl");
  }

  @GetMapping
  public ConfigResponse getConfig() {
    return response;
  }
}
