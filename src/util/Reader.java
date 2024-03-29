package util;

import dto.BinMapping;
import dto.Transaction;
import dto.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Reader {

    private static List<String> readFileLines(final Path filePath) throws IOException{
        ArrayList<String> lines = new ArrayList<>();
        try (FileReader reader = new FileReader(filePath.toFile());
             BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
    public static List<User> readUsers(final Path filePath) throws IOException {
        ArrayList<User> users = new ArrayList<>();
        for (String line : readFileLines(filePath)) {
            User user = processUser(line);
            if (user != null) users.add(user);
        }
        return users;
    }

    private static User processUser(String line){
        String[] values = line.split(",");
        User user = new User();
        user.userId = values[0];
        user.username = values[1];
        user.country = values[3];
        user.frozen = values[4];
        try{
            user.balance = Float.parseFloat(values[2]);
            user.depositMin = Float.parseFloat(values[5]);
            user.depositMax = Float.parseFloat(values[6]);
            user.withdrawMin = Float.parseFloat(values[7]);
            user.withdrawMax = Float.parseFloat(values[8]);
        } catch (Exception e){
            return null;
        }
        return user;
    }

    public static List<Transaction> readTransactions(final Path path) throws IOException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        for (String line : readFileLines(path)) {
            Transaction transaction = processTransaction(line);
            if (transaction != null) transactions.add(transaction);
        }
        return transactions;
    }

    private static Transaction processTransaction(String line) {
        String[] values = line.split(",");
        Transaction transaction = new Transaction();
        transaction.transactionId = values[0];
        transaction.userId = values[1];
        transaction.type = values[2];
        if(values[4].equals(Transaction.PAYMENT_METHOD_CARD) ||
                values[4].equals(Transaction.PAYMENT_METHOD_TRANSFER)){
            transaction.method = values[4];
        } else {
            return null;
        }
        if (values[4].equals(Transaction.PAYMENT_METHOD_CARD)){
            try {
                Long.parseLong(values[5]);
            } catch (Exception e){
                return null;
            }
        }
        transaction.accountNumber = values[5];
        try {
            transaction.amount = Float.parseFloat(values[3]);
        } catch (Exception e){
            return null;
        }
        return transaction;
    }

    public static List<BinMapping> readBinMappings(final Path path) throws IOException {
        ArrayList<BinMapping> binMappings = new ArrayList<>();
        for (String line : readFileLines(path)) {
            BinMapping binMapping = processBinMapping(line);
            if (binMapping != null) binMappings.add(binMapping);
        }
        return binMappings;
    }

    private static BinMapping processBinMapping(String line) {
        String[] values = line.split(",");
        BinMapping binMapping = new BinMapping();
        binMapping.name = values[0];
        try {
            binMapping.rangeFrom = Long.parseLong(values[1]);
            binMapping.rangeTo = Long.parseLong(values[2]);
        }catch (Exception e){
            return null;
        }
        if(values[3].equals(BinMapping.DEBIT_CARD) ||
                values[3].equals(BinMapping.CREDIT_CARD)){
            binMapping.type = values[3];
        } else {
            return null;
        }
        if (values[4].length() != 3) return null;
        binMapping.country = values[4];
        return binMapping;
    }
}
