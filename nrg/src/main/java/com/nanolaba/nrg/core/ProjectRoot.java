package com.nanolaba.nrg.core;

import java.io.File;
import java.io.IOException;

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
 * source file's own directory when there is none. Comparisons use canonical paths, so a
 * symbolic link inside the root that points outside of it does not widen the root.
 */
public final class ProjectRoot {

    private ProjectRoot() {/* utility */}

    /**
     * Detects the root for a source file: the nearest ancestor holding {@code .git}, else
     * the directory of the file. {@code .git} may be a directory or a file (worktrees and
     * submodules use a file).
     */
    public static File detect(File rootSourceFile) {
        File start = canonical(rootSourceFile == null ? new File(".") : rootSourceFile.getAbsoluteFile().getParentFile());
        for (File dir = start; dir != null; dir = dir.getParentFile()) {
            if (new File(dir, ".git").exists()) {
                return dir;
            }
        }
        return start;
    }

    public static boolean contains(File root, File candidate) {
        return canonical(candidate).toPath().startsWith(canonical(root).toPath());
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
                    + canonical(candidate).getPath() + " (root: " + canonical(root).getPath()
                    + "). Pass --root <dir> (Maven: <root>) to widen the root.");
        }
        return candidate;
    }

    static File canonical(File file) {
        File absolute = (file == null ? new File(".") : file).getAbsoluteFile();
        try {
            return absolute.getCanonicalFile();
        } catch (IOException e) {
            return absolute.toPath().normalize().toFile();
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
