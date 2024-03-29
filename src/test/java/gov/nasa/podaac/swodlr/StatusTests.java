package gov.nasa.podaac.swodlr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProductRepository;
import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import gov.nasa.podaac.swodlr.status.State;
import gov.nasa.podaac.swodlr.status.Status;
import gov.nasa.podaac.swodlr.status.StatusRepository;
import graphql.com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.graphql.test.tester.GraphQlTester.Response;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({"file:./src/main/resources/application.properties", "classpath:application.properties"})
@AutoConfigureHttpGraphQlTester
public class StatusTests {
  private static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Autowired
  private HttpGraphQlTester graphQlTester;

  @Autowired
  private StatusRepository statusRepository;

  @Autowired
  private L2RasterProductRepository l2RasterProductRepository;

  @Value("classpath:frost.txt")
  private Resource frost;

  @AfterAll
  public void tearDownMocks() {
    l2RasterProductRepository.deleteAll();
    statusRepository.deleteAll();
  }

  @Test
  public void queryStatus() throws IOException {
    final int pages = 2;
    final int pageLimit = 5;

    BufferedReader poetryReader = new BufferedReader(new InputStreamReader(frost.getInputStream()));
    List<String> reasons = poetryReader.lines().toList();

    List<State> stateEnums = Lists.newArrayList(State.values());
    int stateIndex = (stateEnums.indexOf(State.NEW) + 1) % stateEnums.size();
    int reasonIndex = 0;

    /* Setup mock data */
    final L2RasterProduct mockProduct = new L2RasterProduct(
        0,
        1,
        2,
        false,
        GridType.UTM,
        1000,
        0,
        0
    );
    l2RasterProductRepository.save(mockProduct);

    LocalDateTime start = LocalDateTime.now();

    for (int i = 0; i < pages * pageLimit; i++) {
      Status status = new Status(
          mockProduct,
          stateEnums.get(stateIndex),
          reasons.get(++reasonIndex)
      );
      statusRepository.save(status);

      stateIndex = (stateIndex + 1) % stateEnums.size();
    }

    /* Query statuses */
    Set<UUID> previouslySeen = new HashSet<>();
    LocalDateTime previousTimestamp = null;
    UUID afterId = null;

    for (int i = 0; i < pages; i++) {
      Response response = graphQlTester
          .documentName(i == 0 ? "query/statusByProduct" : "query/statusByPrevious")
          .variable("product", mockProduct.getId())
          .variable("after", afterId)
          .variable("limit", pageLimit)
          .execute();

      /* IDs */
      afterId = response
          .path("status[*].id")
          .entityList(UUID.class)
          .hasSize(pageLimit)
          .satisfies(ids -> {
            for (UUID id : ids) {
              assertFalse(previouslySeen.contains(id));
              previouslySeen.add(id);
            }
          })
          .get()
          .get(pageLimit - 1);

      /* States */
      List<String> testStates = response
          .path("status[*].state")
          .entityList(String.class)
          .get();

      for (String state : testStates) {
        stateIndex = Math.floorMod(stateIndex - 1, stateEnums.size());
        assertEquals(stateEnums.get(stateIndex).toString(), state);
      }

      /* Reasons */
      List<String> testReasons = response
          .path("status[*].reasons")
          .entityList(String.class)
          .get();

      for (String reason : testReasons) {
        if (reasonIndex == 0) {
          assertEquals(reason, null);
        } else {
          assertEquals(reason, stateEnums.get(reasonIndex--).toString());
        }
      }

      /* Timestamps */
      List<LocalDateTime> timestamps = response
          .path("status[*].timestamp")
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

      /* Product */
      response
          .path("status[*].product")
          .entityList(new ParameterizedTypeReference<Map<String, Object>>() {})
          .satisfies(products -> {
            Iterator<Map<String, Object>> it = products.iterator();
            while (it.hasNext()) {
              Map<String, Object> product = it.next();

              assertEquals(mockProduct.getId().toString(), product.get("id"));
              assertEquals(mockProduct.getCycle(), product.get("cycle"));
              assertEquals(mockProduct.getPass(), product.get("pass"));
              assertEquals(mockProduct.getScene(), product.get("scene"));
              assertEquals(mockProduct.getOutputGranuleExtentFlag(), product.get("outputGranuleExtentFlag"));
              assertEquals(mockProduct.getOutputSamplingGridType().toString(), product.get("outputSamplingGridType"));
              assertEquals(mockProduct.getRasterResolution(), product.get("rasterResolution"));
              assertEquals(mockProduct.getUtmZoneAdjust(), product.get("utmZoneAdjust"));
              assertEquals(mockProduct.getMgrsBandAdjust(), product.get("mgrsBandAdjust"));
            }
          });
    }
  }

  @Test
  public void queryStatusWithInvalidProduct() {
    graphQlTester
        .documentName("query/statusByProduct")
        .variable("product", NULL_UUID)
        .execute()
        .errors()
        .satisfy(errors -> {
          assertEquals(1, errors.size());

          var error = errors.get(0);
          assertEquals("status", error.getPath());
          assertEquals("DataFetchingException", error.getExtensions().get("classification"));
          assertEquals("Invalid `product` parameter", error.getMessage());
        });
  }

  @Test
  public void queryStatusWithInvalidAfter() {
    graphQlTester
        .documentName("query/statusByPrevious")
        .variable("after", NULL_UUID)
        .execute()
        .errors()
        .satisfy(errors -> {
          assertEquals(1, errors.size());

          var error = errors.get(0);
          assertEquals("status", error.getPath());
          assertEquals("DataFetchingException", error.getExtensions().get("classification"));
          assertEquals("Invalid `after` parameter", error.getMessage());
        });
  }
}
