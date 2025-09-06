package org.trading.insfrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.trading.domain.enumeration.TradeSide;

@Entity
@Table(name = "TRADE_TRANSACTION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  //todo apply Join with UserWallet if necessary
  @Column(nullable = false)
  private String username;

  @Column(nullable = false)
  private String symbol;

  @Column(nullable = false)
  private TradeSide tradeType; // BUY or SELL

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal quantity;

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal price;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private LocalDateTime timestamp = LocalDateTime.now();

}

