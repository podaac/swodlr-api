package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.producthistory.ProductHistory;
import gov.nasa.podaac.swodlr.producthistory.ProductHistoryRepository;
import gov.nasa.podaac.swodlr.queue.ProductCreateQueue;
import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.status.State;
import gov.nasa.podaac.swodlr.status.Status;
import gov.nasa.podaac.swodlr.status.StatusRepository;
import gov.nasa.podaac.swodlr.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class L2RasterProductService {
  @Autowired
  private L2RasterProductRepository l2RasterProductRepository;

  @Autowired
  private StatusRepository statusRepository;

  @Autowired
  private ProductHistoryRepository productHistoryRepository;

  @Autowired
  private ProductCreateQueue productCreateQueue;

  @Transactional
  public Mono<L2RasterProduct> createL2RasterProduct(
      User user,
      int cycle,
      int pass,
      int scene,
      boolean outputGranuleExtentFlag,
      GridType outputSamplingGridType,
      int rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust
  ) {
    L2RasterProduct product = new L2RasterProduct(
        cycle,
        pass,
        scene,
        outputGranuleExtentFlag,
        outputSamplingGridType,
        rasterResolution,
        utmZoneAdjust,
        mgrsBandAdjust
    );
    ProductHistory history = new ProductHistory(user, product);
    product = l2RasterProductRepository.save(product);
    productHistoryRepository.save(history);

    return startProductGeneration(product);
  }

  @Transactional
  public Mono<L2RasterProduct> getL2RasterProduct(
      User requestor,
      int cycle,
      int pass,
      int scene,
      boolean outputGranuleExtentFlag,
      GridType outputSamplingGridType,
      int rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust
  ) {
    var productResult = l2RasterProductRepository.findOneByParameters(
        cycle,
        pass,
        scene,
        outputGranuleExtentFlag,
        outputSamplingGridType,
        rasterResolution,
        utmZoneAdjust,
        mgrsBandAdjust
    );

    if (productResult.isEmpty()) {
      return Mono.empty();
    }

    L2RasterProduct product = productResult.get();
    ProductHistory history = new ProductHistory(requestor, product);
    productHistoryRepository.save(history);

    State currentState = product.getStatuses().get(0).getState();

    if (currentState == State.UNAVAILABLE || currentState == State.ERROR) {
      return startProductGeneration(product);
    }

    return Mono.just(product);
  }

  @Transactional
  public Mono<L2RasterProduct> startProductGeneration(L2RasterProduct product) {
    return Mono.defer(() -> {
      Status status = new Status(product, State.NEW);
      statusRepository.save(status);
      return productCreateQueue.queueProduct(product).thenReturn(product);
    });
  }
}
