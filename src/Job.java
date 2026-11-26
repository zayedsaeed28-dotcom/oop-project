import java.time.Duration;
import java.time.LocalDateTime;

// A Job stores one student's request to use a GPU.
public class Job {
    // These details stay the same after the job is submitted.
    private final int jobId;
    private final String studentId;
    private final String title;
    private final String applicationName;
    private final String description;
    private final String taskSize;
    private final int workloadUnits;
    private final LocalDateTime submissionTime;

    // A queued job has no GPU, start time, or finish time yet.
    private String status = "Queued";
    private Integer assignedGpuId;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    private LocalDateTime cancellationTime;
    private String cancelledBy;
    private String cancellationReason;

    // The scheduler supplies the job ID and the time on the simulation clock.
    public Job(int jobId, String studentId, String title, String applicationName,
               String description, String taskSize, LocalDateTime submissionTime) {
        if (jobId <= 0 || submissionTime == null) {
            throw new IllegalArgumentException("A job needs a positive ID and a submission time.");
        }
        this.jobId = jobId;
        this.studentId = requiredText(studentId, "Student ID");
        this.title = requiredText(title, "Title");
        this.applicationName = requiredText(applicationName, "Application name");
        this.description = requiredText(description, "Description");
        this.taskSize = requiredText(taskSize, "Task size");
        this.submissionTime = submissionTime;

        switch (this.taskSize) {
            case "Small":      workloadUnits = 5;  break;
            case "Medium":     workloadUnits = 15; break;
            case "Large":      workloadUnits = 30; break;
            case "Very large": workloadUnits = 60; break;
            default: throw new IllegalArgumentException("Unknown task size: " + taskSize);
        }
    }

    private String requiredText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    // Only a queued job can start. GPUs are numbered 1 to 4.
    public void start(int gpuId, LocalDateTime time) {
        if (!status.equals("Queued")) {
            throw new IllegalStateException("Only a queued job can start.");
        }
        if (gpuId < 1 || gpuId > 4 || time == null || time.isBefore(submissionTime)) {
            throw new IllegalArgumentException("Check the GPU number and start time.");
        }
        assignedGpuId = gpuId;
        startTime = time;
        status = "Running";
    }

    // The scheduler calls this when the job has run for its full duration.
    public void complete(LocalDateTime time) {
        if (!status.equals("Running")) {
            throw new IllegalStateException("Only a running job can finish.");
        }
        if (!startTime.plusMinutes(getEstimatedDuration()).equals(time)) {
            throw new IllegalArgumentException("The finish time must match the job duration.");
        }
        finishTime = time;
        status = "Completed";
    }

    // Keep a record of who cancelled the job and why.
    public void cancel(String username, String reason, LocalDateTime time) {
        if (!status.equals("Queued") && !status.equals("Running")) {
            throw new IllegalStateException("This job has already ended.");
        }
        if (time == null || time.isBefore(submissionTime)) {
            throw new IllegalArgumentException("Check the cancellation time.");
        }
        if (status.equals("Running") && (time.isBefore(startTime)
                || !time.isBefore(startTime.plusMinutes(getEstimatedDuration())))) {
            throw new IllegalArgumentException("Cancellation must be during the run.");
        }
        String user = requiredText(username, "Username");
        String note = requiredText(reason, "Cancellation reason");
        if (status.equals("Running")) {
            finishTime = time;
        }
        cancelledBy = user;
        cancellationReason = note;
        cancellationTime = time;
        status = "Cancelled";
    }

    // Once the job starts, its actual queue wait is known.
    public Long getWaitBeforeStartMinutes() {
        if (startTime == null) {
            return null;
        }
        return Duration.between(submissionTime, startTime).toMinutes();
    }

    // Waiting is free. A finished or cancelled run is charged for time on the GPU.
    public int getChargedMinutes() {
        if (startTime == null || finishTime == null) {
            return 0;
        }
        return (int) Duration.between(startTime, finishTime).toMinutes();
    }

    // Each workload unit takes one simulated minute.
    public int getEstimatedDuration() {
        return workloadUnits;
    }

    public int getJobId() { return jobId; }
    public String getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getApplicationName() { return applicationName; }
    public String getDescription() { return description; }
    public String getTaskSize() { return taskSize; }
    public int getWorkloadUnits() { return workloadUnits; }
    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public String getStatus() { return status; }
    public Integer getAssignedGpuId() { return assignedGpuId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public LocalDateTime getCancellationTime() { return cancellationTime; }
    public String getCancelledBy() { return cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
}
