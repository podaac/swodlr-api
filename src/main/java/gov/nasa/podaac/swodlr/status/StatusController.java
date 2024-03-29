package gov.nasa.podaac.swodlr.status;

import gov.nasa.podaac.swodlr.exception.SwodlrException;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProductRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class StatusController {
  @Autowired
  StatusRepository statusRepository;

  @Autowired
  L2RasterProductRepository l2RasterProductRepository;

  @QueryMapping
  List<Status> statusByProduct(@Argument UUID product, @Argument int limit) {
    Optional<L2RasterProduct> result = l2RasterProductRepository.findById(product);
    if (!result.isPresent()) {
      throw new SwodlrException("Invalid `product` parameter");
    }

    return statusRepository.findByProductId(result.get(), null, limit);
  }

  @QueryMapping
  List<Status> statusByPrevious(@Argument UUID after, @Argument int limit) {
    Optional<Status> previous = statusRepository.findById(after);
    if (!previous.isPresent()) {
      throw new SwodlrException("Invalid `after` parameter");
    }

    return statusRepository.findByProductId(previous.get().getProduct(), after, limit);
  }

  @SchemaMapping(typeName = "L2RasterProduct", field = "status")
  List<Status> getStatusForL2RasterProduct(
      L2RasterProduct product, @Argument UUID after, @Argument int limit
  ) {
    return statusRepository.findByProductId(product, after, limit);
  }
}
