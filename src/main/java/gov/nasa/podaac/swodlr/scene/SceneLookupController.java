package gov.nasa.podaac.swodlr.scene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class SceneLookupController {
  @Autowired
  private SceneLookupService sceneLookupService;

  @QueryMapping
  public Mono<Boolean> availableScene(
      @Argument int cycle,
      @Argument int pass,
      @Argument int scene
  ) {
    return sceneLookupService.sceneExists(cycle, pass, scene);
  }
}
