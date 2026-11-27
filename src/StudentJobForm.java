import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.*;

// An approved student uses this form to submit a job to the shared scheduler.
public final class StudentJobForm {
    private final JPanel panel = new JPanel(new BorderLayout(6, 6));
    private final DashboardFrame dashboard;
    private final AccessRequest student;
    private final JTextField title = new JTextField(22);
    private final JTextField application = new JTextField(22);
    private final JTextArea description = new JTextArea(3, 22);
    private final JComboBox<String> size = new JComboBox<>(new String[] {
        "Small (5 min)", "Medium (15 min)", "Large (30 min)", "Very large (60 min)"
    });
    private final JLabel balance = new JLabel();
    private final JLabel message = new JLabel(" ");
    private final JButton submit = new JButton("Confirm and submit job");

    public StudentJobForm(RegistrationManager accounts, String id, String password,
                          DashboardFrame dashboard) {
        if (!accounts.canStudentLogIn(id, password)) {
            throw new IllegalArgumentException("Check your ID, password and account approval.");
        }
        if (dashboard.getScheduler() == null) {
            throw new IllegalStateException("Set the simulation start time first.");
        }
        this.student = accounts.findRequest(id);
        this.dashboard = dashboard;

        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
        fields.add(new JLabel("Student"));
        fields.add(new JLabel(student.getUniversityId()));
        fields.add(new JLabel("Job title"));
        fields.add(title);
        fields.add(new JLabel("Application name"));
        fields.add(application);
        fields.add(new JLabel("Description"));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        fields.add(new JScrollPane(description));
        fields.add(new JLabel("Task size and duration"));
        fields.add(size);
        panel.add(balance, BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(0, 1, 4, 4));
        bottom.add(submit);
        // Let the signed-in student save a copy of their own balance.
JButton saveBalance = new JButton("Save my GPU balance");
bottom.add(saveBalance);
saveBalance.addActionListener(event -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new java.io.File("gpu-balance.txt"));
    if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) {
        return;
    }

    java.io.File file = chooser.getSelectedFile();
    if (file.exists()) {
        int answer = JOptionPane.showConfirmDialog(panel,
                "Replace " + file.getName() + "?", "Save balance",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
    }

    try {
        student.getAllowance().saveSummary(file.toPath());
        message.setText("Balance saved to " + file.getName());
    } catch (java.io.IOException ex) {
        message.setText("Could not save the balance: " + ex.getMessage());
    }
});
        JButton cancelJob = new JButton("Cancel my job");
bottom.add(cancelJob);
cancelJob.addActionListener(event -> {
    Scheduler scheduler = dashboard.getScheduler();
    JComboBox<Integer> jobIds = new JComboBox<>();

    // Only list this student's queued or running jobs.
    for (Job job : scheduler.getJobs()) {
        boolean active = "Queued".equals(job.getStatus()) || "Running".equals(job.getStatus());
        if (student.getUniversityId().equals(job.getStudentId()) && active) {
            jobIds.addItem(job.getJobId());
        }
    }
    if (jobIds.getItemCount() == 0) {
        message.setText("You have no active jobs to cancel.");
        return;
    }

    JTextField reason = new JTextField();
    Object[] cancelFields = {"Choose your job ID", jobIds, "Reason for cancellation", reason};
    int answer = JOptionPane.showOptionDialog(panel, cancelFields, "Cancel a job",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
            new String[] {"Cancel selected job", "Keep job"}, "Keep job");
    if (answer != 0) {
        return;
    }

    try {
        int selectedId = (Integer) jobIds.getSelectedItem();
        scheduler.cancelJob(selectedId, student.getUniversityId(), reason.getText());
        message.setText("Job " + selectedId + " cancelled.");
        refreshBalance();
        dashboard.refreshDashboard();
    } catch (IllegalArgumentException | IllegalStateException ex) {
        message.setText(ex.getMessage());
    }
});
                JButton myJobs = new JButton("My jobs");
        bottom.add(myJobs);
        myJobs.addActionListener(event -> JOptionPane.showMessageDialog(
                panel, makeMyJobsPanel(), "My jobs", JOptionPane.PLAIN_MESSAGE));
        bottom.add(message);
        panel.add(bottom, BorderLayout.SOUTH);
        submit.addActionListener(event -> submitJob());
        refreshBalance();
    }

    public JPanel getPanel() {
        return panel;
    }

       private JPanel makeMyJobsPanel() {
        JTextField searchBox = new JTextField(18);
        JButton search = new JButton("Search");
        JPanel top = new JPanel();
        top.add(new JLabel("Job ID or title"));
        top.add(searchBox);
        top.add(search);

        JTextArea results = new JTextArea(15, 46);
        results.setEditable(false);
        results.setLineWrap(true);
        results.setWrapStyleWord(true);

        search.addActionListener(event -> {
            results.setText(findMyJobs(searchBox.getText()));
            results.setCaretPosition(0);
        });
        searchBox.addActionListener(event -> search.doClick());
        search.doClick(); // An empty search shows all my jobs.

        JPanel view = new JPanel(new BorderLayout(4, 4));
        view.add(top, BorderLayout.NORTH);
        view.add(new JScrollPane(results), BorderLayout.CENTER);
        return view;
    }

    private String findMyJobs(String search) {
        Scheduler scheduler = dashboard.getScheduler();
        ArrayList<Scheduler.QueuePrediction> predictions = scheduler.getQueuePredictions();
        DateTimeFormatter time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String query = search.trim().toLowerCase(Locale.ROOT);
        StringBuilder list = new StringBuilder();

        for (Job job : scheduler.getJobs()) {
            // Check the owner before showing anything about this job.
            if (!student.getUniversityId().equals(job.getStudentId())) continue;

            boolean idMatches = String.valueOf(job.getJobId()).equals(query);
            boolean titleMatches = job.getTitle().toLowerCase(Locale.ROOT).contains(query);
            if (!idMatches && !titleMatches) continue;

            list.append("Job " + job.getJobId() + " - " + job.getTitle() + "\n");
            list.append("Application: " + job.getApplicationName() + " | " + job.getStatus() + "\n");
            list.append("Submitted: " + job.getSubmissionTime().format(time) + "\n");

            if (job.getStartTime() != null) {
                list.append("GPU " + job.getAssignedGpuId() + " | Started: "
                        + job.getStartTime().format(time) + "\n");
                list.append("Wait before start: " + job.getWaitBeforeStartMinutes() + " min\n");
            }

            if ("Queued".equals(job.getStatus())) {
                // Use the scheduler's prediction since this job is still waiting.
                for (Scheduler.QueuePrediction p : predictions) {
                    if (p.jobId == job.getJobId()) {
                        list.append("Queue position: " + p.queuePosition + " | Wait left: "
                                + p.waitMinutes + " min\n");
                        list.append("Predicted GPU: " + p.predictedGpuId + "\n");
                        list.append("Predicted start: " + p.predictedStart.format(time) + "\n");
                        list.append("Predicted finish: " + p.predictedFinish.format(time) + "\n");
                        break;
                    }
                }
            } else if ("Running".equals(job.getStatus())) {
                list.append("Expected finish: " + job.getStartTime()
                        .plusMinutes(job.getEstimatedDuration()).format(time) + "\n");
            } else {
                if ("Cancelled".equals(job.getStatus())) {
                    list.append("Cancelled: " + job.getCancellationTime().format(time)
                            + " by " + job.getCancelledBy() + "\n");
                    list.append("Reason: " + job.getCancellationReason() + "\n");
                } else {
                    list.append("Finished: " + job.getFinishTime().format(time) + "\n");
                }
                list.append("Charged: " + job.getChargedMinutes() + " min\n");
            }
            list.append("\n");
        }
        return list.length() == 0 ? "No matching jobs." : list.toString();
    }
 private void refreshBalance() {
        TimeAllowance minutes = student.getAllowance();
        balance.setText("GPU minutes: allocated " + minutes.getAllocatedMinutes()
                + " | used " + minutes.getUsedMinutes() + " | reserved " + minutes.getReservedMinutes()
                + " | available " + minutes.getAvailableMinutes());
        submit.setEnabled(minutes.getAvailableMinutes() > 0);
    }

    private void submitJob() {
        Scheduler scheduler = dashboard.getScheduler();
        String choice = (String) size.getSelectedItem();
        // The scheduler needs just the size name, for example "Very large".
        String taskSize = choice.substring(0, choice.indexOf(" ("));
        try {
            Job job = scheduler.createJob(student.getUniversityId(), title.getText(),
                    application.getText(), description.getText(), taskSize);
            // Check the student's balance before sending the job to a GPU.
            student.getAllowance().reserve(job);
            scheduler.addAcceptedJob(job);
            message.setText("Job " + job.getJobId() + " accepted: " + job.getStatus()
                    + " (" + job.getEstimatedDuration() + " min).");
            title.setText("");
            application.setText("");
            description.setText("");
            dashboard.refreshDashboard();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            message.setText(ex.getMessage());
        }
        refreshBalance();
    }

    // Open the form with the same accounts and clock that the application uses.
    public static void open(RegistrationManager accounts, DashboardFrame dashboard) {
        JTextField id = new JTextField();
        JPasswordField password = new JPasswordField();
        JPanel login = new JPanel(new GridLayout(0, 2, 6, 6));
        login.add(new JLabel("University ID"));
        login.add(id);
        login.add(new JLabel("Password"));
        login.add(password);
                // New students can request an account from the login window.
        JButton register = new JButton("Request account");
        login.add(new JLabel("New student?"));
        login.add(register);

        register.addActionListener(event -> {
            JTextField newId = new JTextField(id.getText());
            JTextField name = new JTextField();
            JTextField email = new JTextField();
            JTextField phone = new JTextField();
            JPasswordField newPassword = new JPasswordField();

            Object[] fields = {
                "University ID", newId, "Name", name, "Email", email,
                "Phone", phone, "Choose a password", newPassword
            };

            // Keep the entered details if the student needs to fix a mistake.
            while (JOptionPane.showConfirmDialog(login, fields, "Request an account",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                try {
                    AccessRequest request = accounts.register(newId.getText(), name.getText(),
                            email.getText(), phone.getText(), new String(newPassword.getPassword()));
                    id.setText(request.getUniversityId());
                    password.setText("");
                    JOptionPane.showMessageDialog(login, "Request sent. Please wait for approval.");
                    break;
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    JOptionPane.showMessageDialog(login, ex.getMessage());
                }
            }
        });
        int answer = JOptionPane.showConfirmDialog(dashboard.getPanel(), login,
                "Student login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            try {
                StudentJobForm form = new StudentJobForm(accounts, id.getText(),
                        new String(password.getPassword()), dashboard);
                JOptionPane.showOptionDialog(dashboard.getPanel(), form.getPanel(), "Submit a GPU job",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                        new String[] {"Log out"}, "Log out");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                JOptionPane.showMessageDialog(dashboard.getPanel(), ex.getMessage());
            }
        }
    }

    // Run this file to try the form with one sample student and the real dashboard clock.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
                       // Load the saved students when the program starts.
            RegistrationManager accounts;
            try {
                java.nio.file.Path file = java.nio.file.Paths.get(
                        System.getProperty("user.home"), "ku-gpu-registrations.dat");
                accounts = RegistrationManager.load(file);

                if (accounts.findRequest("S01") == null) {
                    accounts.register("S01", "Demo Student", "s01@example.com",
                            "000", "demo").approve();
                }
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(null, "Could not load registrations: " + ex.getMessage());
                return;
            }

           DashboardFrame dashboard = DashboardFrame.resume(accounts.getSavedScheduler());
            JButton login = new JButton("Student login");
            login.addActionListener(event -> open(accounts, dashboard));

            JFrame window = new JFrame("Student job form demo");

            // Save the requests and approval decisions when the main window closes.
            window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent event) {
                    try {
                        accounts.save(dashboard.getScheduler());
                    } catch (java.io.IOException ex) {
                        int answer = JOptionPane.showConfirmDialog(window,
                                "Could not save registrations: " + ex.getMessage()
                                + "\nClose without saving your changes?", "Save error",
                                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                        if (answer != JOptionPane.YES_OPTION) return;
                    }
                    window.dispose();
                }
            }); 
                       java.awt.CardLayout pages = new java.awt.CardLayout();
            JPanel screens = new JPanel(pages);

            // The first screen only has the two login buttons.
            JButton adminLogin = new JButton("Admin login");
            JPanel loginButtons = new JPanel();
            loginButtons.add(login);
            loginButtons.add(adminLogin);
            JPanel home = new JPanel(new java.awt.GridBagLayout());
            home.add(loginButtons);

            // Keep the same dashboard when switching screens.
            JButton review = new JButton("Review account requests");
            JButton logout = new JButton("Log out");
            JPanel adminButtons = new JPanel();
            adminButtons.add(review);
            adminButtons.add(logout);

            JPanel adminPage = new JPanel(new BorderLayout());
            adminPage.add(new JScrollPane(dashboard.getPanel()), BorderLayout.CENTER);
            adminPage.add(adminButtons, BorderLayout.SOUTH);

            screens.add(home, "home");
            screens.add(adminPage, "admin");
            window.setContentPane(screens);
            pages.show(screens, "home");

            logout.addActionListener(event -> pages.show(screens, "home"));

            adminLogin.addActionListener(event -> {
                JTextField username = new JTextField();
                JPasswordField password = new JPasswordField();
                Object[] fields = {"Username", username, "Password", password};

                int answer = JOptionPane.showConfirmDialog(window, fields, "Admin login",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (answer != JOptionPane.OK_OPTION) return;

                if ("admin".equals(username.getText())
                        && "gpu2026".equals(new String(password.getPassword()))) {
                    dashboard.refreshDashboard();
                    pages.show(screens, "admin");
                } else {
                    JOptionPane.showMessageDialog(window, "Incorrect admin username or password.");
                }
            });

            review.addActionListener(event -> {

    // Refresh the list after each decision.
    while (true) {
        java.util.ArrayList<AccessRequest> pending = accounts.getPendingRequests();
        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(window, "No pending requests.");
            return;
        }

        JComboBox<String> choices = new JComboBox<>();
        for (AccessRequest request : pending) {
            choices.addItem(request.getUniversityId() + " | " + request.getName()
                    + " | " + request.getEmail() + " | " + request.getPhone());
        }

        int action = JOptionPane.showOptionDialog(window, choices, "Pending requests",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
               new String[] {"Approve", "Reject", "Back"}, "Back");
        if (action != 0 && action != 1) {
            return;
        }

        AccessRequest selected = pending.get(choices.getSelectedIndex());
        if (action == 0) {
            selected.approve();
        } else {
            selected.reject();
        }
    }
});
            window.setSize(800, 600);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
