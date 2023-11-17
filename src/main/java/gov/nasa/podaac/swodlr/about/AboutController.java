package gov.nasa.podaac.swodlr.about;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AboutController {
  private final Properties versionProperties;

  public AboutController() throws IOException {
    versionProperties = PropertiesLoaderUtils.loadAllProperties("version.properties");
  }

  @GetMapping("/about")
  public About getAbout() {
    return new About(
      versionProperties.getProperty("version"), 
      ManagementFactory.getRuntimeMXBean().getUptime(),
      System.currentTimeMillis()
    );
  }
}
