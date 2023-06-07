package gov.nasa.podaac.swodlr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("swodlr")
@ConstructorBinding 
public class SwodlrProperties {
  private Environment env;

  public SwodlrProperties(
    @DefaultValue("PROD") String env
  ) {
    this.env = Environment.valueOf(env);
  }

  public Environment getEnv() {
    return env;
  }
}
