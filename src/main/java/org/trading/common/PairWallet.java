package org.trading.common;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.lang.NonNull;
import org.trading.insfrastructure.entities.UserWallet;

public class PairWallet extends ImmutablePair<UserWallet,UserWallet> {

  /**
   * Create a new pair instance.
   *
   * @param left  the left value
   * @param right the right value
   */
  public PairWallet(@NonNull UserWallet left, @NonNull UserWallet right) {
    super(left, right);
  }

  public UserWallet getBaseWallet() {
    return this.left;
  }

  public UserWallet getQuoteWallet() {
    return this.right;
  }
}
