package net.johnceo.sparklingmutuals.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class UpdateInstallTest {
    @TempDir Path instance;

    @Test void enablesAutoUpdateOnceAndHonorsSubsequentOptOut() throws Exception {
        var install = fixture().install;
        install.saveAutoUpdate(false);
        assertTrue(install.autoUpdate());
        install.saveAutoUpdate(false);
        assertFalse(install.autoUpdate());
    }

    @Test void interruptedOrOversizedStagingDoesNotPublishAnUpdate() throws Exception {
        var fixture = fixture();
        var next = jar("sparkling-mutuals", "full", "1.1.0", "26.3");
        var artifact = artifact(next);
        assertThrows(IOException.class, () -> fixture.install.stage(artifact, output -> {
            output.write(next, 0, 10);
            throw new IOException("Connection lost");
        }));
        assertThrows(IOException.class, () -> fixture.install.stage(artifact, output -> {
            output.write(next);
            output.write(42);
        }));
        assertFalse(fixture.install.hasPending());
        assertArrayEquals(fixture.old, Files.readAllBytes(fixture.installed));
        try (var files = Files.list(fixture.updates)) {
            assertTrue(files.noneMatch(file -> file.getFileName().toString().endsWith(".jar")
                || file.getFileName().toString().endsWith(".tmp")));
        }
        fixture.install.stage(artifact, output -> output.write(next));
        assertTrue(fixture.install.hasPending());
    }

    @Test void checksBothManifestBytesAndEmbeddedIdentityBeforePublishing() throws Exception {
        var fixture = fixture();
        for (byte[] wrong : List.of(
            jar("other-mod", "full", "1.1.0", "26.3"),
            jar("sparkling-mutuals", "noauto", "1.1.0", "26.3"),
            jar("sparkling-mutuals", "standard", "1.1.0", "26.3"),
            jar("sparkling-mutuals", null, "1.1.0", "26.3"),
            jar("sparkling-mutuals", "full", "1.2.0", "26.3"),
            jar("sparkling-mutuals", "full", "1.1.0", "26.2")
        )) {
            assertThrows(IOException.class, () -> fixture.install.stage(artifact(wrong), output -> output.write(wrong)));
            assertFalse(fixture.install.hasPending());
        }
        var next = jar("sparkling-mutuals", "full", "1.1.0", "26.3");
        var falseHash = new UpdateInstall.Artifact("1.1.0", next.length, "0".repeat(64));
        assertThrows(IOException.class, () -> fixture.install.stage(falseHash, output -> output.write(next)));
        assertArrayEquals(fixture.old, Files.readAllBytes(fixture.installed));
    }

    @Test void refusesChangedInstalledOrStagedFiles() throws Exception {
        var fixture = fixture();
        var next = jar("sparkling-mutuals", "full", "1.1.0", "26.3");
        fixture.install.stage(artifact(next), output -> output.write(next));
        var manuallyInstalled = jar("sparkling-mutuals", "full", "2.0.0", "26.3");
        Files.write(fixture.installed, manuallyInstalled);
        assertThrows(IOException.class, fixture.install::install);
        assertArrayEquals(manuallyInstalled, Files.readAllBytes(fixture.installed));

        Files.write(fixture.installed, fixture.old);
        try (var files = Files.list(fixture.updates)) {
            var staged = files.filter(file -> file.getFileName().toString().endsWith(".jar")).findFirst().orElseThrow();
            Files.write(staged, manuallyInstalled);
        }
        assertThrows(IOException.class, fixture.install::install);
        assertArrayEquals(fixture.old, Files.readAllBytes(fixture.installed));
    }

    @Test void atomicallyInstallsAtTheExistingPathAndKeepsTheOriginalOutsideMods() throws Exception {
        var fixture = fixture();
        var next = jar("sparkling-mutuals", "full", "1.1.0", "26.3");
        var otherMod = Files.writeString(fixture.installed.getParent().resolve("some-other-mod.jar"), "untouched");
        fixture.install.stage(artifact(next), output -> output.write(next));
        fixture.install.install();
        assertArrayEquals(next, Files.readAllBytes(fixture.installed));
        assertArrayEquals(fixture.old, Files.readAllBytes(fixture.updates.resolve("rollback-" + sha(fixture.old) + ".jar")));
        assertEquals("untouched", Files.readString(otherMod));
        assertFalse(fixture.install.hasPending());
    }

    @Test void discardsMissingStagingAndAcceptsAReplacementDownload() throws Exception {
        var fixture = fixture();
        var next = jar("sparkling-mutuals", "full", "1.1.0", "26.3");
        fixture.install.stage(artifact(next), output -> output.write(next));
        Files.delete(fixture.updates.resolve("staged-" + sha(next) + ".jar"));

        assertNull(fixture.install.verifyPending());
        assertFalse(fixture.install.hasPending());
        assertArrayEquals(fixture.old, Files.readAllBytes(fixture.installed));
        fixture.install.stage(artifact(next), output -> output.write(next));
        assertEquals("1.1.0", fixture.install.verifyPending());
    }

    @Test void discardsStagingAfterAManualUpgradeAndAllowsTheNextVersion() throws Exception {
        var fixture = fixture();
        var next = jar("sparkling-mutuals", "full", "1.1.0", "26.3");
        fixture.install.stage(artifact(next), output -> output.write(next));
        var manual = jar("sparkling-mutuals", "full", "1.0.1", "26.3");
        Path moved = fixture.installed.resolveSibling("sparkling-mutuals-1.0.1+mc26.3.jar");
        Files.delete(fixture.installed);
        Files.write(moved, manual);

        var upgraded = new UpdateInstall(moved.getParent(), fixture.updates, moved, "1.0.1", "26.3");
        assertNull(upgraded.verifyPending());
        assertTrue(upgraded.status().failed());
        assertArrayEquals(manual, Files.readAllBytes(moved));
        upgraded.stage(artifact(next), output -> output.write(next));
        assertEquals("1.1.0", upgraded.verifyPending());
    }

    @Test void refusesAnInstalledPathOutsideTheExactModsDirectory() throws Exception {
        var fixture = fixture();
        var outside = Files.write(instance.resolve(fixture.installed.getFileName()), fixture.old);
        assertThrows(IOException.class, () -> new UpdateInstall(fixture.installed.getParent(), fixture.updates, outside, "1.0.0", "26.3"));
        assertThrows(IOException.class, () -> new UpdateInstall(fixture.installed.getParent(),
            fixture.installed.getParent().resolve("updates"), fixture.installed, "1.0.0", "26.3"));
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test void detachedInstallerWaitsForTheExactParentAndItsWindowsJarLock() throws Exception {
        var fixture = fixture();
        var next = jar("sparkling-mutuals", "full", "1.1.0", "26.3");
        fixture.install.stage(artifact(next), output -> output.write(next));
        var parent = lock(fixture.installed);
        Process helper = null;
        try {
            assertThrows(IOException.class, () -> Files.move(fixture.installed, fixture.updates.resolve("windows-lock-probe")));
            helper = fixture.install.launch(parent.toHandle());
            assertFalse(helper.waitFor(2, TimeUnit.SECONDS));
            assertArrayEquals(fixture.old, Files.readAllBytes(fixture.installed));
            parent.getOutputStream().close();
            assertTrue(parent.waitFor(10, TimeUnit.SECONDS));
            assertTrue(helper.waitFor(20, TimeUnit.SECONDS));
            assertEquals(0, helper.exitValue());
            assertArrayEquals(next, Files.readAllBytes(fixture.installed));
        } finally {
            parent.destroyForcibly();
            if (helper != null) helper.destroyForcibly();
        }
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test void failedWindowsSwapKeepsBothTheOriginalAndTheVerifiedRollbackForRetry() throws Exception {
        var fixture = fixture();
        var next = jar("sparkling-mutuals", "full", "1.1.0", "26.3");
        fixture.install.stage(artifact(next), output -> output.write(next));
        var holder = lock(fixture.installed);
        try {
            assertThrows(IOException.class, fixture.install::install);
            assertArrayEquals(fixture.old, Files.readAllBytes(fixture.installed));
            assertArrayEquals(fixture.old, Files.readAllBytes(fixture.updates.resolve("rollback-" + sha(fixture.old) + ".jar")));
            assertTrue(fixture.install.hasPending());
        } finally {
            holder.getOutputStream().close();
            if (!holder.waitFor(10, TimeUnit.SECONDS)) holder.destroyForcibly();
        }
        fixture.install.install();
        assertArrayEquals(next, Files.readAllBytes(fixture.installed));
    }

    private Fixture fixture() throws Exception {
        // The test runner may place its temporary directory behind an OS path alias.
        var directory = instance.toRealPath();
        var mods = Files.createDirectory(directory.resolve("mods"));
        var updates = Files.createDirectories(directory.resolve("config").resolve("updates"));
        var old = jar("sparkling-mutuals", null, "1.0.0", "26.3");
        var installed = Files.write(mods.resolve("sparkling-mutuals-1.0.0+mc26.3.jar"), old);
        return new Fixture(new UpdateInstall(mods, updates, installed, "1.0.0", "26.3"), installed, updates, old);
    }

    private record Fixture(UpdateInstall install, Path installed, Path updates, byte[] old) {}

    private static byte[] jar(String id, String edition, String version, String minecraft) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var jar = new JarOutputStream(bytes)) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            var custom = edition == null ? "" : ",\"custom\":{\"sparkling-mutuals:edition\":\"" + edition + "\"}";
            var metadata = "{\"id\":\"" + id + "\",\"version\":\"" + version + "\",\"depends\":{\"minecraft\":\"" + minecraft + "\"}" + custom + "}";
            jar.write(metadata.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private static UpdateInstall.Artifact artifact(byte[] bytes) throws Exception {
        return new UpdateInstall.Artifact("1.1.0", bytes.length, sha(bytes));
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static Process lock(Path path) throws Exception {
        var command = """
            $file = [System.IO.File]::Open($env:SPARKLING_UPDATE_TEST_LOCK, 'Open', 'Read', 'ReadWrite')
            try { [Console]::WriteLine('locked'); [void][Console]::ReadLine() } finally { $file.Dispose() }
            """;
        var builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command);
        builder.redirectErrorStream(true);
        builder.environment().put("SPARKLING_UPDATE_TEST_LOCK", path.toString());
        var child = builder.start();
        try {
            var ready = CompletableFuture.supplyAsync(() -> {
                try {
                    return child.inputReader().readLine();
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                }
            });
            assertEquals("locked", ready.get(10, TimeUnit.SECONDS));
            return child;
        } catch (Exception | AssertionError failure) {
            child.destroyForcibly();
            throw failure;
        }
    }
}
