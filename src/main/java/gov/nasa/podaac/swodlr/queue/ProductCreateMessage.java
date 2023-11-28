package gov.nasa.podaac.swodlr.queue;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import java.util.UUID;

public record ProductCreateMessage(
    @JsonProperty("product_id") UUID productId,
    int cycle,
    int pass,
    int scene,
    @JsonProperty("output_granule_extent_flag") Boolean outputGranuleExtentFlag,
    @JsonProperty("output_sampling_grid_type") GridType outputSamplingGridType,
    @JsonProperty("raster_resolution") Integer rasterResolution,
    @JsonProperty("utm_zone_adjust") Integer utmZoneAdjust,
    @JsonProperty("mgrs_band_adjust") Integer mgrsBandAdjust
) {
  public ProductCreateMessage(L2RasterProduct product) {
    this(
        product.getId(),
        product.getCycle(),
        product.getPass(),
        product.getScene(),
        product.getOutputGranuleExtentFlag(),
        product.getOutputSamplingGridType(),
        product.getRasterResolution(),
        product.getUtmZoneAdjust(),
        product.getMgrsBandAdjust()
    );
  }
}
