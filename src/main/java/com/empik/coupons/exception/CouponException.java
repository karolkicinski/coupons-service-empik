package com.empik.coupons.exception;

import com.empik.coupons.enumerate.CouponRejectionReason;

public class CouponException extends RuntimeException {

  private final CouponRejectionReason reason;

  public CouponException(CouponRejectionReason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public CouponRejectionReason reason() {
    return reason;
  }
}
