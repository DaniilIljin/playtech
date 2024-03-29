import dto.BinMapping;
import dto.Event;
import dto.Transaction;
import dto.User;
import util.Reader;
import util.Writer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;

public class TransactionProcessorSample {
    private final List<Event> events = new ArrayList<>();
    private final List<User> users;
    private final List<Transaction> transactions;
    private final List<BinMapping> binMappings;
    private User currentUser = null;
    private BinMapping currentBinMapping = null;
    private final StringBuilder stringBuilder = new StringBuilder();

    public TransactionProcessorSample(
            final List<User> users,
            final List<Transaction> transactions,
            final List<BinMapping> binMappings)
    {
        this.users = users;
        this.transactions = transactions;
        this.binMappings = binMappings;
    }

    public static void main(final String[] args) throws IOException {
        List<User> users = Reader.readUsers(Paths.get(args[0]));
        List<Transaction> transactions = Reader.readTransactions(Paths.get(args[1]));
        List<BinMapping> binMappings = Reader.readBinMappings(Paths.get(args[2]));

        TransactionProcessorSample transactionProcessor = new TransactionProcessorSample(users, transactions, binMappings);
        transactionProcessor.processTransactions();

        Writer.writeBalances(Paths.get(args[3]), users);
        Writer.writeEvents(Paths.get(args[4]), transactionProcessor.events);
    }

    private void processTransactions() {
        for (Transaction transaction : transactions) {
            Event event = validate(transaction);
            if(event != null) events.add(event);
        }
    }
    private Event validate(Transaction transaction){
        currentUser = null;
        currentBinMapping = null;
        return validateUniqueId(transaction);
    }
    private Event validateUniqueId(Transaction transaction) {
        boolean isProcessedEvent = events.stream().anyMatch(
                e -> e.transactionId.equals(transaction.transactionId));
        if (isProcessedEvent){
            return returnDeclinedEvent(transaction, String.format("Transaction %s already processed (id non-unique)", transaction.transactionId));
        }
        return validateUserExistAndNotFrozen(transaction);
    }

    private Event validateUserExistAndNotFrozen(Transaction transaction){
        if(getUser(transaction) == null){
            return returnDeclinedEvent(transaction, String.format("User %s not found in Users", transaction.userId));
        }
        return validateTransferPaymentMethod(transaction);
    }

    private Event validateTransferPaymentMethod(Transaction transaction) {
        if (transaction.method.equals(Transaction.PAYMENT_METHOD_TRANSFER)){
            stringBuilder.append(transaction.accountNumber);
            String firstPart = stringBuilder.substring(0, 4);
            String secondPart = stringBuilder.substring(4);
            stringBuilder.setLength(0);
            for (char c : secondPart.toCharArray()) {
                stringBuilder.append(convertToInt(c));
            }
            for (char c : firstPart.toCharArray()) {
                stringBuilder.append(convertToInt(c));
            }
            try {
                BigInteger checkNumber = new BigInteger(stringBuilder.toString());
                stringBuilder.setLength(0);
                if (!checkNumber.remainder(BigInteger.valueOf(97)).equals(BigInteger.ONE)){
                    return returnDeclinedEvent(transaction, String.format("Invalid iban %s", transaction.accountNumber));
                }
            } catch (Exception e) {
                return null;
            }
        }
        return validateAmountAndUserLimits(transaction);
    }
    private Event validateAmountAndUserLimits(Transaction transaction) {
        User user = getUser(transaction);
        if (user == null) return null;

        if (transaction.amount < 0){
            return returnDeclinedEvent(transaction, "Amount is negative for transaction");
        }
        if (transaction.type.equals(Transaction.TRANSACTION_TYPE_WITHDRAW)){
            if (user.withdrawMax < transaction.amount){
                return returnDeclinedEvent(transaction, String.format("Amount %s is over the withdraw limit of %s", formatAmount(transaction.amount), formatAmount(user.withdrawMax)));
            }
            if (user.withdrawMin > transaction.amount){
                return returnDeclinedEvent(transaction, String.format("Amount %s is under the withdraw limit of %s", formatAmount(transaction.amount), formatAmount(user.withdrawMin)));
            }
        }
        if (transaction.type.equals(Transaction.TRANSACTION_TYPE_DEPOSIT)){
            if (user.depositMax < transaction.amount){
                return returnDeclinedEvent(transaction, String.format("Amount %s is over the deposit limit of %s", formatAmount(transaction.amount), formatAmount(user.depositMax)));
            }
            if (user.depositMin > transaction.amount){
                return returnDeclinedEvent(transaction, String.format("Amount %s is under the deposit limit of %s", formatAmount(transaction.amount), formatAmount(user.depositMin)));
            }
        }
        return validateCountry(transaction);
    }

    private Event validateCountry(Transaction transaction) {
        User user = getUser(transaction);
        if (user == null) return null;
        if (transaction.method.equals(Transaction.PAYMENT_METHOD_CARD)){
            BinMapping bin = getBinMapping(transaction);
            if (bin == null) return null;
            if (!bin.country.equals(getISO3(user.country))){
                return returnDeclinedEvent(transaction, String.format("Invalid country %s; expected %s (%s)", bin.country, user.country, getISO3(user.country)));
            }
        }
        if (transaction.method.equals(Transaction.PAYMENT_METHOD_TRANSFER)){
            if (!user.country.equals(transaction.accountNumber.substring(0,2))){
                return returnDeclinedEvent(transaction, String.format("Invalid account country %s; expected %s", transaction.accountNumber.substring(0,2), currentUser.country ));
            }
        }
        return validateEnoughForWithdraw(transaction);
    }

    private Event validateEnoughForWithdraw(Transaction transaction) {
        if (transaction.type.equals(Transaction.TRANSACTION_TYPE_WITHDRAW)){
            Optional<User> userByAccount = users.stream().filter(u -> transaction.getAccountNumber().equals(u.getLastUsedDepositAccount())).findFirst();
            if (userByAccount.isPresent()){
                if (!userByAccount.get().getUserId().equals(transaction.userId))
                    return returnDeclinedEvent(transaction, String.format("Account %s is in use by other user", transaction.getAccountNumber()));
            };
            if (currentUser.balance < transaction.amount)
                return returnDeclinedEvent(transaction, String.format("Not enough balance to withdraw %s - balance is too low at %s", formatAmount(transaction.getAmount()), formatAmount(currentUser.getBalance())));
        }
        return validateWithdrawFromLastAccount(transaction);
    }
    private Event validateWithdrawFromLastAccount(Transaction transaction) {
        if (transaction.type.equals(Transaction.TRANSACTION_TYPE_WITHDRAW)){
            User user = getUser(transaction);
            if (user == null) return null;

            if (user.getLastUsedDepositAccount() == null || !user.getLastUsedDepositAccount().equals(transaction.accountNumber)){
                return returnDeclinedEvent(transaction, String.format("Cannot withdraw with a new account %s", transaction.getAccountNumber()));
            }
        }
        return validateTransactionType(transaction);
    }
    private Event validateTransactionType(Transaction transaction) {
        if (!transaction.getType().equals(Transaction.TRANSACTION_TYPE_DEPOSIT) && !transaction.getType().equals(Transaction.TRANSACTION_TYPE_WITHDRAW))
            return returnDeclinedEvent(transaction, "Wrong transaction method");
        return validateOneUserOneAccountOfEachType(transaction);
    }
    private Event validateOneUserOneAccountOfEachType(Transaction transaction) {
        User user = getUser(transaction);
        if (user == null) return null;

//        if ((user.getCardAccountNumber() != null && !transaction.getAccountNumber().equals(user.getCardAccountNumber()) && transaction.method.equals(Transaction.PAYMENT_METHOD_CARD)) ||
//                (user.getTransferAccountNumber() != null && !transaction.getAccountNumber().equals(user.getTransferAccountNumber()) && transaction.method.equals(Transaction.PAYMENT_METHOD_TRANSFER)) ){
//            return returnDeclinedEvent(transaction, String.format("Cannot withdraw with a new account %s", transaction.getAccountNumber()));
//        }
        return validateOnlyDebitCardPayment(transaction);
    }

    private Event validateOnlyDebitCardPayment(Transaction transaction) {
        if (transaction.method.equals(Transaction.PAYMENT_METHOD_CARD)) {
            BinMapping bin = getBinMapping(transaction);
            if (bin == null) return null;
            if (!bin.type.equals(BinMapping.DEBIT_CARD)) {
                return returnDeclinedEvent(transaction, "Only DC cards allowed; got CC");
            }
        }
        return executeSuccessfulTransaction(transaction);
    }

    private Event executeSuccessfulTransaction(Transaction transaction) {
        User user = getUser(transaction);
        if (user == null) return null;
//        if (transaction.method.equals(Transaction.PAYMENT_METHOD_CARD)
//                && user.getCardAccountNumber() == null){
//            user.setCardAccountNumber(transaction.getAccountNumber());
//        }
//        if (transaction.method.equals(Transaction.PAYMENT_METHOD_TRANSFER)
//                && user.getTransferAccountNumber() == null){
//            user.setTransferAccountNumber(transaction.getAccountNumber());
//        }

        if (transaction.type.equals(Transaction.TRANSACTION_TYPE_DEPOSIT)){
            user.setLastUsedDepositAccount(transaction.accountNumber);
            user.setBalance(user.getBalance() + transaction.amount);
        }
        if (transaction.type.equals(Transaction.TRANSACTION_TYPE_WITHDRAW))
            user.setBalance(user.getBalance() - transaction.amount);
        return new Event(transaction.transactionId, Event.STATUS_APPROVED, "OK");
    }

    private BinMapping getBinMapping(Transaction transaction){
        if (currentBinMapping != null){
            return currentBinMapping;
        } else {
            long accountNumber = Long.parseLong(transaction.accountNumber.substring(0,10));
            Optional<BinMapping> bin = binMappings.stream().filter(e -> e.getRangeFrom() <= accountNumber
                    && accountNumber <= e.getRangeTo()).findFirst();
            if (bin.isPresent()) currentBinMapping = bin.get();
            return bin.orElse(null);
        }
    }

    private User getUser(Transaction transaction){
        if (currentUser != null) return currentUser;
        Optional<User> user = users.stream().filter(u -> u.userId.equals(transaction.userId)
                && u.frozen.equals(User.USER_NOT_FROZEN)).findFirst();
        user.ifPresent(value -> currentUser = value);
        return user.orElse(null);
    }

    private Integer convertToInt(Character character){
        if (Character.isAlphabetic(character)){
            return character - 55;
        } else if (Character.isDigit(character)){
            return character - 48;
        }
        return null;
    }
    private String formatAmount(Float amount){
        return String.format("%.2f", amount).replace(",",".");
    }
    private Event returnDeclinedEvent(Transaction transaction, String message){
        return new Event(transaction.transactionId, Event.STATUS_DECLINED, message);
    }

    private String getISO3(String iso2Country){
        Locale locale = new Locale("", iso2Country);
        return locale.getISO3Country();
    }
}