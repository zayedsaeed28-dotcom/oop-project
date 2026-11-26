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

// Builds the clock controls shown in the dashboard window.
public final class DashboardFrame implements ActionListener {
    private final SimulationClock clock;
    private final JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
    private final JSpinner datePicker = new JSpinner(new SpinnerDateModel());
    private final JSpinner timePicker = new JSpinner(
            new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
    private final JButton startButton = new JButton("Set start time");
    private final JButton advanceButton = new JButton("Advance Minute");
    private final JLabel clockLabel = new JLabel();
    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DashboardFrame(SimulationClock clock) {
        this.clock = clock;
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
        startButton.addActionListener(this);
        advanceButton.addActionListener(this);
        refreshClock();
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
    private void refreshClock() {
        LocalDateTime time = clock.getCurrentTime();
        boolean started = time != null;
        datePicker.setEnabled(!started);
        timePicker.setEnabled(!started);
        startButton.setEnabled(!started);
        advanceButton.setEnabled(started);
        if (started) {
            clockLabel.setText(time.format(format));
        } else {
            clockLabel.setText("Choose a start time");
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
        }
        refreshClock();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame window = new JFrame("GPU dashboard");
                DashboardFrame dashboard = new DashboardFrame(new SimulationClock());
                window.setContentPane(dashboard.getPanel());
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.pack();
                window.setVisible(true);
            }
        });
    }
}
