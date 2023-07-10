package gov.nasa.podaac.swodlr.validation;

import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import javax.validation.ConstraintValidatorContext;

public class ValidateL2RasterProductOptions extends AbstractRasterOptionValidator<L2RasterProduct> {
  @Override
  public boolean isValid(L2RasterProduct product, ConstraintValidatorContext context) {  
    context.disableDefaultConstraintViolation();
    
    boolean valid = validateCps(
        product.getCycle(),
        product.getPass(),
        product.getScene(),
        context
    );

    if (!validateParameters(
        product.getOutputSamplingGridType(),
        product.getRasterResolution(),
        product.getUtmZoneAdjust(),
        product.getMgrsBandAdjust(),
        context
    )) {
      valid = false;
    }

    return valid;
  }
}
