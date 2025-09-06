package org.trading.insfrastructure.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.trading.insfrastructure.entities.UserWallet;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

  Optional<UserWallet> findUserWalletByUsernameAndCurrency(String username, String currency);

  List<UserWallet> findUserWalletByUsername(String username);

  boolean existsUserWalletByUsername(String username);
}
