package gov.nasa.podaac.swodlr.validation;

import gov.nasa.podaac.swodlr.rasterdefinition.RasterDefinition;
import javax.validation.ConstraintValidatorContext;

public class ValidateRasterDefinitionOptions extends AbstractRasterOptionValidator<RasterDefinition> {
  @Override
  public boolean isValid(RasterDefinition definition, ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();

    return validateParameters(
      definition.getOutputSamplingGridType(),
      definition.getRasterResolution(),
      definition.getUtmZoneAdjust(),
      definition.getMgrsBandAdjust(),
      context
    );
  }
}
