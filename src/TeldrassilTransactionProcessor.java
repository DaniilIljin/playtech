import dto.BinMapping;
import dto.Event;
import dto.Transaction;
import dto.User;
import util.Reader;
import util.Writer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;

public class TeldrassilTransactionProcessor {
    private final List<Event> events = new ArrayList<>();
    private final Map<String, String> userAccounts = new HashMap<>();
    private final List<User> users;
    private final List<Transaction> transactions;
    private final List<BinMapping> binMappings;
    private final StringBuilder stringBuilder = new StringBuilder();

    public TeldrassilTransactionProcessor(
            final List<User> users,
            final List<Transaction> transactions,
            final List<BinMapping> binMappings) {
        this.users = users;
        this.transactions = transactions;
        this.binMappings = binMappings;
    }

    public static void main(final String[] args) {
        if (args.length != 5) throw new RuntimeException("Wrong number of file paths provided. Expected: 5. Actual: " + args.length);

        List<User> users = Reader.readUsers(Paths.get(args[0]));
        List<Transaction> transactions = Reader.readTransactions(Paths.get(args[1]));
        List<BinMapping> binMappings = Reader.readBinMappings(Paths.get(args[2]));

        TeldrassilTransactionProcessor transactionProcessor = new TeldrassilTransactionProcessor(users, transactions, binMappings);
        transactionProcessor.processTransactions();

        Writer.writeBalances(Paths.get(args[3]), users);
        Writer.writeEvents(Paths.get(args[4]), transactionProcessor.events);
        System.out.printf("Transaction processing finished! Given transactions: %d, Processed transactions: %d%n", transactions.size(), transactionProcessor.events.size());
    }

    private void processTransactions() {
        for (Transaction transaction : transactions) {
            Optional<User> user = getUserIfExistsAndNotFrozen(transaction);
            if (user.isEmpty()) {
                addDeclinedEvent(transaction, String.format("User %s not found in Users", transaction.getUserId()));
                continue;
            }
            if (validate(transaction, user.get()))
                events.add(createSuccessfulEvent(transaction, user.get()));
        }
    }

    private boolean validate(Transaction transaction, User user) {
        return validateUniqueId(transaction) &&
                validateCorrectUserAccount(transaction, user) &&
                validateTransactionType(transaction) &&
                validateAmountAndUserLimits(transaction, user) &&
                validateEnoughForWithdraw(transaction, user) &&
                validateIBAN(transaction) &&
                validateWithdrawFromExistingAccount(transaction) &&
                validateOnlyDebitCardPayment(transaction) &&
                validateCountry(transaction, user)
                ;
    }

    private Optional<User> getUserIfExistsAndNotFrozen(Transaction transaction) {
        return users.stream().filter(u -> u.getUserId().equals(transaction.getUserId())
                && u.getFrozen().equals(User.USER_NOT_FROZEN)).findFirst();
    }

    private boolean validateUniqueId(Transaction transaction) {
        boolean isIdUnique = events.stream().anyMatch(e -> e.transactionId.equals(transaction.getTransactionId()));
        if (isIdUnique) {
            addDeclinedEvent(transaction, String.format("Transaction %s already processed (id non-unique)", transaction.getTransactionId()));
            return false;
        }
        return true;
    }

    private boolean validateIBAN(Transaction transaction) {
        if (transaction.getMethod().equals(Transaction.PAYMENT_METHOD_TRANSFER)) {
            if (transaction.getAccountNumber().length() < 15 ||
                    transaction.getAccountNumber().length() > 34) {
                addDeclinedEvent(transaction, String.format("Invalid iban %s", transaction.getAccountNumber()));
                return false;
            }

            stringBuilder.setLength(0);
            stringBuilder.append(transaction.getAccountNumber());
            String checkDigits = stringBuilder.substring(0, 4);
            String BBAN = stringBuilder.substring(4);
            stringBuilder.setLength(0);

            for (char c : (BBAN + checkDigits).toCharArray()) {
                if (Character.isLetter(c)) {
                    stringBuilder.append(Character.getNumericValue(c));
                } else {
                    stringBuilder.append(c);
                }
            }
            try {
                BigInteger checkNumber = new BigInteger(stringBuilder.toString());
                if (!checkNumber.remainder(BigInteger.valueOf(97)).equals(BigInteger.ONE)) {
                    addDeclinedEvent(transaction, String.format("Invalid iban %s", transaction.getAccountNumber()));
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private boolean validateAmountAndUserLimits(Transaction transaction, User user) {
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            addDeclinedEvent(transaction, "Amount is negative for transaction");
            return false;
        }
        if (transaction.getType().equals(Transaction.TRANSACTION_TYPE_WITHDRAW)) {
            if (user.getWithdrawMax().compareTo(transaction.getAmount()) < 0) {
                addDeclinedEvent(transaction, String.format("Amount %s is over the withdraw limit of %s", formatAmount(transaction.getAmount()), formatAmount(user.getWithdrawMax())));
                return false;
            }
            if (user.getWithdrawMin().compareTo(transaction.getAmount()) > 0) {
                addDeclinedEvent(transaction, String.format("Amount %s is under the withdraw limit of %s", formatAmount(transaction.getAmount()), formatAmount(user.getWithdrawMin())));
                return false;
            }
        }
        if (transaction.getType().equals(Transaction.TRANSACTION_TYPE_DEPOSIT)) {
            if (user.getDepositMax().compareTo(transaction.getAmount()) < 0) {
                addDeclinedEvent(transaction, String.format("Amount %s is over the deposit limit of %s", formatAmount(transaction.getAmount()), formatAmount(user.getDepositMax())));
                return false;
            }
            if (user.getDepositMin().compareTo(transaction.getAmount()) > 0) {
                addDeclinedEvent(transaction, String.format("Amount %s is under the deposit limit of %s", formatAmount(transaction.getAmount()), formatAmount(user.getDepositMin())));
                return false;
            }
        }
        return true;
    }

    private boolean validateCountry(Transaction transaction, User user) {
        if (transaction.getMethod().equals(Transaction.PAYMENT_METHOD_CARD)) {
            BinMapping bin = getBinMapping(transaction);
            if (bin == null) return false;

            if (!bin.getCountry().equals(getISO3(user.getCountry()))) {
                addDeclinedEvent(transaction, String.format("Invalid country %s; expected %s (%s)", bin.getCountry(), user.getCountry(), getISO3(user.getCountry())));
                return false;
            }
        }
        if (transaction.getMethod().equals(Transaction.PAYMENT_METHOD_TRANSFER)) {
            if (!user.getCountry().equals(transaction.getAccountNumber().substring(0, 2))) {
                addDeclinedEvent(transaction, String.format("Invalid account country %s; expected %s", transaction.getAccountNumber().substring(0, 2), user.getCountry()));
                return false;
            }
        }
        return true;
    }

    private boolean validateEnoughForWithdraw(Transaction transaction, User user) {
        if (transaction.getType().equals(Transaction.TRANSACTION_TYPE_WITHDRAW)
                && user.getBalance().compareTo(transaction.getAmount()) < 0) {
            addDeclinedEvent(transaction, String.format("Not enough balance to withdraw %s - balance is too low at %s", formatAmount(transaction.getAmount()), formatAmount(user.getBalance())));
            return false;
        }
        return true;
    }

    private boolean validateWithdrawFromExistingAccount(Transaction transaction) {
        if (transaction.getType().equals(Transaction.TRANSACTION_TYPE_WITHDRAW)
                && userAccounts.get(transaction.getAccountNumber()) == null) {
            addDeclinedEvent(transaction, String.format("Cannot withdraw with a new account %s", transaction.getAccountNumber()));
            return false;
        }
        return true;
    }

    private boolean validateTransactionType(Transaction transaction) {
        if (!transaction.getType().equals(Transaction.TRANSACTION_TYPE_DEPOSIT)
                && !transaction.getType().equals(Transaction.TRANSACTION_TYPE_WITHDRAW)) {
            addDeclinedEvent(transaction, "Wrong transaction method");
            return false;
        }
        return true;
    }

    private boolean validateCorrectUserAccount(Transaction transaction, User user) {
        String userId = userAccounts.get(transaction.getAccountNumber());
        if (userId != null && !user.getUserId().equals(userId)) {
            addDeclinedEvent(transaction, String.format("Account %s is in use by other user", transaction.getAccountNumber()));
            return false;
        }
        return true;
    }

    private boolean validateOnlyDebitCardPayment(Transaction transaction) {
        if (transaction.getMethod().equals(Transaction.PAYMENT_METHOD_CARD)) {
            BinMapping bin = getBinMapping(transaction);
            if (bin == null) return false;
            if (!bin.getType().equals(BinMapping.DEBIT_CARD)) {
                addDeclinedEvent(transaction, "Only DC cards allowed; got CC");
                return false;
            }
        }
        return true;
    }

    private Event createSuccessfulEvent(Transaction transaction, User user) {
        userAccounts.put(transaction.getAccountNumber(), user.getUserId());
        if (transaction.getType().equals(Transaction.TRANSACTION_TYPE_DEPOSIT)) {
            user.setBalance(user.getBalance().add(transaction.getAmount()));
        }
        if (transaction.getType().equals(Transaction.TRANSACTION_TYPE_WITHDRAW))
            user.setBalance(user.getBalance().subtract(transaction.getAmount()));
        return new Event(transaction.getTransactionId(), Event.STATUS_APPROVED, "OK");
    }

    private BinMapping getBinMapping(Transaction transaction) {
        long accountNumber = Long.parseLong(transaction.getAccountNumber().substring(0, 10));
        Optional<BinMapping> bin = binMappings.stream().filter(e -> e.getRangeFrom() <= accountNumber
                && accountNumber <= e.getRangeTo()).findFirst();
        return bin.orElse(null);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount).replace(",", ".");
    }

    private void addDeclinedEvent(Transaction transaction, String message) {
        events.add(new Event(transaction.getTransactionId(), Event.STATUS_DECLINED, message));
    }

    private String getISO3(String iso2Country) {
        return Locale.of("", iso2Country).getISO3Country();
    }
}