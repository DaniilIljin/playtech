import dto.BinMapping;
import dto.Event;
import dto.Transaction;
import dto.User;
import util.Constants;
import util.Reader;
import util.Writer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class TransactionProcessorSample {

    private List<Event> events = new ArrayList<>();
    private List<User> users;
    private List<Transaction> transactions;
    private List<BinMapping> binMappings;

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
            if (!validateUniqueId(transaction)) continue;
            if (!validateUserExistAndNotFrozen(transaction)) continue;
            if (!validatePaymentMethod(transaction)) continue;
//            if (!validateCountry(transaction)) continue;
            if (!validateAmountAndUserLimits(transaction)) continue;
            if (!validateWithdraw(transaction)) continue;
            if (!validateWithdrawSameAccount(transaction)) continue;
            if (!validateTransactionType(transaction)) continue;
            if (!validateOneUserOneAccount(transaction)) continue;
            executeTransaction(transaction);
        }
    }

    private void executeTransaction(Transaction transaction) {
        addApprovedEvent(transaction);
    }

    private boolean validateOneUserOneAccount(Transaction transaction) {
        return true;
    }

    private boolean validateTransactionType(Transaction transaction) {
        return true;
    }

    private boolean validateWithdrawSameAccount(Transaction transaction) {
        return true;
    }

    private boolean validateWithdraw(Transaction transaction) {
        if (transaction.type.equals(Constants.TRANSACTION_TYPE_WITHDRAW)){
            User user = getUser(transaction);
            if (user.balance < transaction.amount){
                addDeclinedEvent(transaction, "User balance is lower than withdraw a");
                return false;
            }
        }
        return true;
    }

    private boolean validateAmountAndUserLimits(Transaction transaction) {
        if (transaction.amount < 0){
            addDeclinedEvent(transaction, "Amount is negative for transaction: " + transaction.transactionId);
            return false;
        }

        User user = getUser(transaction);

        if (transaction.type.equals(Constants.TRANSACTION_TYPE_WITHDRAW)){
            if (user.withdrawMax < transaction.amount){
                addDeclinedEvent(transaction, String.format("Amount %f is over the withdraw limit of %f", transaction.amount, user.withdrawMax));
                return false;
            }
            if (user.withdrawMin > transaction.amount){
                addDeclinedEvent(transaction, String.format("Amount %f is under the withdraw limit of %f", transaction.amount, user.withdrawMin));
                return false;
            }
        }
        if (transaction.type.equals(Constants.TRANSACTION_TYPE_DEPOSIT)){
            if (user.depositMax < transaction.amount){
                addDeclinedEvent(transaction, "Amount is bigger than user's deposit limit for transaction: " + transaction.transactionId);
                return false;
            }
            if (user.depositMin > transaction.amount){
                addDeclinedEvent(transaction, "Amount is lower than user's deposit limit for transaction: " + transaction.transactionId);
                return false;
            }
        }
        return true;
    }

    private boolean validateCountry(Transaction transaction) {
        if (transaction.method.equals(Constants.PAYMENT_METHOD_CARD)){
            String country = getUser(transaction).country;
            Long cardNumber = Long.parseLong(transaction.accountNumber);
            Optional<BinMapping> bin = binMappings.stream().filter(b -> b.getRangeFrom() <= cardNumber
                    && b.getRangeTo() >= cardNumber).findFirst();
            if (bin.isEmpty()){
                addDeclinedEvent(transaction, "Country of a card and user do not match for transaction: "
                        + transaction.transactionId);
                return false;
            }
        }
        return true;
    }

    private boolean validatePaymentMethod(Transaction transaction) {
        return true;
    }

    private boolean validateUserExistAndNotFrozen(Transaction transaction){
        Optional<User> user = users.stream().filter(u -> u.userId.equals(transaction.userId)
                && u.frozen.equals(Constants.USER_NOT_FROZEN)).findFirst();
        if(user.isEmpty()){
            addDeclinedEvent(transaction,
                    String.format("User %s not found in Users", transaction.userId));
            return false;
        }
        return true;
    }

    private boolean validateUniqueId(Transaction transaction) {
        Optional<Event> event = events.stream().filter(
                e -> e.transactionId.equals(transaction.transactionId)
                        && e.status.equals(Event.STATUS_APPROVED)).findFirst();
        if (event.isPresent()){
            addDeclinedEvent(transaction,
                    String.format("Transaction %s already processed (id non-unique)", transaction.transactionId));
            return false;
        }
        return true;
    }
    private User getUser(Transaction transaction) {
        return users.stream().filter(u -> u.userId.equals(transaction.userId)).toList().getFirst();
    }
    private void addDeclinedEvent(Transaction transaction, String message){
        events.add(new Event(
                transaction.transactionId,
                Event.STATUS_DECLINED,
                message
        ));
    }
    private void addApprovedEvent(Transaction transaction){
        events.add(new Event(
                transaction.transactionId,
                Event.STATUS_APPROVED,
                "OK"
        ));
    }
}


