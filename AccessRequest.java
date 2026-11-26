import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AccessRequest {
    private final String universityId;
    private final String name;
    private final String email;
    private final String phone;
    private final byte[] salt = new byte[16];
    private final byte[] passwordHash;
    private String status = "Pending";
    private TimeAllowance allowance;

    public AccessRequest(String universityId, String name, String email,
                         String phone, String password) {
        this.universityId = universityId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        new SecureRandom().nextBytes(salt);
        passwordHash = hashPassword(password);
    }

    // Save a password check so the file never needs the student's actual password.
    private byte[] hashPassword(String password) {
        PBEKeySpec key = new PBEKeySpec(password.toCharArray(), salt, 600000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(key).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Java could not check the password.", ex);
        } finally {
            key.clearPassword();
        }
    }

    public boolean matchesPassword(String input) {
        return input != null && MessageDigest.isEqual(passwordHash, hashPassword(input));
    }

    public String getUniversityId() { return universityId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
    public TimeAllowance getAllowance() { return allowance; }

    public boolean approve() {
        if (!"Pending".equals(status)) return false;
        allowance = new TimeAllowance(universityId);
        status = "Approved";
        return true;
    }

    public boolean reject() {
        if (!"Pending".equals(status)) return false;
        status = "Rejected";
        return true;
    }

    // Write the length first so spaces and line breaks are preserved.
    private static void writeText(DataOutputStream out, String value) throws IOException {
        out.writeInt(value.length());
        out.writeChars(value);
    }

    private static String readText(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > in.available() / 2) {
            throw new IOException("Invalid saved field.");
        }
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(in.readChar());
        }
        if (value.toString().trim().isEmpty()) {
            throw new IOException("Empty saved registration field.");
        }
        return value.toString();
    }

    // Read one student request in the same order it was saved.
    AccessRequest(DataInputStream in) throws IOException {
        universityId = readText(in);
        name = readText(in);
        email = readText(in);
        phone = readText(in);
        in.readFully(salt);
        passwordHash = new byte[32];
        in.readFully(passwordHash);
        status = in.readUTF();

        if ("Approved".equals(status)) {
            int allocated = in.readInt();
            if (allocated < 0) throw new IOException("Invalid saved allowance.");
            allowance = new TimeAllowance(universityId, allocated);
        } else if (!"Pending".equals(status) && !"Rejected".equals(status)) {
            throw new IOException("Unknown saved registration status.");
        }
    }

    void writeTo(DataOutputStream out) throws IOException {
        writeText(out, universityId);
        writeText(out, name);
        writeText(out, email);
        writeText(out, phone);
        out.write(salt);
        out.write(passwordHash);
        out.writeUTF(status);
        if ("Approved".equals(status)) {
            out.writeInt(allowance.getAllocatedMinutes());
        }
    }
}