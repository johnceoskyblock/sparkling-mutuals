package net.johnceo.sparklingmutuals.update;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

import java.io.ByteArrayInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/** Shared staging and restart transaction. The entry point runs independently of Minecraft and Kotlin. */
public final class UpdateInstall {
    public static final long MAX_JAR_BYTES = 64 * 1024 * 1024;
    private static final int MAX_RECORD_BYTES = 16 * 1024;
    private static final int MAX_METADATA_BYTES = 64 * 1024;
    private static final String MOD_ID = "sparkling-mutuals";
    private static final String EDITION_KEY = "sparkling-mutuals:edition";
    private final Path mods;
    private final Path updates;
    private final Path current;
    private final String installedVersion;
    private final String minecraft;

    public record Artifact(String version, long size, String sha256) {
        public Artifact {
            if (!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")
                || size < 1 || size > MAX_JAR_BYTES || !sha256.matches("[a-f0-9]{64}")) {
                throw new IllegalArgumentException("Invalid Full artifact identity");
            }
        }
    }

    @FunctionalInterface
    public interface Download {
        void write(OutputStream output) throws IOException;
    }

    private record Fingerprint(long size, String sha256) {}
    private record Pending(String previousVersion, Fingerprint original, Artifact artifact, Path staged) {}
    public record Status(boolean failed, String message) {}

    public UpdateInstall(Path mods, Path updates, Path current, String installedVersion, String minecraft) throws IOException {
        this.mods = absolute(mods);
        this.updates = absolute(updates);
        this.current = absolute(current);
        this.installedVersion = installedVersion;
        this.minecraft = minecraft;
        require(this.mods.equals(this.current.getParent()) && this.current.getFileName().toString().endsWith(".jar"),
            "The loaded JAR is outside the instance mods directory");
        require(!this.updates.startsWith(this.mods) && !this.mods.startsWith(this.updates),
            "Update files must be outside mods");
        require(installedVersion.matches("[0-9A-Za-z.+-]{1,128}") && minecraft.matches("[0-9]+(?:\\.[0-9]+){1,2}"),
            "Unsupported installed version or Minecraft target");
        checkedDirectory(this.mods);
        checkedDirectory(this.updates.getParent());
        if (!Files.exists(this.updates, NOFOLLOW_LINKS)) {
            try {
                Files.createDirectory(this.updates);
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                // A second instance can prepare the same update directory.
            }
        }
        checkedDirectory(this.updates);
        require(Files.getFileStore(this.mods).equals(Files.getFileStore(this.updates)),
            "Mods and updates must share a filesystem");
    }

    public boolean autoUpdate() throws IOException {
        return "true".equals(readProperties(updates.resolve("preferences.properties")).getProperty("autoUpdate"));
    }

    public void saveAutoUpdate(boolean enabled) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("autoUpdate", Boolean.toString(enabled));
        writeProperties(updates.resolve("preferences.properties"), properties);
    }

    public Status status() throws IOException {
        Properties properties = readProperties(updates.resolve("status.properties"));
        String message = properties.getProperty("message");
        return message == null ? null : new Status("failed".equals(properties.getProperty("result")), message);
    }

    public boolean hasPending() {
        return Files.exists(updates.resolve("pending.properties"), NOFOLLOW_LINKS);
    }

    public String verifyPending() throws IOException {
        try (var channel = transactionChannel(); var lock = transactionLock(channel)) {
            if (!hasPending()) return null;
            Pending pending;
            boolean alreadyInstalled;
            try {
                pending = readPending();
                alreadyInstalled = matches(current, fingerprint(pending.artifact));
                if (alreadyInstalled) {
                    validateIdentity(current, pending.artifact.version, false);
                } else {
                    checkOriginal(pending);
                    validateArtifact(pending.staged, pending.artifact);
                }
            } catch (IOException invalid) {
                Files.move(updates.resolve("pending.properties"), updates.resolve("pending-invalid.properties"),
                    ATOMIC_MOVE, REPLACE_EXISTING);
                writeStatus(true, "Staged update is no longer valid. Download it again.");
                return null;
            }
            if (alreadyInstalled) {
                finish(pending);
                return null;
            }
            return pending.artifact.version;
        }
    }

    public void stage(Artifact artifact, Download download) throws IOException {
        try (var channel = transactionChannel(); var lock = transactionLock(channel)) {
            require(!hasPending(), "An update is already staged");
            validateIdentity(current, installedVersion, true);
            Fingerprint original = fingerprint(current);
            Path temporary = Files.createTempFile(updates, "download-", ".tmp");
            Path staged = stagedPath(artifact);
            boolean published = false;
            try {
                try (var output = new BoundedOutput(Files.newOutputStream(temporary), artifact.size)) {
                    download.write(output);
                }
                validateArtifact(temporary, artifact);
                require(matches(current, original), "The installed JAR changed during the download");
                if (Files.exists(staged, NOFOLLOW_LINKS)) checkedFile(staged);
                Files.move(temporary, staged, ATOMIC_MOVE, REPLACE_EXISTING);
                published = true;
                writePending(new Pending(installedVersion, original, artifact, staged));
            } finally {
                Files.deleteIfExists(temporary);
                if (published && !hasPending()) Files.deleteIfExists(staged);
            }
        }
    }

    void install() throws IOException {
        try (var channel = transactionChannel(); var lock = transactionLock(channel)) {
            var pending = readPending();
            if (matches(current, fingerprint(pending.artifact))) {
                validateIdentity(current, pending.artifact.version, false);
                finish(pending);
                return;
            }
            require(installedVersion.equals(pending.previousVersion), "The loaded JAR version changed");
            Path backup = updates.resolve("rollback-" + pending.original.sha256 + ".jar");
            if (Files.notExists(current, NOFOLLOW_LINKS) && matches(backup, pending.original)) {
                validateIdentity(backup, pending.previousVersion, false, isLegacy());
                copyAtomically(backup, current, pending.original);
            }
            checkOriginal(pending);
            validateArtifact(pending.staged, pending.artifact);
            if (Files.notExists(backup, NOFOLLOW_LINKS)) copyAtomically(current, backup, pending.original);
            require(matches(backup, pending.original), "Rollback JAR failed verification");

            Path replacement = Files.createTempFile(updates, "replace-", ".tmp");
            try {
                Files.copy(pending.staged, replacement, REPLACE_EXISTING);
                validateArtifact(replacement, pending.artifact);
                force(replacement);
                replaceWhenUnlocked(replacement, pending);
                finish(pending);
            } finally {
                Files.deleteIfExists(replacement);
            }
        }
    }

    private void replaceWhenUnlocked(Path replacement, Pending pending) throws IOException {
        for (int attempt = 0; ; attempt++) {
            checkOriginal(pending);
            try {
                Files.move(replacement, current, ATOMIC_MOVE, REPLACE_EXISTING);
                return;
            } catch (FileSystemException locked) {
                if (attempt >= 11) throw new IOException("Could not replace the installed JAR. The original was preserved", locked);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Installation interrupted", interrupted);
                }
            }
        }
    }

    private void checkOriginal(Pending pending) throws IOException {
        require(installedVersion.equals(pending.previousVersion) && matches(current, pending.original),
            "The installed JAR changed after the update was staged");
        validateIdentity(current, pending.previousVersion, true);
    }

    private void finish(Pending pending) throws IOException {
        writeStatus(false, "Installed version " + pending.artifact.version);
        Files.delete(updates.resolve("pending.properties"));
        Files.deleteIfExists(pending.staged);
    }

    private void writeStatus(boolean failed, String message) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("result", failed ? "failed" : "installed");
        properties.setProperty("message", message.substring(0, Math.min(message.length(), 512)));
        writeProperties(updates.resolve("status.properties"), properties);
    }

    private Pending readPending() throws IOException {
        Properties record = readProperties(updates.resolve("pending.properties"));
        try {
            var artifact = new Artifact(record.getProperty("version"), Long.parseLong(record.getProperty("size")),
                record.getProperty("sha256"));
            var original = new Fingerprint(Long.parseLong(record.getProperty("originalSize")), record.getProperty("originalSha256"));
            Path staged = stagedPath(artifact);
            String previousVersion = record.getProperty("previousVersion");
            require(current.toString().equals(record.getProperty("current")) && staged.toString().equals(record.getProperty("staged"))
                && minecraft.equals(record.getProperty("minecraft")) && "full".equals(record.getProperty("edition"))
                && previousVersion.matches("[0-9A-Za-z.+-]{1,128}") && original.size > 0 && original.size <= MAX_JAR_BYTES
                && original.sha256.matches("[a-f0-9]{64}"), "Invalid pending update record");
            return new Pending(previousVersion, original, artifact, staged);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new IOException("Invalid pending update record", invalid);
        }
    }

    private void writePending(Pending pending) throws IOException {
        Properties record = new Properties();
        record.setProperty("current", current.toString());
        record.setProperty("previousVersion", pending.previousVersion);
        record.setProperty("originalSize", Long.toString(pending.original.size));
        record.setProperty("originalSha256", pending.original.sha256);
        record.setProperty("staged", pending.staged.toString());
        record.setProperty("edition", "full");
        record.setProperty("minecraft", minecraft);
        record.setProperty("version", pending.artifact.version);
        record.setProperty("size", Long.toString(pending.artifact.size));
        record.setProperty("sha256", pending.artifact.sha256);
        force(pending.staged);
        writeProperties(updates.resolve("pending.properties"), record);
    }

    private Path stagedPath(Artifact artifact) {
        return updates.resolve("staged-" + artifact.sha256 + ".jar");
    }

    private FileChannel transactionChannel() throws IOException {
        checkedDirectory(mods);
        checkedDirectory(updates);
        Path file = updates.resolve("transaction.lock");
        if (Files.exists(file, NOFOLLOW_LINKS)) checkedFile(file);
        return FileChannel.open(file, CREATE, WRITE, NOFOLLOW_LINKS);
    }

    private static java.nio.channels.FileLock transactionLock(FileChannel channel) throws IOException {
        try {
            var lock = channel.tryLock();
            require(lock != null, "Another Sparkling Mutuals instance is working on this update");
            return lock;
        } catch (java.nio.channels.OverlappingFileLockException busy) {
            throw new IOException("Another Sparkling Mutuals update operation is already active", busy);
        }
    }

    private void validateArtifact(Path path, Artifact artifact) throws IOException {
        require(matches(path, fingerprint(artifact)), "The downloaded JAR did not match the published size and SHA-256");
        validateIdentity(path, artifact.version, false);
    }

    private boolean isLegacy() {
        return current.getFileName().toString().equals(MOD_ID + "-" + installedVersion.split("\\+", 2)[0] + "+mc" + minecraft + ".jar");
    }

    private void validateIdentity(Path path, String version, boolean allowLegacy) throws IOException {
        validateIdentity(path, version, allowLegacy && path.equals(current), false);
    }

    private void validateIdentity(Path path, String version, boolean allowLegacyCurrent, boolean allowLegacyBackup) throws IOException {
        checkedFile(path);
        try (var jar = new ZipFile(path.toFile())) {
            var metadata = jar.getEntry("fabric.mod.json");
            require(metadata != null && !metadata.isDirectory() && metadata.getSize() <= MAX_METADATA_BYTES
                && jar.stream().filter(entry -> entry.getName().equals("fabric.mod.json")).count() == 1,
                "The JAR must contain exactly one Fabric metadata file");
            try (var input = jar.getInputStream(metadata)) {
                byte[] data = input.readNBytes(MAX_METADATA_BYTES + 1);
                require(data.length <= MAX_METADATA_BYTES, "Fabric metadata is too large");
                var root = new GsonBuilder().setStrictness(Strictness.STRICT).create()
                    .fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);
                var custom = root.getAsJsonObject("custom");
                String edition = custom == null ? null : string(custom, EDITION_KEY);
                boolean legacy = (custom == null || !custom.has(EDITION_KEY))
                    && (allowLegacyBackup || allowLegacyCurrent && isLegacy());
                require(MOD_ID.equals(string(root, "id")) && version.equals(string(root, "version"))
                    && minecraft.equals(string(root.getAsJsonObject("depends"), "minecraft"))
                    && ("full".equals(edition) || legacy), "The JAR must match the Full mod, version and Minecraft target");
            }
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid Fabric metadata", invalid);
        }
    }

    private static String string(JsonObject object, String key) {
        var value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : null;
    }

    private static Fingerprint fingerprint(Artifact artifact) {
        return new Fingerprint(artifact.size, artifact.sha256);
    }

    private static boolean matches(Path path, Fingerprint expected) throws IOException {
        return Files.exists(path, NOFOLLOW_LINKS) && fingerprint(path).equals(expected);
    }

    private static Fingerprint fingerprint(Path path) throws IOException {
        checkedFile(path);
        long size = Files.size(path);
        require(size > 0 && size <= MAX_JAR_BYTES, "Unexpected JAR size");
        var digest = sha256();
        long read = 0;
        try (var input = Files.newInputStream(path, NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[16 * 1024];
            for (int count; (count = input.read(buffer)) != -1; ) {
                read += count;
                require(read <= size, "JAR changed while reading");
                digest.update(buffer, 0, count);
            }
        }
        require(read == size, "JAR changed while reading");
        return new Fingerprint(size, HexFormat.of().formatHex(digest.digest()));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }

    private void copyAtomically(Path source, Path target, Fingerprint expected) throws IOException {
        Path temporary = Files.createTempFile(updates, "copy-", ".tmp");
        try {
            Files.copy(source, temporary, REPLACE_EXISTING);
            require(matches(temporary, expected) && Files.notExists(target, NOFOLLOW_LINKS), "Could not preserve the original JAR");
            force(temporary);
            Files.move(temporary, target, ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path absolute(Path path) throws IOException {
        require(path.isAbsolute() && path.equals(path.normalize()), "Update paths must be absolute and normalized");
        return path;
    }

    private static void checkedDirectory(Path path) throws IOException {
        require(Files.isDirectory(path, NOFOLLOW_LINKS) && path.equals(path.toRealPath()),
            "Update directories cannot contain links");
    }

    private static void checkedFile(Path path) throws IOException {
        checkedDirectory(path.getParent());
        require(Files.isRegularFile(path, NOFOLLOW_LINKS) && path.equals(path.toRealPath()),
            "The update file must be a regular file without links");
    }

    private static void require(boolean condition, String message) throws IOException {
        if (!condition) throw new IOException(message);
    }

    private static Properties readProperties(Path path) throws IOException {
        Properties properties = new Properties();
        if (Files.notExists(path, NOFOLLOW_LINKS)) return properties;
        checkedFile(path);
        try (var input = Files.newInputStream(path, NOFOLLOW_LINKS)) {
            byte[] data = input.readNBytes(MAX_RECORD_BYTES + 1);
            require(data.length <= MAX_RECORD_BYTES, "The update record is too large");
            properties.load(new ByteArrayInputStream(data));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid update record", invalid);
        }
        return properties;
    }

    private static void writeProperties(Path path, Properties properties) throws IOException {
        checkedDirectory(path.getParent());
        if (Files.exists(path, NOFOLLOW_LINKS)) checkedFile(path);
        Path temporary = Files.createTempFile(path.getParent(), "record-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                properties.store(output, null);
            }
            require(Files.size(temporary) <= MAX_RECORD_BYTES, "The update record is too large");
            force(temporary);
            Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void force(Path path) throws IOException {
        try (var channel = FileChannel.open(path, WRITE, NOFOLLOW_LINKS)) {
            channel.force(true);
        }
    }

    private static final class BoundedOutput extends FilterOutputStream {
        private long remaining;

        BoundedOutput(OutputStream output, long maximum) {
            super(output);
            remaining = maximum;
        }

        @Override public void write(int byteValue) throws IOException {
            require(remaining > 0, "The download exceeds its published size");
            out.write(byteValue);
            remaining--;
        }

        @Override public void write(byte[] data, int start, int count) throws IOException {
            require(count <= remaining, "The download exceeds its published size");
            out.write(data, start, count);
            remaining -= count;
        }
    }

    public Process launch(ProcessHandle parent) throws IOException {
        var start = parent.info().startInstant().orElseThrow(() -> new IOException("Unknown game process start time"));
        checkedDirectory(updates);
        // Use a separate classpath so the helper itself cannot keep the mod JAR locked.
        Path helper = Files.createTempFile(updates, "installer-", ".jar");
        boolean started = false;
        try {
            try (var jar = new JarOutputStream(Files.newOutputStream(helper))) {
                addClass(jar, UpdateInstall.class);
            }
            Path gson = Path.of(com.google.gson.Gson.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            checkedFile(gson);
            boolean windows = System.getProperty("os.name").startsWith("Windows");
            Path executable = Path.of(System.getProperty("java.home"), "bin", windows ? "javaw.exe" : "java");
            var process = new ProcessBuilder(executable.toString(), "-cp", helper + java.io.File.pathSeparator + gson,
                UpdateInstall.class.getName(), Long.toString(parent.pid()), start.toString(), mods.toString(),
                updates.toString(), current.toString(), installedVersion, minecraft)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            started = true;
            return process;
        } catch (java.net.URISyntaxException invalid) {
            throw new IOException("Could not locate JSON support for the restart installer", invalid);
        } finally {
            if (!started) Files.deleteIfExists(helper);
        }
    }

    public void removeOldHelpers() throws IOException {
        checkedDirectory(updates);
        Instant cutoff = Instant.now().minusSeconds(24 * 60 * 60);
        try (var helpers = Files.newDirectoryStream(updates, "installer-*.jar")) {
            for (Path helper : helpers) {
                if (Files.isRegularFile(helper, NOFOLLOW_LINKS) && Files.getLastModifiedTime(helper, NOFOLLOW_LINKS).toInstant().isBefore(cutoff)) {
                    Files.deleteIfExists(helper);
                }
            }
        }
    }

    private static void addClass(JarOutputStream jar, Class<?> type) throws IOException {
        String resource = type.getName().replace('.', '/') + ".class";
        try (InputStream source = type.getResourceAsStream("/" + resource)) {
            require(source != null, "Could not load the restart installer class");
            jar.putNextEntry(new JarEntry(resource));
            source.transferTo(jar);
            jar.closeEntry();
        }
        for (Class<?> nested : type.getDeclaredClasses()) addClass(jar, nested);
    }

    public static void main(String[] args) {
        if (args.length != 7) System.exit(2);
        UpdateInstall installer = null;
        try {
            long pid = Long.parseLong(args[0]);
            Instant start = Instant.parse(args[1]);
            installer = new UpdateInstall(Path.of(args[2]), Path.of(args[3]), Path.of(args[4]), args[5], args[6]);
            var parent = ProcessHandle.of(pid);
            if (parent.isPresent() && parent.get().isAlive()) {
                var exact = parent.get();
                require(exact.info().startInstant().filter(start::equals).isPresent(), "Game process identity changed");
                exact.onExit().get(90, TimeUnit.SECONDS);
            }
            installer.install();
        } catch (Exception failed) {
            if (installer != null) {
                try {
                    installer.writeStatus(true, failed.getMessage() == null ? "Restart installation failed" : failed.getMessage());
                } catch (IOException ignored) {
                    // The pending record and rollback remain available after a status write failure.
                }
            }
            System.exit(1);
        }
    }
}
