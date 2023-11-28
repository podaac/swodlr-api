package gov.nasa.podaac.swodlr.queue;

import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import reactor.core.publisher.Mono;

public interface ProductCreateQueue {
  public Mono<Void> queueProduct(L2RasterProduct product);
}
