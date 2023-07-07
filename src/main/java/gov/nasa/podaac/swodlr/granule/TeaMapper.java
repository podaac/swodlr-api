package gov.nasa.podaac.swodlr.granule;

import gov.nasa.podaac.swodlr.SwodlrProperties;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TeaMapper {
  @Autowired
  private SwodlrProperties swodlrProperties;

  public URI convertS3Uri(URI s3Uri) throws URISyntaxException{
    String bucketName = s3Uri.getAuthority();
    String teaHost = swodlrProperties.teaMapping().get(bucketName);
    String path = "/" + bucketName + s3Uri.getPath();

    URI teaUri = new URI("https", teaHost, path, null);
    return teaUri;
  }
}
