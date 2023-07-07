package gov.nasa.podaac.swodlr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProductRepository;
import gov.nasa.podaac.swodlr.queue.ProductCreateQueue;
import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.status.State;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.test.tester.GraphQlTester.Response;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({"file:./src/main/resources/application.properties", "classpath:application.properties"})
@AutoConfigureHttpGraphQlTester
public class L2RasterProductTests {
  @Autowired
  private HttpGraphQlTester graphQlTester;

  @Autowired
  private L2RasterProductRepository l2RasterProductRepository;

  @MockBean
  private ProductCreateQueue productCreateQueue;

  @BeforeEach 
  public void initMock() {
    when(productCreateQueue.queueProduct(any(L2RasterProduct.class)))
      .thenReturn(Mono.empty());
  }

  @AfterEach
  public void deleteProducts() {
    l2RasterProductRepository.deleteAll();
  }

  @Test
  public void createL2RasterProductWithValidDefinition() {
    LocalDateTime start = LocalDateTime.now();

    int cycle = 1;
    int pass = 2;
    int scene = 3;
    boolean outputGranuleExtentFlag = true;
    GridType gridType = GridType.UTM;
    int rasterResolution = 1000;
    int utmZoneAdjust = 1;
    int mgrsBandAdjust = -1;

    Response response = graphQlTester
        .documentName("mutation/generateL2RasterProduct")
        .variable("cycle", cycle)
        .variable("pass", pass)
        .variable("scene", scene)
        .variable("outputGranuleExtentFlag", outputGranuleExtentFlag)
        .variable("outputSamplingGridType", gridType)
        .variable("rasterResolution", rasterResolution)
        .variable("utmZoneAdjust", utmZoneAdjust)
        .variable("mgrsBandAdjust", mgrsBandAdjust)
        .execute();

    /* -- Product -- */
    // Check product options
    response
        .path("generateL2RasterProduct")
        .entity(new ParameterizedTypeReference<Map<String, Object>>() {})
        .satisfies(product -> {
          assertEquals(product.get("cycle"), cycle);
          assertEquals(product.get("pass"), pass);
          assertEquals(product.get("scene"), scene);
          assertEquals(product.get("outputGranuleExtentFlag"), outputGranuleExtentFlag);
          assertEquals(product.get("outputSamplingGridType"), gridType.toString());
          assertEquals(product.get("rasterResolution"), rasterResolution);
          assertEquals(product.get("utmZoneAdjust"), utmZoneAdjust);
          assertEquals(product.get("mgrsBandAdjust"), mgrsBandAdjust);
        });
      
    // Verify queue invoked on new product
    verify(productCreateQueue).queueProduct(any());

    /* -- Status -- */
    // Timestamp
    response
        .path("generateL2RasterProduct.status[*].timestamp")
        .entityList(LocalDateTime.class)
        .hasSize(1)
        .satisfies(timestamps -> {
          var timestamp = timestamps.get(0);
          assertTrue(timestamp.compareTo(start) >= 0, "timestamp: %s, start: %s".formatted(timestamp, start));
        });

    // State
    response
        .path("generateL2RasterProduct.status[*].state")
        .entityList(String.class)
        .hasSize(1)
        .containsExactly(State.NEW.toString());
    
    // Reason
    response
        .path("generateL2RasterProduct.status[*].reason")
        .entityList(Object.class)
        .containsExactly(new Object[] {null});
  }

  @Test
  public void createL2RasterProductWithInvalidCps() {
    Set<String> invalidParams = new HashSet<>(Set.of(
        "cycle", "pass", "scene"
    ));

    graphQlTester
        .documentName("mutation/generateL2RasterProduct")
        .variable("cycle", -1)
        .variable("pass", -1)
        .variable("scene", -1)
        .variable("outputGranuleExtentFlag", false)
        .variable("outputSamplingGridType", GridType.GEO)
        .variable("rasterResolution", 8)
        .execute()
        .errors()
        .satisfy(errors -> {
          System.out.println(errors);
          assertEquals(
              invalidParams.size(),
              errors.size(),
              "%d != %d".formatted(invalidParams.size(), errors.size())
          );

          for (var error : errors) {
            assertEquals("generateL2RasterProduct", error.getPath());
            assertEquals("ValidationError", error.getExtensions().get("classification"));
            assertEquals("must be >= 0 and < 1000", error.getMessage());

            var prop = error.getExtensions().get("property");
            assertTrue(invalidParams.contains(prop));
            invalidParams.remove(prop);
          }

          assertEquals(0, invalidParams.size());
        });
  }

  @Test
  public void generateL2RasterProductWithInvalidParameters() {
    graphQlTester
        .documentName("mutation/generateL2RasterProduct")
        .variable("cycle", 0)
        .variable("pass", 0)
        .variable("scene", 0)
        .variable("outputGranuleExtentFlag", false)
        .variable("outputSamplingGridType", "UTM")
        .variable("rasterResolution", -1)
        .variable("mgrsBandAdjust", -2)
        .variable("utmZoneAdjust", 2)
        .execute()
        .errors()
        .satisfy(errors -> {
          final Set<String> expectedProperties = new HashSet<>();
          Collections.addAll(
            expectedProperties,
            "rasterResolution",
            "mgrsBandAdjust",
            "utmZoneAdjust"
          );

          assertEquals(expectedProperties.size(), errors.size());
          for (var error : errors) {
            assertEquals("generateL2RasterProduct", error.getPath());
            assertEquals("ValidationError", error.getExtensions().get("classification"));
            assertTrue(expectedProperties.remove(error.getExtensions().get("property")));
          }
        });
  }

  @Test
  public void queryCurrentUsersProducts() {
    final int pages = 2;
    final int pageLimit = 5;

    LocalDateTime start = LocalDateTime.now();

    final GridType gridType = GridType.GEO;
    final int rasterResolution = 8;

    for (int i = 0; i < pageLimit * pages; i++) {
      graphQlTester
          .documentName("mutation/generateL2RasterProduct")
          .variable("cycle", i)
          .variable("pass", i)
          .variable("scene", i)
          .variable("outputGranuleExtentFlag", false)
          .variable("outputSamplingGridType", gridType)
          .variable("rasterResolution", rasterResolution)
          .executeAndVerify();
    }

    Set<UUID> previouslySeen = new HashSet<>();
    LocalDateTime previousTimestamp = null;
    UUID afterId = null;

    // Iterate through pages
    for (int i = 0; i < pages; i++) {
      final int j = (pageLimit * (pages - i));

      Response response = graphQlTester
          .documentName("query/currentUser_products")
          .variable("after", afterId)
          .variable("limit", pageLimit)
          .execute();

      /* -- IDs -- */
      afterId = response
          .path("currentUser.products[*].id")
          .entityList(UUID.class)
          .hasSize(pageLimit)
          .satisfies(ids -> {
            for (UUID id : ids) {
              assertFalse(previouslySeen.contains(id), "Item has duplicated in pagination: %s".formatted(id));
              previouslySeen.add(id);
            }
          })
          .get()
          .get(pageLimit - 1);
  
      /* -- cycle -- */
      response
          .path("currentUser.products[*].cycle")
          .entityList(Integer.class)
          .satisfies(ids -> {
            int k = j;
            for (int id : ids) {
              assertEquals(--k, id);
            }
          });

      /* -- pass -- */
      response
          .path("currentUser.products[*].pass")
          .entityList(Integer.class)
          .satisfies(ids -> {
            int k = j;
            for (int id : ids) {
              assertEquals(--k, id);
            }
          });

      /* -- scene -- */
      response
          .path("currentUser.products[*].scene")
          .entityList(Integer.class)
          .satisfies(ids -> {
            int k = j;
            for (int id : ids) {
              assertEquals(--k, id);
            }
          });

      /* -- outputGranuleExtentFlag -- */
      response
          .path("currentUser.products[*].outputGranuleExtentFlag")
          .entityList(Boolean.class)
          .satisfies(flags -> {
            for (var flag : flags) {
              assertEquals(flag, false);
            }
          });

      /* -- outputSamplingGridType -- */
      response
          .path("currentUser.products[*].outputSamplingGridType")
          .entityList(String.class)
          .satisfies(gridTypes -> {
            for (var testGridType : gridTypes) {
              assertEquals(testGridType, gridType.toString());
            }
          });

      /* -- rasterResolution -- */
      response
          .path("currentUser.products[*].rasterResolution")
          .entityList(Integer.class)
          .satisfies(rasterResolutions -> {
            for (var testRasterResolution : rasterResolutions) {
              assertEquals(testRasterResolution, rasterResolution);
            }
          });

      /* -- Statuses -- */
      // State
      response
          .path("currentUser.products[*].status[*].state")
          .entityList(String.class)
          .hasSize(pageLimit)
          .satisfies(states -> states.forEach(state -> assertEquals(State.NEW.toString(), state)));

      // Reason
      response
          .path("currentUser.products[*].status[*].reason")
          .entityList(Object.class)
          .hasSize(pageLimit)
          .satisfies(reasons -> reasons.forEach(reason -> assertEquals(null, reason)));

      // Timestamp
      List<LocalDateTime> timestamps = response
          .path("currentUser.products[*].status[*].timestamp")
          .entityList(LocalDateTime.class)
          .hasSize(pageLimit)
          .get();
      
      for (LocalDateTime timestamp : timestamps) {
        if (previousTimestamp != null) {
          assertTrue(previousTimestamp.compareTo(timestamp) > 0, "previousTimestamp: %s, timestamp: %s".formatted(previousTimestamp, timestamp));
        }

        previousTimestamp = timestamp;
        assertTrue(timestamp.compareTo(start) > 0, "timestamp: %s, start: %s".formatted(timestamp, start));
      }
    }
  }
}
