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

import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;

import static dev.ultreon.logging.AnsiColors.*;

public class Logger {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final LoggerManager manager;
    private final String name;

    Logger(LoggerManager manager, String name) {
        this.manager = manager;
        this.name = name;
    }

    /**
     * Log a message with the specified level, category, message, exception, and other flags.
     *
     * @param time          The time of the log event
     * @param level         The log level
     * @param category      The category of the log
     * @param msg           The message to log
     * @param exc           The exception to log
     * @param fromReplay    Flag indicating if the log is from a replay
     * @param wasSuppressed Flag indicating if the log was suppressed
     */
    public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
        if (this.name.startsWith("io.netty.handler.codec.http.websocketx."))
            return;

        if (!this.isEnabled(level)) return;
        if (msg == null) {
            this.doLog(time, level, category, null, exc, fromReplay, wasSuppressed);
            return;
        }
        for (var line : msg.split("\r\n|\r|\n")) {
            this.doLog(time, level, category, line, exc, fromReplay, wasSuppressed);
            exc = null; //? Prevent exception log spam
        }
    }

    private void doLog(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
        var sb = new StringBuilder();
        var format = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()).format(FORMAT);
        sb.append(BLUE).append(format).append(" ");
        sb.append(this.colorFor(level)).append("[").append(Thread.currentThread().getName()).append("/").append(level).append("] ");
        sb.append(CYAN).append("(").append(this.name);
        if (category != null && category != LogCategory.DEFAULT) {
            sb.append("/").append(category.getName());
        }
        sb.append(") ");
        if (msg != null)
            sb.append(RESET).append(msg);
        if (wasSuppressed) {
            sb.append(RED + " (suppressed)" + RESET);
        }
        if (fromReplay) {
            sb.append(RED + " (replay)" + RESET);
        }
        sb.append(RESET + "\n");
        if (exc == null) {
            this.outFor(level).print(sb);
            return;
        }
        sb.append(BLUE).append(exc.getClass().getName()).append(": ").append(RESET).append(exc.getMessage()).append("\n");
        stack(exc, new StackTraceElement[0], sb);

        sb.append("\n");

        this.outFor(level).print(sb);
    }

    private PrintStream outFor(LogLevel level) {
        if (level.ordinal() < LogLevel.DEBUG.ordinal()) {
            return System.out;
        } else if (level.ordinal() < LogLevel.INFO.ordinal()) {
            return this.manager.debugOut;
        } else {
            return this.manager.out;
        }
    }

    private static void stack(Throwable exc, StackTraceElement[] lastStack, StringBuilder sb) {
        var stElems = removeLast(exc.getStackTrace(), lastStack);
        for (var ste : stElems) {
            sb.append("    at ").append(CYAN).append(ste.getClassName().replace("$", GREEN + "$").replace(".", CYAN + ".")).append(RESET);
            var methodName = ste.getMethodName();
            if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
                sb.append(".").append(RED).append(methodName);
            } else if (methodName.startsWith("lambda$")) {
                sb.append(".").append(RED).append("lamda$").append(YELLOW).append(methodName.substring("lambda$".length()).replace("$", RED + "$"));
            } else {
                sb.append(".").append(YELLOW).append(methodName.replace("$", RED + "$" + YELLOW));
            }
            sb.append(RESET).append("(").append(PURPLE).append(ste.getFileName() == null ? RED + "Unknown source" : ste.getFileName()).append(RESET);
            if (ste.getLineNumber() > 0) {
                sb.append(":").append(PURPLE).append(ste.getLineNumber());
            } else {
                sb.append(":").append(RED).append("0");
            }
            sb.append(RESET).append(")").append("\n");
        }
        if (stElems.size() != exc.getStackTrace().length) {
            sb.append("    ... ").append(PURPLE).append(exc.getStackTrace().length - stElems.size()).append(" more").append("\n");
        }

        var cause = exc.getCause();
        if (cause != null) {
            sb.append(RED).append("Caused by: ").append(BLUE).append(cause.getClass().getName()).append(": ").append(RESET).append(cause.getMessage()).append("\n");
            stack(cause, exc.getStackTrace(), sb);
        }

        for (var suppressed : exc.getSuppressed()) {
            sb.append(RED).append("    Suppressed: ").append(BLUE).append(suppressed.getClass().getName()).append(": ").append(RESET).append(suppressed.getMessage()).append("\n");
            var suppressedSb = new StringBuilder();
            stack(suppressed, exc.getStackTrace(), suppressedSb);
            for (var str : suppressedSb.toString().split("\r\n|\r|\n")) {
                sb.append("    ").append(str).append("\n");
            }
        }
    }

    private static Collection<? extends StackTraceElement> removeLast(StackTraceElement[] stackTrace, StackTraceElement[] lastStack) {
        if (lastStack.length == 0) {
            return Arrays.asList(stackTrace);
        }

        var i = 0;
        while (true) {
            if (i == stackTrace.length || i >= lastStack.length) break;

            var ste = stackTrace[stackTrace.length - i - 1];
            var lastSte = lastStack[lastStack.length - i - 1];

            if (!ste.equals(lastSte)) {
                if (i > 0) i--;
                break;
            }

            i++;
        }

        return Arrays.asList(stackTrace).subList(0, stackTrace.length - i);
    }

    /**
     * Determines the ANSI color code associated with the specified log level.
     *
     * @param level The log level for which to retrieve the associated color.
     * @return The ANSI color code as a string that corresponds to the given log level.
     * @throws IllegalArgumentException If the provided log level is not recognized.
     */
    private String colorFor(LogLevel level) {
        return switch (level) {
            case TRACE -> PURPLE;
            case DEBUG -> BLACK;
            case INFO -> GREEN;
            case WARN -> YELLOW;
            case ERROR -> RED;
        };
    }

    public void info(LogCategory category, String msg) {
        this.log(System.currentTimeMillis(), LogLevel.INFO, category, msg, null, false, false);
    }

    public void trace(LogCategory category, String msg) {
        this.log(System.currentTimeMillis(), LogLevel.TRACE, category, msg, null, false, false);
    }

    public void debug(LogCategory category, String msg) {
        this.log(System.currentTimeMillis(), LogLevel.DEBUG, category, msg, null, false, false);
    }

    public void warn(LogCategory category, String msg) {
        this.log(System.currentTimeMillis(), LogLevel.WARN, category, msg, null, false, false);
    }

    public void error(LogCategory category, String msg) {
        this.log(System.currentTimeMillis(), LogLevel.ERROR, category, msg, null, false, false);
    }

    public void trace(String msg) {
        this.log(System.currentTimeMillis(), LogLevel.TRACE, LogCategory.DEFAULT, msg, null, false, false);
    }

    public boolean isEnabled(LogLevel logLevel) {
        var ordinal = logLevel.ordinal();
        return ordinal >= this.manager.getLevel().ordinal();
    }
}
