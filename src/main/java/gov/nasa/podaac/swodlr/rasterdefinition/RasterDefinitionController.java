package gov.nasa.podaac.swodlr.rasterdefinition;

import gov.nasa.podaac.swodlr.exception.SwodlrException;
import gov.nasa.podaac.swodlr.user.User;
import gov.nasa.podaac.swodlr.user.UserReference;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class RasterDefinitionController {
  @Autowired
  RasterDefinitionRepository rasterDefinitionRepository;

  @SchemaMapping(typeName = "User", field = "rasterDefinitions")
  Mono<List<RasterDefinition>> getRasterDefinitionsForUser(
      @ContextValue UserReference userRef,
      @Argument UUID id,
      @Argument Boolean outputGranuleExtentFlag,
      @Argument GridType outputSamplingGridType,
      @Argument Integer rasterResolution,
      @Argument Integer utmZoneAdjust,
      @Argument Integer mgrsBandAdjust
  ) {
    return Mono.fromCallable(() -> {
      User user = userRef.fetch();
      return rasterDefinitionRepository.findByParameter(
        user, id, outputGranuleExtentFlag, outputSamplingGridType,
        rasterResolution, utmZoneAdjust, mgrsBandAdjust
      );
    });
  }

  @MutationMapping
  Mono<Boolean> deleteRasterDefinition(@ContextValue UserReference userRef, @Argument UUID id) {
    return Mono.fromCallable(() -> {
      User user = userRef.fetch();
      var result = rasterDefinitionRepository.findOneByUserAndId(user, id);
      if (result.isEmpty()) {
        throw new SwodlrException("Raster definition not found");
      }

      rasterDefinitionRepository.delete(result.get());
      return true;
    });
  }

  @MutationMapping
  Mono<RasterDefinition> createRasterDefinition(
      @ContextValue UserReference userRef,
      @Argument String name,
      @Argument boolean outputGranuleExtentFlag,
      @Argument GridType outputSamplingGridType,
      @Argument int rasterResolution,
      @Argument Integer utmZoneAdjust,
      @Argument Integer mgrsBandAdjust
  ) {
    return Mono.fromCallable(() -> {
      User user = userRef.fetch();
      RasterDefinition definition = new RasterDefinition(
        user,
        name,
        outputGranuleExtentFlag,
        outputSamplingGridType,
        rasterResolution,
        utmZoneAdjust,
        mgrsBandAdjust
      );

      definition = rasterDefinitionRepository.save(definition);
      return definition;
    });
  }
}
