public class AccessRequest {
    private final String universityId;
    private final String name;
    private final String email;
    private final String phone;
    private final String password;
    private String status;
    private TimeAllowance allowance;

    // A new request waits for the administrator to review it.
    public AccessRequest(String universityId, String name, String email,
                         String phone, String password) {
        this.universityId = universityId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.status = "Pending";
    }

    public String getUniversityId() { return universityId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }

    public boolean matchesPassword(String input) {
        // The student gets an allowance only after approval.
public TimeAllowance getAllowance() {
    return allowance;
}
        return input != null && input.equals(password);
    }

    // Return false if this request was already reviewed.
    public boolean approve() {
        if (!status.equals("Pending")) {
            return false;
        }allowance = new TimeAllowance(universityId);
        status = "Approved";
        return true;
    }

    public boolean reject() {
        if (!status.equals("Pending")) {
            return false;
        }
        status = "Rejected";
        return true;
    }
}
