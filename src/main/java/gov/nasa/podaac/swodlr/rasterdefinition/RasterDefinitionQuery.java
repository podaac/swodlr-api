package gov.nasa.podaac.swodlr.rasterdefinition;

import gov.nasa.podaac.swodlr.user.User;
import java.util.List;
import java.util.UUID;

public interface RasterDefinitionQuery {
  List<RasterDefinition> findByParameter(
      User user,
      UUID id,
      Boolean outputGranuleExtentFlag,
      GridType outputSamplingGridType,
      Integer rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust
  );
}
