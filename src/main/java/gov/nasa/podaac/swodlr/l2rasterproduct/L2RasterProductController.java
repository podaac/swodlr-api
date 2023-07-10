package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.status.Status;
import gov.nasa.podaac.swodlr.user.User;
import gov.nasa.podaac.swodlr.user.UserReference;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class L2RasterProductController {
  @Autowired
  L2RasterProductService l2RasterProductService;

  @Autowired
  L2RasterProductRepository l2RasterProductRepository;

  @MutationMapping
  public Mono<L2RasterProduct> generateL2RasterProduct(
      @ContextValue UserReference userRef,
      @Argument int cycle,
      @Argument int scene,
      @Argument int pass,
      @Argument boolean outputGranuleExtentFlag,
      @Argument @NotNull GridType outputSamplingGridType,
      @Argument int rasterResolution,
      @Argument Integer utmZoneAdjust,
      @Argument Integer mgrsBandAdjust
  ) {
    return Mono.defer(() -> {
      User user = userRef.fetch();

      return l2RasterProductService.getL2RasterProduct(
        user,
        cycle,
        scene,
        pass,
        outputGranuleExtentFlag,
        outputSamplingGridType,
        rasterResolution,
        utmZoneAdjust,
        mgrsBandAdjust
      )
        .switchIfEmpty(Mono.defer(() -> l2RasterProductService.createL2RasterProduct(
          user,
          cycle,
          pass,
          scene,
          outputGranuleExtentFlag,
          outputSamplingGridType,
          rasterResolution,
          utmZoneAdjust,
          mgrsBandAdjust
        )));
    });
  }

  @QueryMapping
  public L2RasterProduct l2RasterProduct(@Argument UUID id) {
    var result = l2RasterProductRepository.findById(id);
    if (result.isPresent()) {
      return result.get();
    }

    return null;
  }

  @SchemaMapping(typeName = "Status", field = "product")
  public L2RasterProduct getProductForStatus(Status status) {
    return status.getProduct();
  }

  @SchemaMapping(typeName = "User", field = "products")
  public List<L2RasterProduct> getProductsForUser(
      @ContextValue UserReference userRef,
      @Argument UUID after,
      @Argument int limit
  ) {
    return l2RasterProductRepository.findByUser(userRef.fetch(), after, limit);
  }
}
