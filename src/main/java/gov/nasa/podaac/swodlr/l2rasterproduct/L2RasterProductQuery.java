package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.user.User;
import java.util.List;
import java.util.UUID;

public interface L2RasterProductQuery {
  List<L2RasterProduct> findByUser(User user, UUID after, int limit);
}
