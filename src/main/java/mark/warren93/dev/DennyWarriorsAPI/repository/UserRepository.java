package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.Role;
import mark.warren93.dev.DennyWarriorsAPI.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUserName(String userName);

    long countByRole(Role role);
}
