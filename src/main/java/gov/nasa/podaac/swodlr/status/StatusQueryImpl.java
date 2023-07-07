package gov.nasa.podaac.swodlr.status;

import gov.nasa.podaac.swodlr.l2rasterproduct.L2RasterProduct;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.type.IntegerType;
import org.hibernate.type.UUIDCharType;

public class StatusQueryImpl implements StatusQuery {
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
  public List<Status> findByProductId(L2RasterProduct product, UUID after, int limit) {
    String statement = """
        SELECT * FROM \"Status\"
        WHERE 
          (\"productId\" = CAST(:productId AS UUID))
          AND
          (
            (:after is NULL)
            OR
            ((timestamp, id) < (SELECT timestamp, id FROM \"Status\" WHERE id = CAST(:after as UUID)))
          )
        ORDER BY timestamp DESC, id DESC LIMIT :limit
    """;

    Session session = entityManager.unwrap(Session.class);
    Query<Status> query = session.createNativeQuery(statement, Status.class);
    query.setParameter("productId", product.getId(), UUIDCharType.INSTANCE);
    query.setParameter("after", after, UUIDCharType.INSTANCE);
    query.setParameter("limit", limit, IntegerType.INSTANCE);

    return query.getResultList();
  }
}
