package dto;

public class User {
    public String userId;
    public String username;
    public Float balance;
    public String country;
    public String frozen;
    public Float depositMin;
    public Float depositMax;
    public Float withdrawMin;
    public Float withdrawMax;

    private String cardAccountNumber = null;
    private String transferAccountNumber = null;

    public String getCardAccountNumber() {
        return cardAccountNumber;
    }

    public void setCardAccountNumber(String cardAccountNumber) {
        this.cardAccountNumber = cardAccountNumber;
    }

    public String getTransferAccountNumber() {
        return transferAccountNumber;
    }

    public void setTransferAccountNumber(String transferAccountNumber) {
        this.transferAccountNumber = transferAccountNumber;
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

    public Float getBalance() {
        return balance;
    }

    public void setBalance(Float balance) {
        this.balance = balance;
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

    public Float getDepositMin() {
        return depositMin;
    }

    public void setDepositMin(Float depositMin) {
        this.depositMin = depositMin;
    }

    public Float getDepositMax() {
        return depositMax;
    }

    public void setDepositMax(Float depositMax) {
        this.depositMax = depositMax;
    }

    public Float getWithdrawMin() {
        return withdrawMin;
    }

    public void setWithdrawMin(Float withdrawMin) {
        this.withdrawMin = withdrawMin;
    }

    public Float getWithdrawMax() {
        return withdrawMax;
    }

    public void setWithdrawMax(Float withdrawMax) {
        this.withdrawMax = withdrawMax;
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
