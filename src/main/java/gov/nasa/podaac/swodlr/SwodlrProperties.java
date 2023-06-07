package gov.nasa.podaac.swodlr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties("swodlr")
@ConstructorBinding 
public class SwodlrProperties {
  private Environment env;

  public SwodlrProperties(String env) {
    this.env = Environment.valueOf(env);
  }

  public Environment getEnv() {
    return env;
  }
}
