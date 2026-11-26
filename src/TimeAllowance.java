import java.time.Duration;
import java.util.ArrayList;

// Keeps track of one approved student's GPU minutes.
public class TimeAllowance {
    private final String studentId;
    private final int allocatedMinutes;
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
            if (job.getStartTime() != null && job.getFinishTime() != null) {
                used += (int) Duration.between(job.getStartTime(), job.getFinishTime()).toMinutes();
            }
        }
        return used;
    }

    public int getAvailableMinutes() {
        return allocatedMinutes - getUsedMinutes() - getReservedMinutes();
    }
}
