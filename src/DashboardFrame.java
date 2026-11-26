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
    private Scheduler scheduler;
    private boolean useScheduler;
    private final JTextArea waitingJobs = new JTextArea(3, 36);

    private final JLabel clockLabel = new JLabel();
    private final JLabel busyCountLabel = new JLabel();
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
        waitingJobs.setEditable(false);
        panel.add(new JLabel("Waiting jobs"));
        panel.add(new JScrollPane(waitingJobs));
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
