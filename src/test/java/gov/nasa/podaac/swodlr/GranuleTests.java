package gov.nasa.podaac.swodlr;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nasa.podaac.swodlr.granule.Granule;
import gov.nasa.podaac.swodlr.granule.GranuleRepository;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProductRepository;
import gov.nasa.podaac.swodlr.rasterdefinition.GridType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
public class GranuleTests {
  private L2RasterProduct mockProduct;

  @Autowired
  private GranuleRepository granuleRepository;

  @Autowired
  private L2RasterProductRepository productRepository;

  @Autowired
  private HttpGraphQlTester graphQlTester;

  @BeforeAll
  void initMocks() {
    mockProduct = new L2RasterProduct(
      0,
      0,
      0,
      false,
      GridType.UTM,
      1000,
      0,
      0
    );
    
    mockProduct = productRepository.save(mockProduct);
  }

  @AfterAll
  void tearDownMocks() {
    productRepository.deleteAll();
  }

  @AfterEach
  void deleteAllGranules() {
    granuleRepository.deleteAll();
  }

  @Test
  public void convertS3UriToTeaUri() {
    Granule granule = new Granule(mockProduct, "s3://test-bucket/path");
    granule = granuleRepository.save(granule);

    graphQlTester
      .documentName("query/l2RasterProduct_granules")
      .variable("id", mockProduct.getId())
      .execute()
      .path("l2RasterProduct.granules[0].uri")
      .entity(String.class)
      .satisfies(uri -> 
        assertTrue(uri.equals("https://earl-grey/test-bucket/path"))
      );
  }
}
