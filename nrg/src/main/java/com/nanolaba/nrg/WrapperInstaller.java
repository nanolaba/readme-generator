package com.nanolaba.nrg;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Backs {@code nrg --init-wrapper}: writes {@code nrgw}, {@code nrgw.cmd} and
 * {@code .nrg/wrapper.properties} into a project, so that anyone who clones it — a contributor,
 * a CI job, a coding agent in a fresh sandbox — runs the pinned NRG release with one command
 * and nothing installed but Java. It is the {@code mvnw} / {@code gradlew} idea for projects
 * that are not built with Maven.
 *
 * <p>The scripts are copied from the jar's {@code wrapper/} resources with their line endings
 * forced (LF for the shell script, CRLF for the batch file), whatever the checkout did to them.
 * The release checksum is taken from the {@code .sha256} file published next to the release
 * zip unless the caller passes one; when neither is available the properties are written
 * without it and the wrapper falls back to checking the download against the published file.
 */
final class WrapperInstaller {

    static final String PROPERTIES_PATH = ".nrg/wrapper.properties";
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)*");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final int FETCH_TIMEOUT_MS = 15_000;
    private static final int MAX_CHECKSUM_BYTES = 4096;

    private final Function<String, String> fetcher;

    WrapperInstaller() {
        this(WrapperInstaller::fetch);
    }

    /** @param fetcher returns the body of a URL, or {@code null} when it cannot be read */
    WrapperInstaller(Function<String, String> fetcher) {
        this.fetcher = fetcher;
    }

    static String distributionUrl(String version) {
        return "https://github.com/nanolaba/readme-generator/releases/download/v" + version
                + "/readme-generator-" + version + ".zip";
    }

    /**
     * @param version release to pin; {@code null} pins the running NRG, which must then be a
     *                release rather than a development build
     * @param sha256  checksum of the release zip; {@code null} fetches the published one
     * @return the CLI exit status
     */
    int install(File directory, String version, String sha256) throws IOException {
        String pinned = version != null ? version.trim() : NRG.getVersion().trim();
        if (!VERSION.matcher(pinned).matches()) {
            System.err.println("--init-wrapper: '" + pinned + "' is not a release version; "
                    + "name one with --wrapper-version (e.g. --wrapper-version 1.3)");
            return NRG.EXIT_USAGE;
        }
        String checksum = sha256 != null ? sha256.trim().toLowerCase(Locale.ROOT) : publishedChecksum(pinned);
        if (checksum != null && !SHA256.matcher(checksum).matches()) {
            System.err.println("--init-wrapper: '" + sha256 + "' is not a sha256 checksum (64 hex digits)");
            return NRG.EXIT_USAGE;
        }
        if (checksum == null) {
            System.err.println("WARN: could not read " + distributionUrl(pinned) + ".sha256; the properties are "
                    + "written without sha256= and the wrapper will check the download against the published "
                    + "checksum. Add sha256= to " + PROPERTIES_PATH + " to pin it.");
        }

        File nrgw = new File(directory, "nrgw");
        write(nrgw, resource("nrgw"), "\n");
        if (!nrgw.setExecutable(true, false)) {
            System.err.println("WARN: could not mark " + nrgw.getPath() + " executable; run: chmod +x nrgw");
        }
        write(new File(directory, "nrgw.cmd"), resource("nrgw.cmd"), "\r\n");
        String properties = "# NRG wrapper (https://github.com/nanolaba/readme-generator): ./nrgw and nrgw.cmd run this\n"
                + "# release, downloaded once into ~/.nrg/wrapper/. Change the version with:\n"
                + "#   nrg --init-wrapper --wrapper-version <version>\n"
                + "version=" + pinned + "\n"
                + (checksum != null ? "sha256=" + checksum + "\n" : "")
                + "# distributionUrl=" + distributionUrl(pinned) + "\n";
        write(new File(directory, PROPERTIES_PATH), properties, "\n");

        System.out.println("Created nrgw, nrgw.cmd and " + PROPERTIES_PATH + " for NRG " + pinned
                + ". Commit them, then run ./nrgw -f README.src.md (nrgw.cmd on Windows).");
        return NRG.EXIT_OK;
    }

    private String publishedChecksum(String version) {
        String body = fetcher.apply(distributionUrl(version) + ".sha256");
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        String first = body.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        return SHA256.matcher(first).matches() ? first : null;
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = WrapperInstaller.class.getResourceAsStream("/wrapper/" + name)) {
            if (in == null) {
                throw new IOException("wrapper script " + name + " is missing from the NRG jar");
            }
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    private static void write(File file, String content, String lineEnding) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        String normalised = content.replace("\r\n", "\n").replace("\n", lineEnding);
        Files.write(file.toPath(), normalised.getBytes(StandardCharsets.UTF_8));
    }

    private static String fetch(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(FETCH_TIMEOUT_MS);
            connection.setReadTimeout(FETCH_TIMEOUT_MS);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[MAX_CHECKSUM_BYTES];
                int length = IOUtils.read(in, buffer);
                return new String(buffer, 0, length, StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
