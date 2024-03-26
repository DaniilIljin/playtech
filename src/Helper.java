import dto.Transaction;
import dto.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Helper {

    private static List<String> readLines(final Path filePath) throws IOException{
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
        for (String line : readLines(filePath)) {
            User user = processUser(line);
            if (user != null) users.add(user);
        }
        return users;
    }

    private static User processUser(String line){
        User newUser = new User();
        String[] values = line.split(",");
        if (values.length != User.class.getDeclaredFields().length) return null;
        newUser.userId = values[0];
        newUser.username = values[1];
        newUser.country = values[3];
        newUser.frozen = values[4];
        try{
            newUser.balance = Float.parseFloat(values[2]);
            newUser.depositMin = Float.parseFloat(values[5]);
            newUser.depositMax = Float.parseFloat(values[6]);
            newUser.withdrawMin = Float.parseFloat(values[7]);
            newUser.withdrawMax = Float.parseFloat(values[8]);
        } catch (Exception e){
            return null;
        }
        return newUser;
    }

    public static List<Transaction> readTransactions(final Path path) throws IOException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        for (String line : readLines(path)) {
            Transaction transaction = processTransaction(line);
            if (transaction != null) transactions.add(transaction);
        }
        return transactions;
    }

    private static Transaction processTransaction(String line) {
        Transaction transaction = new Transaction();
        String[] values = line.split(",");
        if (values.length != Transaction.class.getDeclaredFields().length) return null;

        return transaction;
    }
}
