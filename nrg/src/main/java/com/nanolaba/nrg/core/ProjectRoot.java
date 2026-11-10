package com.nanolaba.nrg.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * The directory a template is confined to: every path a template can name — files it
 * imports or lists, descriptors it reads, output files it asks for — must stay inside it.
 *
 * <p>A template is data that may come from someone else (a pull request, a cloned
 * repository, a remote import), yet {@code ../../} in a path or in an output file name
 * pattern used to reach any file the process could read or write. Writing an arbitrary file
 * is as good as running a command, which made the {@code --allow-exec} opt-in pointless.
 *
 * <p>The root is never taken from the template itself. Unless the caller sets one
 * ({@code --root} on the CLI, {@code <root>} in the Maven plugin,
 * {@link GeneratorConfig#setProjectRoot(File)} in code) it is detected from the root
 * source file: the nearest ancestor directory that contains a {@code .git} entry, or the
 * source file's own directory when there is none. Paths are compared after following
 * every link the way the operating system will (see {@link #canonical(File)}), so a
 * symbolic link or junction inside the root that points outside of it does not widen the
 * root.
 */
public final class ProjectRoot {

    /** Bound on chained dangling links, so a link cycle cannot recurse forever. */
    private static final int MAX_LINK_HOPS = 40;

    private ProjectRoot() {/* utility */}

    /**
     * Detects the root for a source file: the nearest ancestor holding {@code .git}, else
     * the directory of the file. {@code .git} may be a directory or a file (worktrees and
     * submodules use a file).
     */
    public static File detect(File rootSourceFile) {
        File start = rootSourceFile == null ? new File(".") : rootSourceFile.getAbsoluteFile().getParentFile();
        try {
            start = canonical(start);
        } catch (UnresolvablePathException e) {
            // Kept as written: every later containment check against it fails closed.
            return start.getAbsoluteFile();
        }
        for (File dir = start; dir != null; dir = dir.getParentFile()) {
            if (new File(dir, ".git").exists()) {
                return dir;
            }
        }
        return start;
    }

    public static boolean contains(File root, File candidate) {
        try {
            return canonical(candidate).toPath().startsWith(canonical(root).toPath());
        } catch (UnresolvablePathException e) {
            return false;
        }
    }

    /**
     * Returns {@code candidate} when it lies inside {@code root} and throws otherwise. The
     * message names the offending path and the way to widen the root, because the usual
     * cause is a legitimate layout the detection could not guess.
     *
     * @param what short description of the path's role, e.g. {@code "import path"}.
     */
    public static File require(File root, File candidate, String what) {
        if (!contains(root, candidate)) {
            throw new OutsideProjectRootException(what + " is outside the project root: "
                    + describe(candidate) + " (root: " + describe(root)
                    + "). Pass --root <dir> (Maven: <root>) to widen the root.");
        }
        return candidate;
    }

    /**
     * Resolves a path the way the operating system will when the file is opened: component
     * by component, following every link on the way.
     *
     * <p>{@link File#getCanonicalFile()} is not enough here. On Windows it does not follow
     * symbolic links or junctions (JDK 8–23), so a link committed inside the repository would
     * pass the containment check and still lead outside. And normalising {@code ..} lexically
     * before following links is wrong on every platform: {@code link/../x} is a sibling of the
     * link's <em>target</em>, not of the link.
     *
     * <p>Components that do not exist yet (an output file about to be created) are appended
     * as written; nothing can redirect them. A dangling symbolic link is followed by hand,
     * because writing through it would create its target.
     */
    static File canonical(File file) {
        Path absolute = (file == null ? new File(".") : file).getAbsoluteFile().toPath();
        return resolveLinks(absolute, 0).toFile();
    }

    private static Path resolveLinks(Path absolute, int depth) {
        Path current = absolute.getRoot();
        if (current == null) {
            return absolute.normalize();
        }
        for (Path name : absolute) {
            String segment = name.toString();
            if (segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                // 'current' holds no links at this point, so its parent is the real parent.
                if (current.getParent() != null) {
                    current = current.getParent();
                }
                continue;
            }
            Path next = current.resolve(name);
            if (Files.exists(next, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    next = next.toRealPath();
                } catch (IOException e) {
                    next = followDanglingLink(current, next, depth);
                }
            }
            current = next;
        }
        return current;
    }

    /**
     * Reached when an existing entry could not be resolved by the file system. The only case
     * with a safe answer is a dangling symbolic link whose target can be read; anything else
     * (a link cycle, an unreadable link, too many hops) fails closed — the lexical path says
     * nothing about where the entry really leads.
     */
    private static Path followDanglingLink(Path parent, Path link, int depth) {
        if (depth >= MAX_LINK_HOPS || !Files.isSymbolicLink(link)) {
            throw new UnresolvablePathException(link);
        }
        try {
            return resolveLinks(parent.resolve(Files.readSymbolicLink(link)), depth + 1);
        } catch (IOException e) {
            throw new UnresolvablePathException(link);
        }
    }

    /** A path whose real location could not be established; never treated as inside a root. */
    private static final class UnresolvablePathException extends RuntimeException {
        UnresolvablePathException(Path path) {
            super("cannot resolve " + path);
        }
    }

    /** Resolved path for messages; falls back to the absolute path when resolution fails. */
    static String describe(File file) {
        try {
            return canonical(file).getPath();
        } catch (UnresolvablePathException e) {
            return (file == null ? new File(".") : file).getAbsolutePath();
        }
    }

    /**
     * Thrown when a template names a path outside the project root. Unchecked so it travels
     * through widget rendering like the other fatal template errors.
     */
    public static final class OutsideProjectRootException extends RuntimeException {
        public OutsideProjectRootException(String message) {
            super(message);
        }
    }
}
