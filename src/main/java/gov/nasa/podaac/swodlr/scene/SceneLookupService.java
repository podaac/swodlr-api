package gov.nasa.podaac.swodlr.scene;

import gov.nasa.podaac.swodlr.SwodlrProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

@Service
public class SceneLookupService {
  private final DynamoDbAsyncClient dynamoDbClient;
  private final SwodlrProperties swodlrProperties;

  public SceneLookupService(
      AwsCredentialsProvider credentialsProvider,
      AwsRegionProvider awsRegionProvider,
      SwodlrProperties swodlrProperties
  ) {
    dynamoDbClient = DynamoDbAsyncClient.builder()
      .credentialsProvider(credentialsProvider)
      .region(awsRegionProvider.getRegion())
      .build();

    this.swodlrProperties = swodlrProperties;
  }

  public Mono<Boolean> sceneExists(int cycle, int pass, int scene) {
    return Mono
      .fromFuture(() -> {
        List<String> tiles = generateTileList(cycle, pass, scene);
        List<Map<String, AttributeValue>> keys = new ArrayList<>(8);

        for (String tile : tiles) {
          keys.add(Map.of("tile_id", AttributeValue.fromS(tile)));
        }

        KeysAndAttributes keyAndAttrs = KeysAndAttributes.builder()
          .keys(keys)
          .build();

        return dynamoDbClient.batchGetItem((request) -> {
          request.requestItems(Map.of(
            swodlrProperties.availableTilesTableName(),
            keyAndAttrs
          ));
        });
      })
      .flatMap((batchResponse) -> {
        if (batchResponse.hasUnprocessedKeys()) {
          return Mono.just(false);
        }

        List<Map<String, AttributeValue>> responses = batchResponse
          .responses()
          .get(swodlrProperties.availableTilesTableName());

        for (var response : responses) {
          if (!response.containsKey("tile_id")) {
            return Mono.just(false);
          }
        }

        return Mono.just(true);
      });
  }

  private List<String> generateTileList(int cycle, int pass, int scene) {
    final char[] DIRECTIONS = {'L', 'R'};
    final String[] PRODUCTS = {"PIXC", "PIXCVec"};

    List<String> tiles = new ArrayList<>(8);

    for (String product : PRODUCTS) {
      for (int tile = (scene * 2) - 2; tile <= (scene * 2) + 1; tile++) {
        for (char direction : DIRECTIONS) {
          String cpsString = "%s,%d,%d,%d%c".formatted(product, cycle, pass, scene, direction);
          tiles.add(cpsString);
        }
      }
    }

    return tiles;
  }
}
