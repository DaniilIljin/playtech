import dto.BinMapping;
import dto.Event;
import dto.Transaction;
import dto.User;
import util.Constants;
import util.Reader;
import util.Writer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;

public class TransactionProcessorSample {

    private List<Event> events = new ArrayList<>();
    private List<User> users;
    private List<Transaction> transactions;
    private List<BinMapping> binMappings;
    private StringBuilder stringBuilder = new StringBuilder();

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
            Event event = validateUniqueId(transaction);
            events.add(event);
        }
    }
    private Event validateUniqueId(Transaction transaction) {
        Optional<Event> event = events.stream().filter(
                e -> e.transactionId.equals(transaction.transactionId)).findFirst();
        if (event.isPresent()){
            return returnDeclinedEvent(transaction, String.format("Transaction %s already processed (id non-unique)", transaction.transactionId));
        }
        return validateUserExistAndNotFrozen(transaction);
    }

    private Event validateUserExistAndNotFrozen(Transaction transaction){
        Optional<User> user = users.stream().filter(u -> u.userId.equals(transaction.userId)
                && u.frozen.equals(Constants.USER_NOT_FROZEN)).findFirst();
        if(user.isEmpty()){
            return returnDeclinedEvent(transaction, String.format("User %s not found in Users", transaction.userId));
        }
        return validateTransferPaymentMethod(transaction);
    }

    private Event validateTransferPaymentMethod(Transaction transaction) {
        if (transaction.method.equals(Constants.PAYMENT_METHOD_TRANSFER)){
            stringBuilder.append(transaction.accountNumber);
            String firstPart = stringBuilder.substring(0, 4);
            String secondPart = stringBuilder.substring(4, 8);
            String thirdPart = stringBuilder.substring(8);
            stringBuilder.setLength(0);
            for (char c : secondPart.toCharArray()) {
                stringBuilder.append(convertToInt(c));
            }
            stringBuilder.append(thirdPart);
            for (char c : firstPart.toCharArray()) {
                stringBuilder.append(convertToInt(c));
            }
            BigInteger checkNumber = new BigInteger(stringBuilder.toString());
            stringBuilder.setLength(0);
            if (!checkNumber.remainder(BigInteger.valueOf(97)).equals(BigInteger.ONE)){
                return returnDeclinedEvent(transaction, String.format("Invalid iban %s", transaction.accountNumber));
            }
        }
        return validateCountry(transaction);
    }
    private Event validateCountry(Transaction transaction) {
        String userCountry = getTransactionUser(transaction).country;
        if (transaction.method.equals(Constants.PAYMENT_METHOD_CARD)){
            Long cardNumber = Long.parseLong(transaction.accountNumber.substring(0,10));
            Optional<BinMapping> bin = binMappings.stream().filter(b -> b.getRangeFrom() <= cardNumber
                    && b.getRangeTo() >= cardNumber).findFirst();
            if (!bin.get().country.substring(0,2).equals(userCountry) ){
                return returnDeclinedEvent(transaction, String.format("Invalid country %s; expected %s", bin.get().country, userCountry ));
            }
        } else if (transaction.method.equals(Constants.PAYMENT_METHOD_TRANSFER)){
            if (!userCountry.equals(transaction.accountNumber.substring(0,2))){
                return returnDeclinedEvent(transaction, String.format("Invalid account country %s; expected %s", transaction.accountNumber.substring(0,2), userCountry ));
            }
        }
        return validateAmountAndUserLimits(transaction);
    }

    private Event validateAmountAndUserLimits(Transaction transaction) {
        if (transaction.amount < 0){
            return returnDeclinedEvent(transaction, "Amount is negative for transaction");
        }

        User user = getTransactionUser(transaction);

        if (transaction.type.equals(Constants.TRANSACTION_TYPE_WITHDRAW)){
            if (user.withdrawMax < transaction.amount){
                return returnDeclinedEvent(transaction, String.format("Amount %s is over the withdraw limit of %s", formatAmount(transaction.amount), formatAmount(user.withdrawMax)));
            }
            if (user.withdrawMin > transaction.amount){
                return returnDeclinedEvent(transaction, String.format("Amount %s is under the withdraw limit of %s", formatAmount(transaction.amount), formatAmount(user.withdrawMin)));
            }
        }
        if (transaction.type.equals(Constants.TRANSACTION_TYPE_DEPOSIT)){
            if (user.depositMax < transaction.amount){
                return returnDeclinedEvent(transaction, String.format("Amount %s is over the deposit limit of %s", formatAmount(transaction.amount), formatAmount(user.depositMax)));
            }
            if (user.depositMin > transaction.amount){
                return returnDeclinedEvent(transaction, String.format("Amount %s is under the deposit limit of %s", formatAmount(transaction.amount), formatAmount(user.depositMin)));
            }
        }
        return validateEnoughForWithdraw(transaction);
    }

    private Event validateEnoughForWithdraw(Transaction transaction) {
        if (transaction.type.equals(Constants.TRANSACTION_TYPE_WITHDRAW)){
            User user = getTransactionUser(transaction);
            Optional<User> someoneAccount = users.stream().filter(u -> transaction.getAccountNumber().equals(u.getCardAccountNumber()) ||
                    transaction.getAccountNumber().equals(u.getTransferAccountNumber())).findFirst();
            if (someoneAccount.isPresent() && !someoneAccount.get().getUserId().equals(transaction.userId))
                return returnDeclinedEvent(transaction, String.format("Account %s is in use by other user", transaction.getAccountNumber()));

            if (user.balance < transaction.amount)
                return returnDeclinedEvent(transaction, String.format("Not enough balance to withdraw %s - balance is too low at %s", formatAmount(transaction.getAmount()), formatAmount(user.getBalance())));
        }
        return validateWithdrawSameAccount(transaction);
    }
    private Event validateWithdrawSameAccount(Transaction transaction) {
        return validateTransactionType(transaction);
    }
    private Event validateTransactionType(Transaction transaction) {

        return validateOneUserOneAccountOfEachType(transaction);
    }
    private Event validateOneUserOneAccountOfEachType(Transaction transaction) {
        User user = getTransactionUser(transaction);
        if ((user.getCardAccountNumber() != null &&  !user.getCardAccountNumber().equals(transaction.getAccountNumber()) && transaction.method.equals(Constants.PAYMENT_METHOD_TRANSFER)) ||
                (user.getTransferAccountNumber() != null && !user.getTransferAccountNumber().equals(transaction.getAccountNumber()) && transaction.method.equals(Constants.PAYMENT_METHOD_CARD))){
            return returnDeclinedEvent(transaction, String.format("Cannot withdraw with a new account %s", transaction.getAccountNumber()));
        }
        return validateOnlyDebitCardPayment(transaction);
    }

    private Event validateOnlyDebitCardPayment(Transaction transaction) {
        if (transaction.method.equals(Constants.PAYMENT_METHOD_CARD)) {
            long accountNumber = Long.parseLong(transaction.accountNumber.substring(0,10));
            Optional<BinMapping> bin = binMappings.stream().filter(e -> e.getRangeFrom() <= accountNumber
                    && accountNumber <= e.getRangeTo()).findFirst();
            if (bin.isEmpty() || !bin.get().type.equals(Constants.DEBIT_CARD)) {
                return returnDeclinedEvent(transaction, "Only DC cards allowed; got CC");
            }
        }
        return executeSuccessfulTransaction(transaction);
    }

    private Event executeSuccessfulTransaction(Transaction transaction) {
        User user = getTransactionUser(transaction);
        if (transaction.method.equals(Constants.PAYMENT_METHOD_TRANSFER)
                && user.getCardAccountNumber() == null){
            user.setCardAccountNumber(transaction.getAccountNumber());
        }

        if (transaction.method.equals(Constants.PAYMENT_METHOD_CARD)
                && user.getTransferAccountNumber() == null){
            user.setTransferAccountNumber(transaction.getAccountNumber());
        }

        return new Event(transaction.transactionId, Event.STATUS_APPROVED, "OK");
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
        return String.format("%.2f", amount);
    }
    private User getTransactionUser(Transaction transaction) {
        return users.stream().filter(u -> u.userId.equals(transaction.userId)).toList().getFirst();
    }
    private Event returnDeclinedEvent(Transaction transaction, String message){
        return new Event(transaction.transactionId, Event.STATUS_DECLINED, message);
    }
}