package gov.nasa.podaac.swodlr.l2rasterproduct;

import gov.nasa.podaac.swodlr.user.User;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.type.IntegerType;
import org.hibernate.type.UUIDCharType;

public class L2RasterProductQueryImpl implements L2RasterProductQuery {
  @PersistenceContext
  private EntityManager entityManager;

  /*
   * This implementation is utilized to workaround an JPA issue with the
   * PostgreSQL dialect in Hibernate where Hibernate does not parameterize
   * null values as their original datatype and instead parameterizes as
   * bytea's. This is a known issue of the PostgreSQL driver/dialect
   * 
   * This can lead to the exceptions:
   *    - "ERROR: could not determine data type of parameter $1"
   *    - "ERROR: operator does not exist: uuid = bytea"
   * 
   * Solutions researched include casting values, however, by casting
   * in the query, we loose the benefits of parameterization in our
   * prepared statements and create a statement which is harder to read.
   * Workarounds such as COALESCE and testing other PostgreSQL dialects
   * available in Hibernate were tried, but were found to either not
   * solve the issue or still require the use of CASTs in queries
   * 
   * The hope is to one day remove this code in favor of Spring Data JPA
   * queries, pending the PostgreSQL/Hibernate teams' cooperation with
   * one another
   * 
   * Relevant discussions:
   *    - https://stackoverflow.com/a/64223435
   *    - https://stackoverflow.com/a/62680643
   *    - https://github.com/pgjdbc/pgjdbc/issues/247#issuecomment-78213991
   */
  @Override
  public List<L2RasterProduct> findByUser(User user, UUID after, int limit) {
    @SuppressWarnings("LineLength")
    String statement =
        """
        SELECT \"L2RasterProducts\".* FROM \"L2RasterProducts\"
        JOIN \"ProductHistory\" ON \"ProductHistory\".\"rasterProductId\" = \"L2RasterProducts\".id
        WHERE
          (\"ProductHistory\".\"requestedById\" = CAST(:userId as UUID)) AND
          (
            (:after is NULL)
            OR
            (\"ProductHistory\".timestamp, \"ProductHistory\".\"rasterProductId\") < (SELECT timestamp, \"rasterProductId\" FROM \"ProductHistory\" WHERE \"requestedById\" = CAST(:userId as UUID) AND \"rasterProductId\" = CAST(:after as UUID))
          )
          ORDER BY \"ProductHistory\".timestamp DESC, \"ProductHistory\".\"rasterProductId\" DESC LIMIT :limit
        """;

    Session session = entityManager.unwrap(Session.class);
    Query<L2RasterProduct> query = session.createNativeQuery(statement, L2RasterProduct.class);
    query.setParameter("userId", user.getId(), UUIDCharType.INSTANCE);
    query.setParameter("after", after, UUIDCharType.INSTANCE);
    query.setParameter("limit", limit, IntegerType.INSTANCE);

    return query.getResultList();
  }
}
