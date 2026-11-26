import java.util.ArrayList;

// Keeps the student registration requests together.
public class RegistrationManager {
    private final ArrayList<AccessRequest> requests = new ArrayList<>();

    // Find the most recent request for this university ID.
    public AccessRequest findRequest(String universityId) {
        if (universityId == null) {
            return null;
        }
        for (int i = requests.size() - 1; i >= 0; i--) {
            AccessRequest request = requests.get(i);
            if (request.getUniversityId().equals(universityId.trim())) {
                return request;
            }
        }
        return null;
    }

    // A rejected student can apply again. Keep the old request in the list.
    public AccessRequest register(String universityId, String name, String email,
                                  String phone, String password) {
        if (isBlank(universityId) || isBlank(name) || isBlank(email)
                || isBlank(phone) || isBlank(password)) {
            throw new IllegalArgumentException("Please fill in all five fields.");
        }

        AccessRequest previous = findRequest(universityId);
        if (previous != null && !previous.getStatus().equals("Rejected")) {
            throw new IllegalStateException("This ID already has a pending or approved request.");
        }

        AccessRequest request = new AccessRequest(universityId.trim(), name.trim(),
                email.trim(), phone.trim(), password);
        requests.add(request);
        return request;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
