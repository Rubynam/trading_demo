package org.trading.insfrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.trading.insfrastructure.enumeration.TradeSide;

@Entity
@Table(name = "TRADE_TRANSACTION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String symbol;

  @Column(nullable = false)
  private TradeSide tradeType; // BUY or SELL

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal quantity;

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal price;

  @Column(nullable = false)
  private LocalDateTime timestamp = LocalDateTime.now();

}

