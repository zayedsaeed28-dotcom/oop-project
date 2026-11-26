import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
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
    private final JLabel clockLabel = new JLabel();
        private final JLabel busyCountLabel = new JLabel();
    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DashboardFrame(SimulationClock clock) {
        this(clock, new GPU[] {new GPU(1), new GPU(2), new GPU(3), new GPU(4)});
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
         // Count how many GPUs are running a job.
        int busyCount = 0;
        for (GPU gpu : gpus) {
            if (!gpu.isAvailable()) {
                busyCount++;
            }
        }
        busyCountLabel.setText(busyCount + " of " + gpus.length);
        datePicker.setEnabled(!started);
        timePicker.setEnabled(!started);
        startButton.setEnabled(!started);
        advanceButton.setEnabled(started);
        if (started) {
            clockLabel.setText(time.format(format));
        } else {
            clockLabel.setText("Choose a start time");
        }

        for (int i = 0; i < gpus.length; i++) {
            GPU gpu = gpus[i];
            Job job = gpu.getCurrentJob();
            if (job == null) {
                gpuDetails[i].setText("Free");
            } else {
                String details = "Busy | Job " + job.getJobId();
                if (started) {
                    details += " | " + gpu.getRemainingMinutes(time) + " min left";
                }
                details += " | Finish " + gpu.getExpectedFinishTime().format(format);
                gpuDetails[i].setText(details);
            }
        }
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
            for (GPU gpu : gpus) {
                gpu.finishIfReady(clock.getCurrentTime());
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
                window.setSize(Math.max(640, window.getWidth()), window.getHeight());
                window.setVisible(true);
            }
        });
    }
}
