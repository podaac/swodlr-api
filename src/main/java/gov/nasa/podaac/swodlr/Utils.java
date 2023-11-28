package gov.nasa.podaac.swodlr;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class Utils implements ApplicationContextAware {
  private static ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    Utils.applicationContext = applicationContext;
  }

  public static ApplicationContext applicationContext() {
    return applicationContext;
  }
}
