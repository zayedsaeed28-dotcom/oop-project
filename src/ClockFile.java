import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;

// Saves and loads the minute shown on the simulation clock.
public class ClockFile {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
                             .withResolverStyle(ResolverStyle.STRICT);

    public static void save(Path file, SimulationClock clock) throws IOException {
        LocalDateTime time = clock.getCurrentTime();
        if (time == null) {
            throw new IllegalStateException("Set the clock before saving.");
        }

        // Finish writing the new time before replacing the previous save.
        Path target = file.toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), "clock-", ".tmp");
        try {
            Files.write(temp, Collections.singletonList(time.format(FORMAT)));
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static SimulationClock load(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        if (lines.size() != 1) {
            throw new IOException("The clock file must contain one date and time.");
        }

        // Start from the saved minute, even if the program was closed for hours.
        try {
            LocalDateTime time = LocalDateTime.parse(lines.get(0), FORMAT);
            SimulationClock clock = new SimulationClock();
            clock.start(time);
            return clock;
        } catch (DateTimeParseException ex) {
            throw new IOException("Invalid clock time. Use dd/MM/yyyy HH:mm.", ex);
        }
    }
}
