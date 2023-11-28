package gov.nasa.podaac.swodlr.rasterdefinition;

import gov.nasa.podaac.swodlr.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RasterDefinitionRepository extends JpaRepository<RasterDefinition, UUID>,
    RasterDefinitionQuery {
  Optional<RasterDefinition> findOneByUserAndId(User user, UUID id);
}
