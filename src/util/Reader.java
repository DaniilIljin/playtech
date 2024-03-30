package util;

import dto.BinMapping;
import dto.Transaction;
import dto.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Reader {
    public static List<User> readUsers(final Path path) {
        ArrayList<User> users = new ArrayList<>();
        try (FileReader reader = new FileReader(path.toFile());
             BufferedReader br = new BufferedReader(reader)) {
            br.readLine(); // For skipping heading line
            String line;
            while ((line = br.readLine()) != null) {
                User user = processUser(line, path.toString());
                if (user != null){
                    users.add(user);
                }
            }
        } catch (IOException e){
            throw new RuntimeException(String.format("Can not find or read file %s", path));
        }
        return users;
    }

    private static User processUser(String line, String filePath){
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
        } catch (IndexOutOfBoundsException e) {
            System.out.printf(
                    "Skipped line: Wrong number of parameters in line %s in %s. Expected: %d, Given: %d%n",line, filePath, 9, values.length);
            return null;
        }catch (NumberFormatException e){
            System.out.printf("Skipped line: Can not covert value to BigDecimal in line %s in %s.",line, filePath);
            return null;
        } catch (Exception e){
            System.out.printf("Found and skipped corrupted line (%s) in file (%s)%n", line, filePath);
            return null;
        }
        return user;
    }

    public static List<Transaction> readTransactions(final Path path){
        ArrayList<Transaction> transactions = new ArrayList<>();
        try (FileReader reader = new FileReader(path.toFile());
             BufferedReader br = new BufferedReader(reader)) {
            br.readLine(); // For skipping heading line
            String line;
            while ((line = br.readLine()) != null) {
                Transaction transaction = processTransaction(line, path.toString());
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        } catch (IOException e){
            throw new RuntimeException(String.format("Can not find or read file %s", path), e);
        }
        return transactions;
    }

    private static Transaction processTransaction(String line, String filePath) {
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
                System.out.printf("Skipped line: Payment method can be only CARD or TRANSFER in line %s in %s. Given: %s%n", line, filePath, values[4]);
                return null;
            }
            if (values[4].equals(Transaction.PAYMENT_METHOD_CARD)){
                Long.parseLong(values[5].substring(0,10));
            }
            transaction.setAccountNumber(values[5]);
            transaction.setAmount(new BigDecimal(values[3]));
        } catch (IndexOutOfBoundsException e) {
            System.out.printf("Skipped line: Wrong number of parameters in line %s in %s. Expected: %d, Given: %d%n",line, filePath, 6, values.length);
            return null;
        }catch (NumberFormatException e){
            System.out.printf("Skipped line: Can not convert value to Long or BigDecimal in line %s in %s.",line, filePath);
            return null;
        } catch (Exception e){
            System.out.printf("Found and skipped corrupted line (%s) in file (%s)%n", line, filePath);
            return null;
        }
        return transaction;
    }

    public static List<BinMapping> readBinMappings(final Path path){
        ArrayList<BinMapping> binMappings = new ArrayList<>();
        try (FileReader reader = new FileReader(path.toFile());
             BufferedReader br = new BufferedReader(reader)) {
            br.readLine(); // For skipping heading line
            String line;
            while ((line = br.readLine()) != null) {
                BinMapping binMapping = processBinMapping(line, path.toString());
                if (binMapping != null) {
                    binMappings.add(binMapping);
                }
            }
        }catch (IOException e){
            throw new RuntimeException(String.format("Can not find or read file %s", path), e);
        }
        return binMappings;
    }

    private static BinMapping processBinMapping(String line, String filePath) {
        String[] values = line.split(",");
        BinMapping binMapping = new BinMapping();
        try {
            binMapping.setName(values[0]);
            binMapping.setRangeFrom(Long.parseLong(values[1]));
            binMapping.setRangeTo(Long.parseLong(values[2]));
            if(values[3].equals(BinMapping.DEBIT_CARD) ||
                    values[3].equals(BinMapping.CREDIT_CARD)){
                System.out.printf("Skipped line: BinMapping type can be CC or DC in line %s in %s. Given: %s%n",line, filePath, values[3]);
                binMapping.setType(values[3]);
            } else {
                return null;
            }
            if (values[4].length() != 3) {
                System.out.printf("Skipped line: Country must be represented in 3-letters C in line %s in %s. Given: %s%n",line, filePath, values[4]);
                return null;
            }
            binMapping.setCountry(values[4]);
        } catch (IndexOutOfBoundsException e) {
            System.out.printf("Skipped line: Wrong number of parameters in line %s in %s. Expected: %d, Given: %d%n",line, filePath, 5, values.length);
            return null;
        }catch (NumberFormatException e){
            System.out.printf(" Skipped line: Can not convert value to Long in line %s in %s.",line, filePath);
            return null;
        } catch (Exception e){
            System.out.printf("Found and skipped corrupted line (%s) in file (%s)%n", line, filePath);
            return null;
        }
        return binMapping;
    }
}
