import java.time.LocalDateTime;
import java.util.ArrayList;

// Sends accepted jobs to the four GPUs in first come, first served order.
public class Scheduler {
    private final GPU[] gpus = {new GPU(1), new GPU(2), new GPU(3), new GPU(4)};
    private final ArrayList<Job> jobs = new ArrayList<>();
    private int nextJobId = 1;
    private LocalDateTime lastMinute;

    // Start at the same minute the user selected on the simulation clock.
    public Scheduler(LocalDateTime startTime) {
        if (startTime == null || startTime.getSecond() != 0 || startTime.getNano() != 0) {
            throw new IllegalArgumentException("Choose a start time in whole minutes.");
        }
        lastMinute = startTime;
    }

    // Give the job an ID and timestamp before its minutes are reserved.
    public Job createJob(String studentId, String title, String applicationName,
                         String description, String taskSize) {
        Job job = new Job(nextJobId, studentId, title, applicationName,
                description, taskSize, lastMinute);
        nextJobId++;
        return job;
    }

    // Call this after the student's allowance has been checked and reserved.
    public void addAcceptedJob(Job job) {
        if (job == null || !job.getStatus().equals("Queued")
                || !lastMinute.equals(job.getSubmissionTime())) {
            throw new IllegalArgumentException("Choose a new job from the current minute.");
        }
        for (Job existing : jobs) {
            if (existing.getJobId() == job.getJobId()) {
                throw new IllegalArgumentException("This job ID is already in use.");
            }
        }
        jobs.add(job);
        startWaitingJobs();
    }

    // Finish all jobs due this minute before assigning the free GPUs.
    public void advanceMinute(LocalDateTime time) {
        if (!lastMinute.plusMinutes(1).equals(time)) {
            throw new IllegalArgumentException("Advance the clock by one minute.");
        }
        for (GPU gpu : gpus) {
            gpu.finishIfReady(time);
        }
        lastMinute = time;
        startWaitingJobs();
    }

    // Check GPUs in ID order so the lowest free ID gets the next job.
    private void startWaitingJobs() {
        for (GPU gpu : gpus) {
            if (gpu.isAvailable()) {
                Job next = firstWaitingJob();
                if (next == null) {
                    return;
                }
                gpu.startJob(next, lastMinute);
            }
        }
    }

    // An earlier submission goes first. Use the lower job ID to break a tie.
    private Job firstWaitingJob() {
        Job first = null;
        for (Job job : jobs) {
            if (job.getStatus().equals("Queued")) {
                if (first == null || job.getSubmissionTime().isBefore(first.getSubmissionTime())
                        || (job.getSubmissionTime().equals(first.getSubmissionTime())
                            && job.getJobId() < first.getJobId())) {
                    first = job;
                }
            }
        }
        return first;
    }

    // The dashboard can read the same GPU objects and job history.
    public GPU[] getGpus() {
        return gpus.clone();
    }

    public ArrayList<Job> getJobs() {
        return new ArrayList<Job>(jobs);
    }
}
