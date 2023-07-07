package gov.nasa.podaac.swodlr.granule;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;

@Controller
public class GranuleController {
  @Autowired
  private GranuleRepository granuleRepository;

  @SchemaMapping(typeName = "L2RasterProduct", field = "granules")
  public Set<Granule> getGranulesForProduct(L2RasterProduct product) {
    return granuleRepository.findByProduct(product);
  }
}
