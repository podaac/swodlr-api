package gov.nasa.podaac.swodlr.queue;

import gov.nasa.podaac.swodlr.SwodlrProperties;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Component
public class SqsProductCreateQueue implements ProductCreateQueue {
  private final SqsTemplate productCreateQueue;

  public SqsProductCreateQueue(
      @Autowired SqsAsyncClient sqsAsyncClient,
      @Autowired SwodlrProperties swodlrProperties
  ) {
    productCreateQueue = SqsTemplate.builder()
        .sqsAsyncClient(sqsAsyncClient)
        .configure((options) -> {
          options.defaultQueue(swodlrProperties.productCreateQueueUrl());
        })
        .build();
  }
  
  @Override
  public Mono<Void> queueProduct(L2RasterProduct product) {
    return Mono.defer(() -> {
      ProductCreateMessage message = new ProductCreateMessage(product);
      return Mono.fromFuture(productCreateQueue.sendAsync(message)).then();
    });
  }
}
