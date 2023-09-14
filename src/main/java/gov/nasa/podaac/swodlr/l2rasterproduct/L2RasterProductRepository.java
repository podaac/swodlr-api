package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface L2RasterProductRepository extends
    JpaRepository<L2RasterProduct, UUID>, L2RasterProductQuery {
  List<L2RasterProduct> findById(L2RasterProduct product);

  @SuppressWarnings("LineLength")
  @Query(
      value = """
      SELECT * FROM \"L2RasterProducts\" WHERE
      \"cycle\" = :cycle AND
      \"pass\" = :pass AND
      \"scene\" = :scene AND
      \"outputGranuleExtentFlag\" = :outputGranuleExtentFlag AND
      \"outputSamplingGridType\" = :#{#gridType.toString()} AND
      \"rasterResolution\" = :rasterResolution AND
      (\"utmZoneAdjust\" = :utmZoneAdjust OR (:utmZoneAdjust is NULL and \"utmZoneAdjust\" is NULL)) AND
      (\"mgrsBandAdjust\" = :mgrsBandAdjust OR (:mgrsBandAdjust is NULL and \"mgrsBandAdjust\" is NULL))
      """,
      nativeQuery = true
  )
  Optional<L2RasterProduct> findOneByParameters(
      int cycle,
      int pass,
      int scene,
      boolean outputGranuleExtentFlag,
      @Param("gridType") GridType outputSamplingGridType,
      int rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust
  );
}
