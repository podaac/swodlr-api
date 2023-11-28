package gov.nasa.podaac.swodlr.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {
  ValidateL2RasterProductOptions.class,
  ValidateRasterDefinitionOptions.class
})
@Documented
public @interface ValidRasterOptions {
  String message() default "Raster options invalid";

  Class<?>[] groups() default { };

  Class<? extends Payload>[] payload() default { };
}
