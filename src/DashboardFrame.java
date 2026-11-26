import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.swing.*;

// Shows the simulation clock and the four GPUs on the admin dashboard.
public final class DashboardFrame implements ActionListener {
    private final SimulationClock clock;
    private final GPU[] gpus = new GPU[4];
    private final JLabel[] gpuDetails = new JLabel[4];
    private final JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
    private final JSpinner datePicker = new JSpinner(new SpinnerDateModel());
    private final JSpinner timePicker = new JSpinner(
            new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
    private final JButton startButton = new JButton("Set start time");
    private final JButton advanceButton = new JButton("Advance Minute");
    private Scheduler scheduler;
    private boolean useScheduler;
    private final JTextArea waitingJobs = new JTextArea(3, 36);
    private final JTextArea completedJobs = new JTextArea(5, 36);

    private final JLabel clockLabel = new JLabel();
    private final JLabel busyCountLabel = new JLabel();
    private final JLabel utilizationLabel = new JLabel();
    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DashboardFrame(SimulationClock clock) {
        this(clock, new GPU[] {new GPU(1), new GPU(2), new GPU(3), new GPU(4)});
        useScheduler = true;
        refreshDashboard();
    }

    // Use the same GPU objects as the simulation.
    public DashboardFrame(SimulationClock clock, GPU[] devices) {
        this.clock = clock;
        if (devices == null || devices.length != 4) {
            throw new IllegalArgumentException("The dashboard needs four GPUs.");
        }
        for (GPU gpu : devices) {
            if (gpu == null || gpus[gpu.getGpuId() - 1] != null) {
                throw new IllegalArgumentException("Use GPU IDs 1 to 4 once each.");
            }
            gpus[gpu.getGpuId() - 1] = gpu;
        }
        setPickerFormat(datePicker, "dd/MM/yyyy");
        setPickerFormat(timePicker, "HH:mm");
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel("Start date"));
        panel.add(datePicker);
        panel.add(new JLabel("Start time"));
        panel.add(timePicker);
        panel.add(startButton);
        panel.add(advanceButton);
        panel.add(new JLabel("Simulation time"));
        panel.add(clockLabel);
        // Keep one row per GPU, in GPU number order.
        for (int i = 0; i < gpus.length; i++) {
            panel.add(new JLabel("GPU " + gpus[i].getGpuId()));
            gpuDetails[i] = new JLabel();
            panel.add(gpuDetails[i]);
        }
        panel.add(new JLabel("Busy GPUs"));
        panel.add(busyCountLabel);
        panel.add(new JLabel("GPU utilization"));
        panel.add(utilizationLabel);
        waitingJobs.setEditable(false);
        panel.add(new JLabel("Waiting jobs"));
        panel.add(new JScrollPane(waitingJobs));
        completedJobs.setEditable(false);
        panel.add(new JLabel("Completed jobs"));
        panel.add(new JScrollPane(completedJobs));
        startButton.addActionListener(this);
        advanceButton.addActionListener(this);
        refreshDashboard();
    }

    private void setPickerFormat(JSpinner picker, String pattern) {
        JSpinner.DateEditor editor = new JSpinner.DateEditor(picker, pattern);
        editor.getFormat().setLenient(false);
        picker.setEditor(editor);
    }

    public JPanel getPanel() {
        return panel;
    }

    // Lock both pickers after the simulation starts.
    public void refreshDashboard() {
        LocalDateTime time = clock.getCurrentTime();
        boolean started = time != null;
        if (useScheduler && started && scheduler == null) {
            scheduler = new Scheduler(time);
            GPU[] devices = scheduler.getGpus();
            for (int i = 0; i < devices.length; i++) {
                gpus[i] = devices[i];
            }
        }
        datePicker.setEnabled(!started);
        timePicker.setEnabled(!started);
        startButton.setEnabled(!started);
        advanceButton.setEnabled(started);
        if (started) {
            clockLabel.setText(time.format(format));
        } else {
            clockLabel.setText("Choose a start time");
        }

        int busyCount = 0;
        for (int i = 0; i < gpus.length; i++) {
            GPU gpu = gpus[i];
            Job job = gpu.getCurrentJob();
            if (job == null) {
                gpuDetails[i].setText("Free");
            } else {
                busyCount++;
                String details = "Busy | Job " + job.getJobId();
                if (started) {
                    details += " | " + gpu.getRemainingMinutes(time) + " min left";
                }
                details += " | Finish " + gpu.getExpectedFinishTime().format(format);
                gpuDetails[i].setText(details);
            }
        }
        busyCountLabel.setText(busyCount + " of " + gpus.length);

        // The scheduler counts busy minutes since the simulation started.
        Double percent = null;
        if (scheduler != null) {
            percent = scheduler.getUtilizationPercent();
        }
        if (percent == null) {
            utilizationLabel.setText("N/A");
        } else {
            utilizationLabel.setText(String.format(Locale.ROOT, "%.1f%%", percent));
        }
        refreshWaitingJobs();
        refreshCompletedJobs();
    }

    // Other screens can use this scheduler once the start time is set.
    public Scheduler getScheduler() {
        return scheduler;
    }

    private void refreshWaitingJobs() {
        if (scheduler == null) {
            waitingJobs.setText(useScheduler ? "Set the start time first." : "Queue unavailable.");
            return;
        }
        StringBuilder lines = new StringBuilder();
        for (Scheduler.QueuePrediction job : scheduler.getQueuePredictions()) {
            lines.append("#").append(job.queuePosition).append("  Job ").append(job.jobId)
                    .append("  GPU ").append(job.predictedGpuId)
                    .append("  Wait ").append(job.waitMinutes).append(" min")
                    .append("  Start ").append(job.predictedStart.format(format))
                    .append("  Finish ").append(job.predictedFinish.format(format)).append("\n");
        }
        waitingJobs.setText(lines.length() == 0 ? "No waiting jobs." : lines.toString());
        waitingJobs.setCaretPosition(0);
    }

    // Show the final times recorded by the scheduler for each completed job.
    private void refreshCompletedJobs() {
        if (scheduler == null) {
            completedJobs.setText(useScheduler ? "Set the start time first." : "Job history unavailable.");
            return;
        }

        StringBuilder lines = new StringBuilder();
        for (Job job : scheduler.getJobs()) {
            if ("Completed".equals(job.getStatus())) {
                int runtime = job.getChargedMinutes(); // The whole run is charged on completion.
                lines.append("Job ").append(job.getJobId()).append(" | GPU ").append(job.getAssignedGpuId())
                        .append(" | Wait ").append(job.getWaitBeforeStartMinutes()).append(" min")
                        .append(" | Runtime ").append(runtime).append(" min")
                        .append(" | Charged ").append(runtime).append(" min\n");
                lines.append("Submitted ").append(job.getSubmissionTime().format(format))
                        .append(" | Started ").append(job.getStartTime().format(format))
                        .append(" | Finished ").append(job.getFinishTime().format(format)).append("\n\n");
            }
        }
        completedJobs.setText(lines.length() == 0 ? "No completed jobs." : lines.toString());
        completedJobs.setCaretPosition(0);
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == startButton) {
            try {
                // Read typed changes as well as values selected with the arrows.
                datePicker.commitEdit();
                timePicker.commitEdit();
                ZoneId zone = ZoneId.systemDefault();
                Date date = (Date) datePicker.getValue();
                Date time = (Date) timePicker.getValue();
                clock.start(LocalDateTime.of(date.toInstant().atZone(zone).toLocalDate(),
                        time.toInstant().atZone(zone).toLocalTime()));
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(panel, "Use dd/MM/yyyy for the date and HH:mm for the time.");
                return;
            }
        } else if (event.getSource() == advanceButton) {
            clock.advanceMinute();
            if (scheduler != null) {
                scheduler.advanceMinute(clock.getCurrentTime());
            } else {
                for (GPU gpu : gpus) {
                    gpu.finishIfReady(clock.getCurrentTime());
                }
            }
        }
        refreshDashboard();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame window = new JFrame("GPU dashboard");
                DashboardFrame dashboard = new DashboardFrame(new SimulationClock());
                window.setContentPane(dashboard.getPanel());
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.pack();
                window.setSize(Math.max(840, window.getWidth()), window.getHeight());
                window.setVisible(true);
            }
        });
    }
}
