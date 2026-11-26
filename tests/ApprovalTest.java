public class ApprovalTest {
    public static void main(String[] args) {
        RegistrationManager manager = new RegistrationManager();
        AccessRequest student = manager.register(
                "S02", "Test Student", "s02@example.com", "000", "demo");

        // First approval gives the student 100 GPU minutes.
        if (!student.approve() || student.getAllowance().getAvailableMinutes() != 100) {
            throw new AssertionError("First approval should give 100 minutes.");
        }

        // A second approval must keep the 15 extra minutes.
        student.getAllowance().grantExtraMinutes(15, "Extra lab time");
        if (student.approve()) {
            throw new AssertionError("The same request was approved twice.");
        }
        if (student.getAllowance().getAvailableMinutes() != 115) {
            throw new AssertionError("The balance should still be 115 minutes.");
        }

        System.out.println("Passed: repeat approval kept the balance at 115 minutes.");
    }
}
