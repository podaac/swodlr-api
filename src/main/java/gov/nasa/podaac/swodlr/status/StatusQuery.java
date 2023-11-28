package gov.nasa.podaac.swodlr.status;

import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import java.util.List;
import java.util.UUID;

public interface StatusQuery {
  List<Status> findByProductId(L2RasterProduct product, UUID after, int limit);
}
