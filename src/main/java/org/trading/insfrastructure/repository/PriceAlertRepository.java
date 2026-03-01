package org.trading.insfrastructure.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.trading.insfrastructure.entities.PriceAlertEntity;

@Repository
public interface PriceAlertRepository extends CassandraRepository<PriceAlertEntity, String> {
}
