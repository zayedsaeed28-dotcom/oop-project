import java.time.Duration;
import java.time.LocalDateTime;

// Each GPU can run one job at a time.
public class GPU {
    private final int gpuId;
    private Job currentJob;

    public GPU(int gpuId) {
        if (gpuId < 1 || gpuId > 4) {
            throw new IllegalArgumentException("GPU ID must be from 1 to 4.");
        }
        this.gpuId = gpuId;
    }

    public int getGpuId() {
        return gpuId;
    }

    // A job that has ended no longer occupies the GPU.
    public Job getCurrentJob() {
        if (currentJob != null && currentJob.getStatus().equals("Running")) {
            return currentJob;
        }
        return null;
    }

    public boolean isAvailable() {
        return getCurrentJob() == null;
    }

    // Start a queued job on this GPU using the simulation clock.
    public void startJob(Job job, LocalDateTime time) {
        if (!isAvailable()) {
            throw new IllegalStateException("This GPU is already busy.");
        }
        if (job == null) {
            throw new IllegalArgumentException("Choose a job to start.");
        }
        job.start(gpuId, time);
        currentJob = job;
    }

    // An idle GPU has no expected finish time.
    public LocalDateTime getExpectedFinishTime() {
        if (isAvailable()) {
            return null;
        }
        return currentJob.getStartTime().plusMinutes(currentJob.getEstimatedDuration());
    }

    public int getRemainingMinutes(LocalDateTime time) {
        if (isAvailable()) {
            return 0;
        }
        if (time == null || time.isBefore(currentJob.getStartTime())) {
            throw new IllegalArgumentException("Check the simulation time.");
        }
        LocalDateTime finish = getExpectedFinishTime();
        if (!time.isBefore(finish)) {
            return 0;
        }
        long minutesRun = Duration.between(currentJob.getStartTime(), time).toMinutes();
        return currentJob.getEstimatedDuration() - (int) minutesRun;
    }

    // After a clock tick, return the finished job so its runtime can be charged once.
    public Job finishIfReady(LocalDateTime time) {
        if (isAvailable() || getRemainingMinutes(time) > 0) {
            return null;
        }
        Job finishedJob = currentJob;
        finishedJob.complete(getExpectedFinishTime());
        currentJob = null;
        return finishedJob;
    }

    // Keep the job's cancellation details and free this GPU for the next job.
    public void cancelCurrentJob(String username, String reason, LocalDateTime time) {
        if (isAvailable()) {
            throw new IllegalStateException("This GPU has no running job.");
        }
        currentJob.cancel(username, reason, time);
        currentJob = null;
    }
}
