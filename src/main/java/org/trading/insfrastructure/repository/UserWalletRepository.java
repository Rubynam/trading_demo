package org.trading.insfrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.trading.insfrastructure.entities.UserWallet;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

}
