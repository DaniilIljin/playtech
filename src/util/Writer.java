package util;

import dto.Event;
import dto.User;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Writer {
    public static void writeBalances(final Path filePath, final List<User> users) {
        try (final FileWriter writer = new FileWriter(filePath.toFile(), false)) {
            writer.append("USER_ID,BALANCE\n");
            for (final var user : users) {
                writer.append(user.getUserId()).append(",").append(String.format("%.2f", user.getBalance()).replace(",",".")).append("\n");
            }
        }catch (IOException e){
            throw new RuntimeException(String.format("Can not write to file %s", filePath), e);
        }
    }

    public static void writeEvents(final Path filePath, final List<Event> events) {
        try (final FileWriter writer = new FileWriter(filePath.toFile(), false)) {
            writer.append("TRANSACTION_ID,STATUS,MESSAGE\n");
            for (final var event : events) {
                writer.append(event.transactionId).append(",").append(event.status).append(",").append(event.message).append("\n");
            }
        } catch (IOException e){
            throw new RuntimeException(String.format("Can not write to file %s", filePath), e);
        }
    }
}
