package gov.nasa.podaac.swodlr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.rasterdefinition.RasterDefinition;
import gov.nasa.podaac.swodlr.rasterdefinition.RasterDefinitionRepository;
import gov.nasa.podaac.swodlr.user.User;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({"file:./src/main/resources/application.properties", "classpath:application.properties"})
@AutoConfigureHttpGraphQlTester
public class RasterDefinitionTests {
  @Autowired
  private HttpGraphQlTester graphQlTester;

  @Autowired
  private User mockUser;

  @Autowired
  private RasterDefinitionRepository rasterDefinitionRepository;

  @AfterEach
  public void clearDefinitions() {
    rasterDefinitionRepository.deleteAll();
  }

  @Test
  public void deleteDefinition() {
    // Generate definition
    RasterDefinition definition = new RasterDefinition(
      mockUser,
      "Test Definition",
      false,
      GridType.GEO,
      8,
      null,
      null
    );
    rasterDefinitionRepository.save(definition);

    // Delete definition
    graphQlTester
      .documentName("mutation/deleteRasterDefinition")
      .variable("id", definition.getId())
      .executeAndVerify();

    // Verify removal in database
    var result = rasterDefinitionRepository.findById(definition.getId());
    assertTrue(result.isEmpty());
  }

  @Test
  public void createDefinition() {
    final String name = "Test Definition";
    final boolean outputGranuleExtentFlag = false;
    final GridType outputSamplingGridType = GridType.UTM;
    final int rasterResolution = 100;
    final int utmZoneAdjust = -1;
    final int mgrsBandAdjust = 1;

    var response = graphQlTester
      .documentName("mutation/createRasterDefinition")
      .variable("name", name)
      .variable("outputGranuleExtentFlag", outputGranuleExtentFlag)
      .variable("outputSamplingGridType", outputSamplingGridType)
      .variable("rasterResolution", rasterResolution)
      .variable("utmZoneAdjust", utmZoneAdjust)
      .variable("mgrsBandAdjust", mgrsBandAdjust)
      .execute();

    List<RasterDefinition> definitions = rasterDefinitionRepository.findAll();
    assertEquals(1, definitions.size());

    RasterDefinition definition = definitions.get(0);
    assertEquals(definition.getId(), response.path("createRasterDefinition.id").entity(UUID.class).get());
    assertEquals(definition.getOutputGranuleExtentFlag(), outputGranuleExtentFlag);
    assertEquals(definition.getOutputSamplingGridType().toString(), outputSamplingGridType.toString());
    assertEquals(definition.getRasterResolution(), rasterResolution);
    assertEquals(definition.getUtmZoneAdjust(), utmZoneAdjust);
    assertEquals(definition.getMgrsBandAdjust(), mgrsBandAdjust);
  }

  @Test
  public void queryRasterDefinitionsWithoutArgs() {
    var utmDefinition = new RasterDefinition(
      mockUser,
      "utm-definition",
      true,
      GridType.UTM,
      10000,
      -1,
      1
    );
    var geoDefinition = new RasterDefinition(
      mockUser,
      "geo-definition",
      false,
      GridType.GEO,
      3,
      null,
      null
    );

    rasterDefinitionRepository.save(utmDefinition);
    rasterDefinitionRepository.save(geoDefinition);

    Set<UUID> validUuids = new HashSet<>();
    validUuids.add(utmDefinition.getId());
    validUuids.add(geoDefinition.getId());

    graphQlTester
        .documentName("query/currentUser_rasterDefinitions")
        .execute()
        .path("currentUser.rasterDefinitions[*].id")
        .entityList(UUID.class)
        .satisfies(uuidList -> {
          for (UUID uuid : uuidList) {
            assertTrue(validUuids.contains(uuid));
          }
        });
  }

  @Test
  public void queryRasterDefinitionsWithArgs() {
    final int numDefinitions = 20;
    final Random random = new Random();
    final GridType[] gridTypes = GridType.values();
    final int[] validGeoResolutions = {3, 4, 5, 6, 8, 15, 30, 60, 180, 300};
    final int[] validUtmResolutions = {100, 125, 200, 250, 500, 1000, 2500, 5000, 10000};

    final Map<UUID, RasterDefinition> definitions = new HashMap<>();

    final String[] parameters = {"id", "outputGranuleExtentFlag", "outputSamplingGridType", "rasterResolution", "utmZoneAdjust", "mgrsBandAdjust"};

    for (int i = 0; i < numDefinitions; i++) {
      GridType gridType = gridTypes[random.nextInt(gridTypes.length)];
      Integer utmZoneAdjust = null;
      Integer mgrsBandAdjust = null;
      Integer rasterResolution;
      if (gridType == GridType.UTM) {
        rasterResolution = validUtmResolutions[random.nextInt(validUtmResolutions.length)];
        utmZoneAdjust = random.nextInt(-1, 1 + 1);
        mgrsBandAdjust = random.nextInt(-1, 1 + 1);
      } else {
        rasterResolution = validGeoResolutions[random.nextInt(validGeoResolutions.length)];
      }

      RasterDefinition definition = new RasterDefinition(
        mockUser,
        UUID.randomUUID().toString(),
        random.nextBoolean(),
        gridType,
        rasterResolution,
        utmZoneAdjust,
        mgrsBandAdjust
      );

      definitions.put(definition.getId(), definition);
      rasterDefinitionRepository.save(definition);
    }

    /* Single parameter tests */
    for (RasterDefinition definition : definitions.values()) {
      for (String paramName : parameters) {
        var paramVal = getDefinitionField(paramName, definition);
        if (paramVal == null) {
          // Skip when value is null b/c it doesn't filter
          continue;
        }

        graphQlTester
            .documentName("query/currentUser_rasterDefinitions")
            .variable(paramName, getDefinitionField(paramName, definition))
            .execute()
            .path("currentUser.rasterDefinitions[*].id")
            .entityList(UUID.class)
            .satisfies(uuidList -> {
              assertTrue(uuidList.contains(definition.getId()));

              for (UUID uuid : uuidList) {
                var testVal = getDefinitionField(paramName, definitions.get(uuid));
                assertEquals(paramVal, testVal, "%s: %s != %s".formatted(paramName, paramVal, testVal));
              }
            });
      }
    }

    /* Multiple parameter tests */
    var testExtentVals = new Boolean[] {true, false};
    var testGridSamplingTypes = GridType.values();
    Set<UUID> seen = new HashSet<>();

    for (boolean extentVal : testExtentVals) {
      for (GridType gridType : testGridSamplingTypes) {
        graphQlTester
            .documentName("query/currentUser_rasterDefinitions")
            .variable("outputGranuleExtentFlag", extentVal)
            .variable("outputSamplingGridType", gridType)
            .execute()
            .path("currentUser.rasterDefinitions[*].id")
            .entityList(UUID.class)
            .satisfies(uuidList -> {
              for (UUID uuid : uuidList) {
                var testExtentFlag = definitions.get(uuid).getOutputGranuleExtentFlag();
                var testGridType = definitions.get(uuid).getOutputSamplingGridType();

                assertEquals(extentVal, testExtentFlag,
                    "outputGranuleExtentFlag: %s != %s".formatted(extentVal, testExtentFlag));
                assertEquals(gridType, testGridType,
                    "outputSamplingGridType: %s != %s".formatted(gridType, testGridType));

                seen.add(uuid);
              }
            });
      }
    }

    assertEquals(definitions.size(), seen.size());
  }

  private Object getDefinitionField(String name, RasterDefinition definition) {
    switch (name) {
      case "id":
        return definition.getId();
      case "outputGranuleExtentFlag":
        return definition.getOutputGranuleExtentFlag();
      case "outputSamplingGridType":
        return definition.getOutputSamplingGridType();
      case "rasterResolution":
        return definition.getRasterResolution();
      case "utmZoneAdjust":
        return definition.getUtmZoneAdjust();
      case "mgrsBandAdjust":
        return definition.getMgrsBandAdjust();
      default:
        // We shouldn't end up here
        assert false;
        return null;
    }
  }
}
