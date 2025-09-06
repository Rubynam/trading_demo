package org.trading.insfrastructure.entities;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "USER_WALLET")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWallet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private String username;

  @Column(nullable = false)
  private String currency;

  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal balance;


}
