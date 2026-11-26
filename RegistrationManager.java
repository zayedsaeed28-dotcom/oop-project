import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;

public class RegistrationManager {
    private final ArrayList<AccessRequest> requests = new ArrayList<>();
    private static final String FORMAT = "KU registrations 1";
    private Path file;
    private byte[] lastSave;

    // Find the most recent request for this university ID.
    public AccessRequest findRequest(String universityId) {
        if (universityId == null) return null;
        for (int i = requests.size() - 1; i >= 0; i--) {
            AccessRequest request = requests.get(i);
            if (request.getUniversityId().equals(universityId.trim())) {
                return request;
            }
        }
        return null;
    }

    public AccessRequest register(String universityId, String name, String email,
                                  String phone, String password) {
        if (isBlank(universityId) || isBlank(name) || isBlank(email)
                || isBlank(phone) || isBlank(password)) {
            throw new IllegalArgumentException("Please fill in all five fields.");
        }

        // A rejected student can apply again.
        AccessRequest previous = findRequest(universityId);
        if (previous != null && !"Rejected".equals(previous.getStatus())) {
            throw new IllegalStateException("This ID already has a pending or approved request.");
        }

        AccessRequest request = new AccessRequest(universityId.trim(), name.trim(),
                email.trim(), phone.trim(), password);
        requests.add(request);
        return request;
    }

    public boolean canStudentLogIn(String universityId, String password) {
        AccessRequest request = findRequest(universityId);
        if (request == null || !"Approved".equals(request.getStatus())) {
            return false;
        }
        return request.matchesPassword(password);
    }

    public ArrayList<AccessRequest> getPendingRequests() {
        ArrayList<AccessRequest> pending = new ArrayList<>();
        for (AccessRequest request : requests) {
            if ("Pending".equals(request.getStatus())) {
                pending.add(request);
            }
        }
        return pending;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static byte[] readFile(Path file) throws IOException {
        try {
            return Files.readAllBytes(file);
        } catch (NoSuchFileException ex) {
            return null; // The first time the program runs, there is no save yet.
        }
    }

    public static RegistrationManager load(Path path) throws IOException {
        RegistrationManager manager = new RegistrationManager();
        manager.file = path.toAbsolutePath();
        manager.lastSave = readFile(manager.file);
        if (manager.lastSave == null) return manager;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(manager.lastSave))) {
            if (!FORMAT.equals(in.readUTF())) {
                throw new IOException("Unknown registration file format.");
            }
            int count = in.readInt();
            if (count < 0) throw new IOException("Invalid registration count.");

            // Load in order so that the latest request for each ID stays the latest.
            for (int i = 0; i < count; i++) {
                AccessRequest request = new AccessRequest(in);
                AccessRequest earlier = manager.findRequest(request.getUniversityId());
                if (earlier != null && !"Rejected".equals(earlier.getStatus())) {
                    throw new IOException("Duplicate student ID in the registration file.");
                }
                manager.requests.add(request);
            }
            if (in.read() != -1) {
                throw new IOException("Unexpected data after the last request.");
            }
        } catch (EOFException ex) {
            throw new IOException("The registration file is incomplete.", ex);
        }
        return manager;
    }

    public void save() throws IOException {
        if (file == null) throw new IOException("Open a registration file before saving.");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeUTF(FORMAT);
            out.writeInt(requests.size());
            for (AccessRequest request : requests) {
                request.writeTo(out);
            }
        }
        byte[] nextSave = buffer.toByteArray();
        if (Arrays.equals(nextSave, lastSave)) return;

        // Protect changes if two copies of the program are open.
        Path lockPath = file.resolveSibling(file.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE); FileLock lock = channel.lock()) {
            if (!lock.isValid() || !Arrays.equals(lastSave, readFile(file))) {
                throw new IOException("Another window changed the registration file.");
            }

            // Finish the new file before replacing the previous save.
            Path temp = Files.createTempFile(file.getParent(), "requests-", ".tmp");
            try {
                Files.write(temp, nextSave);
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                lastSave = nextSave;
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (java.nio.channels.OverlappingFileLockException ex) {
            throw new IOException("Another window is saving registrations. Please try again.", ex);
        }
    }
}