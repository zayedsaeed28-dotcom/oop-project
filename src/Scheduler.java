import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;

// Sends accepted jobs to the four GPUs in first come, first served order.
	public class Scheduler implements java.io.Serializable {
            private static final long serialVersionUID = 1L;
    private final GPU[] gpus = {new GPU(1), new GPU(2), new GPU(3), new GPU(4)};
    private final ArrayList<Job> jobs = new ArrayList<>();
    private int nextJobId = 1;
    private final LocalDateTime simulationStart;
    private LocalDateTime lastMinute;

    // Start at the same minute the user selected on the simulation clock.
    public Scheduler(LocalDateTime startTime) {
        if (startTime == null || startTime.getSecond() != 0 || startTime.getNano() != 0) {
            throw new IllegalArgumentException("Choose a start time in whole minutes.");
        }
        simulationStart = startTime;
        lastMinute = startTime;
    }
        public LocalDateTime getStartTime() {
        return simulationStart;
    }

    public LocalDateTime getCurrentTime() {
        return lastMinute;
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

    // The caller supplies the username of the signed-in student or administrator.
    public void cancelJob(int jobId, String username, String reason) {
        for (Job job : jobs) {
            if (job.getJobId() == jobId) {
                if (!job.getStudentId().equals(username) && !"admin".equals(username)) {
                    throw new IllegalArgumentException("You can only cancel your own jobs.");
                }
                job.cancel(username, reason, lastMinute);
                startWaitingJobs();
                return;
            }
        }
        throw new IllegalArgumentException("Job ID was not found.");
    }

    // Both the real schedule and the predictions use the same queue order.
    private ArrayList<Job> getWaitingJobsInOrder() {
        ArrayList<Job> waiting = new ArrayList<>();
        for (Job job : jobs) {
            if (job.getStatus().equals("Queued")) {
                waiting.add(job);
            }
        }
        waiting.sort(new Comparator<Job>() {
            public int compare(Job first, Job second) {
                int order = first.getSubmissionTime().compareTo(second.getSubmissionTime());
                if (order == 0) {
                    return Integer.compare(first.getJobId(), second.getJobId());
                }
                return order;
            }
        });
        return waiting;
    }

    // Assign the first waiting job to the lowest-numbered free GPU.
    private void startWaitingJobs() {
        ArrayList<Job> waiting = getWaitingJobsInOrder();
        int next = 0;
        for (GPU gpu : gpus) {
            if (next == waiting.size()) {
                break;
            }
            if (gpu.isAvailable()) {
                gpu.startJob(waiting.get(next), lastMinute);
                next++;
            }
        }
    }

    // Each result describes one queued job at the current simulation minute.
    public static class QueuePrediction {
        public final int jobId;
        public final int queuePosition;
        public final int predictedGpuId;
        public final LocalDateTime predictedStart;
        public final LocalDateTime predictedFinish;
        public final long waitMinutes;

        private QueuePrediction(Job job, int position, int gpuId,
                                LocalDateTime start, LocalDateTime now) {
            jobId = job.getJobId();
            queuePosition = position;
            predictedGpuId = gpuId;
            predictedStart = start;
            predictedFinish = start.plusMinutes(job.getEstimatedDuration());
            waitMinutes = Duration.between(now, start).toMinutes();
        }
    }

    public ArrayList<QueuePrediction> getQueuePredictions() {
        // Work with a copy of each GPU's next free time.
        LocalDateTime[] freeAt = new LocalDateTime[gpus.length];
        for (int i = 0; i < gpus.length; i++) {
            freeAt[i] = gpus[i].getExpectedFinishTime();
            if (freeAt[i] == null) {
                freeAt[i] = lastMinute;
            }
        }

        ArrayList<QueuePrediction> predictions = new ArrayList<>();
        for (Job job : getWaitingJobsInOrder()) {
            // On a tie, keep the lower GPU ID.
            int earliest = 0;
            for (int i = 1; i < freeAt.length; i++) {
                if (freeAt[i].isBefore(freeAt[earliest])) {
                    earliest = i;
                }
            }
            QueuePrediction prediction = new QueuePrediction(job, predictions.size() + 1,
                    gpus[earliest].getGpuId(), freeAt[earliest], lastMinute);
            predictions.add(prediction);
            // The next job in line has to include this job's running time.
            freeAt[earliest] = prediction.predictedFinish;
        }
        return predictions;
    }

    // At the start, no time has passed. The dashboard can show N/A for null.
    public Double getUtilizationPercent() {
        long elapsedMinutes = Duration.between(simulationStart, lastMinute).toMinutes();
        if (elapsedMinutes == 0) {
            return null;
        }

        long busyMinutes = 0;
        for (Job job : jobs) {
            // A job that never started used zero GPU minutes.
            LocalDateTime start = job.getStartTime();
            if (start != null) {
                LocalDateTime end = job.getFinishTime();
                if (end == null) {
                    end = lastMinute; // This job is still running.
                }
                busyMinutes += Duration.between(start, end).toMinutes();
            }
        }

        long totalGpuMinutes = elapsedMinutes * gpus.length;
        return 100.0 * busyMinutes / totalGpuMinutes;
    }

    // The dashboard can read the same GPU objects and job history.
    public GPU[] getGpus() {
        return gpus.clone();
    }

    public ArrayList<Job> getJobs() {
        return new ArrayList<Job>(jobs);
    }
}
