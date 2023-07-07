package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface L2RasterProductRepository extends JpaRepository<L2RasterProduct, UUID>, L2RasterProductQuery {
  List<L2RasterProduct> findById(L2RasterProduct product);

  @Query("""
      SELECT p FROM L2RasterProduct p WHERE
      p.cycle = :cycle and
      p.pass = :pass and
      p.scene = :scene and
      p.outputGranuleExtentFlag = :outputGranuleExtentFlag and
      p.outputSamplingGridType = :outputSamplingGridType and
      p.rasterResolution = :rasterResolution and
      p.utmZoneAdjust = :utmZoneAdjust and
      p.mgrsBandAdjust = :mgrsBandAdjust
  """)
  Optional<L2RasterProduct> findOneByParameters(
      int cycle,
      int pass,
      int scene,
      boolean outputGranuleExtentFlag,
      GridType outputSamplingGridType,
      int rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust
  );
}
