package gov.nasa.podaac.swodlr.validation;

import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public abstract class AbstractRasterOptionValidator<T> implements
    ConstraintValidator<ValidRasterOptions, T> {
  static final Set<Integer> VALID_GEO_RESOLUTIONS = Set.of(
      3, 4, 5, 6, 8, 15, 30, 60, 180, 300
  );

  static final Set<Integer> VALID_UTM_RESOLUTIONS = Set.of(
      100, 125, 200, 250, 500, 1000, 2500, 5000, 10000
  );

  static final Set<Integer> VALID_ADJUSTS = Set.of(-1, 0, 1);
  
  public abstract boolean isValid(T value, ConstraintValidatorContext context);

  boolean validateCps(int cycle, int pass, int scene, ConstraintValidatorContext context) {
    Map<String, Integer> parameters = Map.ofEntries(
        Map.entry("cycle", cycle),
        Map.entry("pass", pass),
        Map.entry("scene", scene)
    );
    
    boolean valid = true;
    var it = parameters.entrySet().iterator();
  
    while (it.hasNext()) {
      var entry = it.next();
      String paramName = entry.getKey();
      int value = entry.getValue();

      if (value < 0 || value > 999) {
        valid = false;

        context
            .buildConstraintViolationWithTemplate("must be >= 0 and < 1000")
            .addPropertyNode(paramName)
            .addConstraintViolation();
      }
    }

    return valid;
  }

  boolean validateParameters(
      GridType gridType,
      int rasterResolution,
      Integer utmZoneAdjust,
      Integer mgrsBandAdjust,
      ConstraintValidatorContext context
  ) {
    boolean valid = true;

    if (gridType == GridType.UTM) {
      if (!VALID_UTM_RESOLUTIONS.contains(rasterResolution)) {
        valid = false;
        context
            .buildConstraintViolationWithTemplate(
              "must be a valid resolution according to the grid type")
            .addPropertyNode("rasterResolution")
            .addConstraintViolation();
      }

      if (utmZoneAdjust == null || !VALID_ADJUSTS.contains(utmZoneAdjust)) {
        valid = false;
        context
            .buildConstraintViolationWithTemplate("must be one of: -1, 0, -1")
            .addPropertyNode("utmZoneAdjust")
            .addConstraintViolation();
      }

      if (mgrsBandAdjust == null || !VALID_ADJUSTS.contains(mgrsBandAdjust)) {
        valid = false;
        context
            .buildConstraintViolationWithTemplate("must be one of: -1, 0, -1")
            .addPropertyNode("mgrsBandAdjust")
            .addConstraintViolation();
      }
    } else if (gridType == GridType.GEO) {
      if (!VALID_GEO_RESOLUTIONS.contains(rasterResolution)) {
        valid = false;
        context
            .buildConstraintViolationWithTemplate(
              "must be a valid resolution according to the grid type")
            .addPropertyNode("rasterResolution")
            .addConstraintViolation();
      }

      if (utmZoneAdjust != null) {
        valid = false;
        context
            .buildConstraintViolationWithTemplate("not applicable to GEO rasters")
            .addPropertyNode("utmZoneAdjust")
            .addConstraintViolation();
      }

      if (mgrsBandAdjust != null) {
        valid = false;
        context
            .buildConstraintViolationWithTemplate("not applicable to GEO rasters")
            .addPropertyNode("mgrsBandAdjust")
            .addConstraintViolation();
      }
    } else {
      context
          .buildConstraintViolationWithTemplate("must be one of: UTM or GEO")
          .addPropertyNode("gridType")
          .addConstraintViolation();

      valid = false;
    }

    return valid;
  }
}
