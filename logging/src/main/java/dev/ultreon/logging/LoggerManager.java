/*
 * Copyright (c) 2023-2026. Omnixerio Studios.
 *
 * All Rights Reserved.
 *
 * Permission is granted to use and execute the Software. Users may modify
 * the behavior, state, configuration, memory contents, or execution flow
 * of the Software solely during runtime for their own use.
 *
 * No person may create, save, distribute, publish, sublicense, sell, or
 * otherwise make available any modified version, derivative work, patched
 * copy, reverse-engineered version, or adapted form of the Software. Any
 * modifications must exist only in volatile memory during execution and
 * must not be preserved after the Software terminates.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dev.ultreon.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class LoggerManager {
    private static final LoggerManager INSTANCE = new LoggerManager();
    private final Map<String, Logger> loggers = new HashMap<>();
    public final PrintStream out;
    public final PrintStream debugOut;
    private final PrintStream oldOut;
    private final PrintStream oldErr;
    private final Path path;
    private LogLevel level = LogLevel.INFO;

    private LoggerManager() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        if (System.getProperty("ultreon.log.level") != null) {
            try {
                this.level = LogLevel.valueOf(System.getProperty("ultreon.log.level").toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid log level: " + System.getProperty("ultreon.log.level"));
            }
        }

        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) this.path = Paths.get(System.getenv("APPDATA"), "QuantumVoxel");
        else if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) this.path = Paths.get(System.getProperty("user.home"), "Library/Application Support/QuantumVoxel");
        else if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")) this.path = Paths.get(System.getProperty("user.home"), ".config/QuantumVoxel");
        else throw new RuntimeException("Platform unsupported: " + System.getProperty("os.name"));

        if (Files.notExists(this.path.resolve("logs"))) {
            try {
                Files.createDirectories(this.path.resolve("logs"));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create log directory", e);
            }
        }

        this.compressIfExists(this.path.resolve("logs/latest.log"));
        this.compressIfExists(this.path.resolve("logs/debug.log"));

        try {
            this.debugOut = new MultiPrintStream(true, System.out, new PrintStream(new ANSIEscapingOutputStream(Files.newOutputStream(
                    this.path.resolve("logs/debug.log")
            )), true));
            this.out = new MultiPrintStream(true, this.debugOut, new PrintStream(new ANSIEscapingOutputStream(Files.newOutputStream(
                    this.path.resolve("logs/latest.log")
            )), true));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log file", e);
        }

        this.oldOut = System.out;
        this.oldErr = System.err;
    }

    private void compressIfExists(Path path) {
        if (Files.exists(path)) {
            this.compress(path, this.path.resolve("logs/latest.log.gz"));
        }
    }

    private void compress(Path path, Path out) {
        try {
            Files.deleteIfExists(out);
            Files.createFile(out);

            try (var gzip = new GZIPOutputStream(Files.newOutputStream(out))) {
                var buffer = new byte[1024];
                int len;
                try (var in = Files.newInputStream(path)) {
                    while ((len = in.read(buffer)) > 0) {
                        gzip.write(buffer, 0, len);
                    }
                }

                gzip.finish();
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress log file", e);
        }
    }

    private void close() {
        this.out.close();

        System.setOut(this.oldOut);
        System.setErr(this.oldErr);

        this.loggers.clear();
        INSTANCE.loggers.clear();
    }

    public static Logger getLogger(String name) {
        return INSTANCE.loggers.computeIfAbsent(name, INSTANCE::createLogger);
    }

    public Logger createLogger(String name) {
        return new Logger(this, name);
    }

    public LogLevel getLevel() {
        return this.level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public static void registerOutputStream(PrintStream stream) {
        if (INSTANCE.out instanceof MultiPrintStream multi) {
            multi.addStream(stream);
        }
    }
}
