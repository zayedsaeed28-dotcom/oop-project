import java.time.LocalDateTime;

// Holds the time shared by the dashboard and the scheduler.
public class SimulationClock {
    private LocalDateTime currentTime;

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public void start(LocalDateTime time) {
        if (currentTime != null) {
            throw new IllegalStateException("The clock has already started.");
        }
        if (time == null) {
            throw new IllegalArgumentException("Choose a start date and time.");
        }
        currentTime = time.withSecond(0).withNano(0);
    }

    // Time moves only when the user advances the simulation.
    public void advanceMinute() {
        if (currentTime == null) {
            throw new IllegalStateException("Set the start time first.");
        }
        currentTime = currentTime.plusMinutes(1);
    }
}
