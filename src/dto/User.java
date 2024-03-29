package dto;

import java.math.BigDecimal;

public class User {
    public static final String USER_NOT_FROZEN = "0";
    private String userId;
    private String username;
    private BigDecimal balance;
    private String country;
    private String frozen;
    private BigDecimal depositMin;
    private BigDecimal depositMax;
    private BigDecimal withdrawMin;
    private BigDecimal withdrawMax;
    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getDepositMin() {
        return depositMin;
    }

    public void setDepositMin(BigDecimal depositMin) {
        this.depositMin = depositMin;
    }

    public BigDecimal getDepositMax() {
        return depositMax;
    }

    public void setDepositMax(BigDecimal depositMax) {
        this.depositMax = depositMax;
    }

    public BigDecimal getWithdrawMin() {
        return withdrawMin;
    }

    public void setWithdrawMin(BigDecimal withdrawMin) {
        this.withdrawMin = withdrawMin;
    }

    public BigDecimal getWithdrawMax() {
        return withdrawMax;
    }

    public void setWithdrawMax(BigDecimal withdrawMax) {
        this.withdrawMax = withdrawMax;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getFrozen() {
        return frozen;
    }

    public void setFrozen(String frozen) {
        this.frozen = frozen;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", balance=" + balance +
                ", country='" + country + '\'' +
                ", frozen='" + frozen + '\'' +
                ", depositMin=" + depositMin +
                ", depositMax=" + depositMax +
                ", withdrawMin=" + withdrawMin +
                ", withdrawMax=" + withdrawMax +
                '}';
    }
}
