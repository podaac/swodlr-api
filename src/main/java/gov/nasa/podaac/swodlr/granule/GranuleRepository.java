package gov.nasa.podaac.swodlr.granule;

import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GranuleRepository extends JpaRepository<Granule, UUID> {
  Set<Granule> findByProduct(L2RasterProduct product);
}
