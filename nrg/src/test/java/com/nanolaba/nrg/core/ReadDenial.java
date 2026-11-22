package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Test helper that takes away the current user's permission to read a file or to list a
 * directory, so that tests can reach the code that handles an {@link IOException} on reading.
 *
 * <p>POSIX file systems get every permission bit cleared; ACL file systems (Windows) get a DENY
 * entry for reading data, which for a directory is listing it. Where that does not bind (no such
 * attribute view, or a user permissions do not apply to, such as root) the calling test is
 * skipped. {@link #close()} restores the original permissions — {@code @TempDir} cannot delete
 * the entry otherwise.
 */
final class ReadDenial implements AutoCloseable {

    private interface Restore {
        void run() throws IOException;
    }

    private final Restore restore;

    private ReadDenial(Restore restore) {
        this.restore = restore;
    }

    static ReadDenial of(Path path) throws IOException {
        ReadDenial denial = new ReadDenial(deny(path));
        if (isReadable(path)) {
            denial.close();
            Assumptions.assumeTrue(false, "permissions do not keep this user from reading " + path);
        }
        return denial;
    }

    private static Restore deny(Path path) throws IOException {
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null) {
            Set<PosixFilePermission> original = posix.readAttributes().permissions();
            posix.setPermissions(EnumSet.noneOf(PosixFilePermission.class));
            return () -> posix.setPermissions(original);
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        Assumptions.assumeTrue(acl != null, "no way to restrict reading on this file system");
        List<AclEntry> original = acl.getAcl();
        UserPrincipal me = path.getFileSystem().getUserPrincipalLookupService()
                .lookupPrincipalByName(System.getProperty("user.name"));
        List<AclEntry> restricted = new ArrayList<>();
        restricted.add(AclEntry.newBuilder().setType(AclEntryType.DENY).setPrincipal(me)
                .setPermissions(AclEntryPermission.READ_DATA).build());
        restricted.addAll(original);
        acl.setAcl(restricted);
        return () -> acl.setAcl(original);
    }

    private static boolean isReadable(Path path) throws IOException {
        try {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> ignored = Files.newDirectoryStream(path)) {
                    return true;
                }
            }
            try (InputStream ignored = Files.newInputStream(path)) {
                return true;
            }
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        restore.run();
    }
}
