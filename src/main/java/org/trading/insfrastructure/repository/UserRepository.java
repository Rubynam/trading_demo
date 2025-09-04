package org.trading.insfrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.trading.insfrastructure.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
