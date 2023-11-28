package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.granule.Granule;
import gov.nasa.podaac.swodlr.producthistory.ProductHistory;
import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.status.Status;
import gov.nasa.podaac.swodlr.user.User;
import gov.nasa.podaac.swodlr.validation.ValidRasterOptions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "L2RasterProducts")
@ValidRasterOptions
public class L2RasterProduct {
  @Id
  private UUID id;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @Column(nullable = false)
  private int cycle;

  @Column(nullable = false)
  private int pass;

  @Column(nullable = false)
  private int scene;

  @Column(nullable = false)
  private Boolean outputGranuleExtentFlag;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private GridType outputSamplingGridType;

  @Column(nullable = false)
  private Integer rasterResolution;

  @Column
  private Integer utmZoneAdjust;

  @Column
  private Integer mgrsBandAdjust;

  @OneToMany(mappedBy = "product")
  @OrderBy("timestamp DESC")
  private List<Status> statuses;

  @OneToMany(mappedBy = "product")
  private Set<Granule> granules;

  @ManyToMany
  @JoinTable(
      name = "ProductHistory",
      joinColumns = @JoinColumn(name = "rasterProductId"),
      inverseJoinColumns = @JoinColumn(name = "requestedById")
  )
  private Set<User> users;

  @OneToMany(mappedBy = "id.rasterProduct")
  private Set<ProductHistory> history;

  public L2RasterProduct() { }

  public L2RasterProduct(
      int cycle,
      int pass,
      int scene,
      boolean outputGranuleExtentFlag,
      GridType outputSamplingGridType,
      int rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust
  ) {
    this.id = UUID.randomUUID();
    this.timestamp = LocalDateTime.now();
    this.cycle = cycle;
    this.pass = pass;
    this.scene = scene;
    this.outputGranuleExtentFlag = outputGranuleExtentFlag;
    this.outputSamplingGridType = outputSamplingGridType;
    this.rasterResolution = rasterResolution;
    this.utmZoneAdjust = utmZoneAdjust;
    this.mgrsBandAdjust = mgrsBandAdjust;
  }

  public UUID getId() {
    return id;
  }

  public int getCycle() {
    return cycle;
  }

  public int getPass() {
    return pass;
  }

  public int getScene() {
    return scene;
  }

  public boolean getOutputGranuleExtentFlag() {
    return outputGranuleExtentFlag;
  }

  public GridType getOutputSamplingGridType() {
    return outputSamplingGridType;
  }

  public int getRasterResolution() {
    return rasterResolution;
  }

  public Integer getUtmZoneAdjust() {
    return utmZoneAdjust;
  }

  public Integer getMgrsBandAdjust() {
    return mgrsBandAdjust;
  }

  public Set<Granule> getGranules() {
    return granules;
  }

  public List<Status> getStatuses() {
    return statuses;
  }
}
