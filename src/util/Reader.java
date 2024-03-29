package util;

import dto.BinMapping;
import dto.Transaction;
import dto.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Reader {
    public static List<User> readUsers(final Path filePath) throws IOException {
        ArrayList<User> users = new ArrayList<>();
        try (FileReader reader = new FileReader(filePath.toFile());
             BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                User user = processUser(line);
                if (user != null) users.add(user);
            }
        }
        return users;
    }

    private static User processUser(String line){
        String[] values = line.split(",");
        User user = new User();
        try{
            user.setUserId(values[0]);
            user.setUsername(values[1]);
            user.setBalance(new BigDecimal(values[2]));
            user.setCountry(values[3]);
            user.setFrozen(values[4]);
            user.setDepositMin(new BigDecimal(values[5]));
            user.setDepositMax(new BigDecimal(values[6]));
            user.setWithdrawMin(new BigDecimal(values[7]));
            user.setWithdrawMax(new BigDecimal(values[8]));
        } catch (Exception e){
            return null;
        }
        return user;
    }

    public static List<Transaction> readTransactions(final Path path) throws IOException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try (FileReader reader = new FileReader(path.toFile());
             BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                Transaction transaction = processTransaction(line);
                if (transaction != null) transactions.add(transaction);
            }
        }
        return transactions;
    }

    private static Transaction processTransaction(String line) {
        String[] values = line.split(",");
        Transaction transaction = new Transaction();
        try {
            transaction.setTransactionId(values[0]);
            transaction.setUserId(values[1]);
            transaction.setType(values[2]);
            transaction.setAmount(new BigDecimal(values[3]));
            if(values[4].equals(Transaction.PAYMENT_METHOD_CARD) ||
                    values[4].equals(Transaction.PAYMENT_METHOD_TRANSFER)){
                transaction.setMethod(values[4]);
            } else {
                return null;
            }
            if (values[4].equals(Transaction.PAYMENT_METHOD_CARD)){
                Long.parseLong(values[5].substring(0,10));
            }
            transaction.setAccountNumber(values[5]);
            transaction.setAmount(new BigDecimal(values[3]));
        } catch (Exception e){
            return null;
        }
        return transaction;
    }

    public static List<BinMapping> readBinMappings(final Path path) throws IOException {
        ArrayList<BinMapping> binMappings = new ArrayList<>();
        try (FileReader reader = new FileReader(path.toFile());
             BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                BinMapping binMapping = processBinMapping(line);
                if (binMapping != null) binMappings.add(binMapping);
            }
        }
        return binMappings;
    }

    private static BinMapping processBinMapping(String line) {
        String[] values = line.split(",");
        BinMapping binMapping = new BinMapping();
        try {
            binMapping.setName(values[0]);
            binMapping.setRangeFrom(Long.parseLong(values[1]));
            binMapping.setRangeTo(Long.parseLong(values[2]));
            if(values[3].equals(BinMapping.DEBIT_CARD) ||
                    values[3].equals(BinMapping.CREDIT_CARD)){
                binMapping.setType(values[3]);
            } else {
                return null;
            }
            if (values[4].length() != 3) return null;
            binMapping.setCountry(values[4]);
        } catch (Exception e){
            return null;
        }
        return binMapping;
    }
}
