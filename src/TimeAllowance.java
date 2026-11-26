import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

// Keeps track of one approved student's GPU minutes.
public class TimeAllowance {
    private final String studentId;
    private int allocatedMinutes;
    private final ArrayList<String> grantHistory = new ArrayList<String>();
    private final ArrayList<Job> jobs = new ArrayList<Job>();

    // An approved student normally starts with 100 minutes.
    public TimeAllowance(String studentId) {
        this(studentId, 100);
    }

    public TimeAllowance(String studentId, int allocatedMinutes) {
        if (studentId == null || studentId.trim().isEmpty() || allocatedMinutes < 0) {
            throw new IllegalArgumentException("Check the student ID and allocated minutes.");
        }
        this.studentId = studentId.trim();
        this.allocatedMinutes = allocatedMinutes;
    }

    public int getAllocatedMinutes() {
        return allocatedMinutes;
    }

    // The admin adds extra minutes and records why the student needs them.
    public void grantExtraMinutes(int minutes, String reason) {
        if (minutes <= 0 || minutes > Integer.MAX_VALUE - allocatedMinutes) {
            throw new IllegalArgumentException("Enter a positive number of minutes within range.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a reason for the extra minutes.");
        }
        grantHistory.add(minutes + " minutes: " + reason.trim());
        allocatedMinutes += minutes;
    }

    // An admin can remove only minutes the student still has available.
public void reduceAllocatedMinutes(int minutes) {
    if (minutes <= 0) {
        throw new IllegalArgumentException("Enter a positive number of minutes.");
    }
    if (minutes > getAvailableMinutes()) {
        throw new IllegalArgumentException("You can only remove available minutes.");
    }
    allocatedMinutes -= minutes;
}
    // Give the caller a copy so it cannot change earlier grant records.
    public ArrayList<String> getGrantHistory() {
        return new ArrayList<String>(grantHistory);
    }

    // Reserve minutes before the scheduler starts the job.
    public void reserve(Job job) {
        if (job == null || !studentId.equals(job.getStudentId())) {
            throw new IllegalArgumentException("Choose a job belonging to this student.");
        }
        if (!job.getStatus().equals("Queued")) {
            throw new IllegalStateException("Only a queued job can be reserved.");
        }
        for (Job existing : jobs) {
            if (existing.getJobId() == job.getJobId()) {
                throw new IllegalStateException("This job was already reserved.");
            }
        }
        if (job.getEstimatedDuration() > getAvailableMinutes()) {
            throw new IllegalStateException("There are not enough available minutes.");
        }
        jobs.add(job);
    }

    // The full duration stays reserved while a job waits or runs.
    public int getReservedMinutes() {
        int reserved = 0;
        for (Job job : jobs) {
            if (job.getStatus().equals("Queued") || job.getStatus().equals("Running")) {
                reserved += job.getEstimatedDuration();
            }
        }
        return reserved;
    }

    // An ended job with a start time is charged only for the time it ran.
    public int getUsedMinutes() {
        int used = 0;
        for (Job job : jobs) {
            used += job.getChargedMinutes();
        }
        return used;
    }

    // Save the student's GPU minutes and extra-time grants to a text file.
public void saveSummary(Path file) throws IOException {
    ArrayList<String> lines = new ArrayList<String>();

    lines.add("Student ID: " + studentId);
    lines.add("Allocated minutes: " + getAllocatedMinutes());
    lines.add("Used minutes: " + getUsedMinutes());
    lines.add("Reserved minutes: " + getReservedMinutes());
    lines.add("Available minutes: " + getAvailableMinutes());

    lines.add("Extra time grants:");
    if (grantHistory.isEmpty()) {
        lines.add("None");
    } else {
        lines.addAll(grantHistory);
    }

    Files.write(file, lines);
}

    public int getAvailableMinutes() {
        return allocatedMinutes - getUsedMinutes() - getReservedMinutes();
    }
}
