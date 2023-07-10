package gov.nasa.podaac.swodlr.granule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nasa.podaac.swodlr.Utils;
import gov.nasa.podaac.swodlr.exception.SwodlrException;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "Granules")
public class Granule {
  @Id
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "productId", nullable = false)
  private L2RasterProduct product;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @Column(nullable = false)
  private String uri;

  Granule() { }

  public Granule(L2RasterProduct product, String uri) {
    this.id = UUID.randomUUID();
    this.timestamp = LocalDateTime.now();
    this.product = product;
    this.uri = uri;
  }

  public UUID getId() {
    return id;
  }

  public L2RasterProduct getProduct() {
    return product;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  @JsonIgnore
  public String getS3Uri() {
    return uri;
  }

  public String getUri() {
    try {
      return Utils
        .applicationContext()
        .getBean(TeaMapper.class)
        .convertS3Uri(URI.create(uri))
        .toString();
    } catch (URISyntaxException ex) {
      throw new SwodlrException(
        "Error generating download link. Please contact support"
      );
    }
  }
}
