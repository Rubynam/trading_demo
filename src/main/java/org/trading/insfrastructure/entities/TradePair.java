package org.trading.insfrastructure.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CRYPTO_PAIR")
@Getter
@Setter
public class TradePair {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "base_currency", nullable = false)
  private String baseCurrency; // e.g., BTC

  @Column(name = "quote_currency", nullable = false)
  private String quoteCurrency; // e.g., USDT


}
