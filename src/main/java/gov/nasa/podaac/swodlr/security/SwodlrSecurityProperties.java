package gov.nasa.podaac.swodlr.security;

import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.security.crypto.codec.Hex;

@ConfigurationProperties("swodlr.security")
@ConstructorBinding 
public class SwodlrSecurityProperties {
  private final JWEEncrypter encrypter;
  private final JWEDecrypter decrypter;
  private final Duration sessionLength;
  private final String edlBaseUrl;
  private final String edlJwksPath;
  private final String edlClientId;
  private final String edlClientSecret;

  /**
   * Configuration properties for swodlr sessions.
   *
   * @param sessionEncryptionKey Encryption key to use for web sessions.
   *                             Must be 128 bits (16 bytes).
   * @param sessionLength How long sessions are valid
   */
  public SwodlrSecurityProperties(
      String sessionEncryptionKey,
      Duration sessionLength,
      String edlBaseUrl,
      String edlJwksPath,
      String edlClientId,
      String edlClientSecret
  ) {
    byte[] key = Hex.decode(sessionEncryptionKey);

    if (key.length != 16) {
      throw new IllegalArgumentException("SWODLR only supports 128 bit encryption keys.");
    }

    try {
      this.encrypter = new DirectEncrypter(key);
      this.decrypter = new DirectDecrypter(key);
    } catch (KeyLengthException e) {
      throw new IllegalArgumentException("SWODLR only supports 128 bit encryption keys.", e);
    }
    
    this.sessionLength = sessionLength;
    this.edlBaseUrl = edlBaseUrl;
    this.edlJwksPath = edlJwksPath;
    this.edlClientId = edlClientId;
    this.edlClientSecret = edlClientSecret;
  }

  public JWEEncrypter encrypter() {
    return encrypter;
  }

  public JWEDecrypter decrypter() {
    return decrypter;
  }

  public Duration sessionLength() {
    return sessionLength;
  }

  public String edlBaseUrl() {
    return edlBaseUrl;
  }

  public String edlJwksPath() {
    return edlJwksPath;
  }

  public String edlClientId() {
    return edlClientId;
  }

  public String edlClientSecret() {
    return edlClientSecret;
  }
}
