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

package dev.ultreon.logging.compat;

import dev.ultreon.logging.LogCategory;
import dev.ultreon.logging.LogLevel;
import dev.ultreon.logging.Logger;
import dev.ultreon.logging.LoggerManager;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

class ULogger extends AbstractLogger {
    private static final Logger FAIL_LOGGER = LoggerManager.getLogger("Slf4J:Compat");
    private final Logger logger;

    ULogger(String name) {
        this.logger = LoggerManager.getLogger(name);
        this.name = name;
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.isEnabled(LogLevel.TRACE);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return this.logger.isEnabled(LogLevel.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isEnabled(LogLevel.DEBUG);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return this.logger.isEnabled(LogLevel.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isEnabled(LogLevel.INFO);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return this.logger.isEnabled(LogLevel.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return this.logger.isEnabled(LogLevel.WARN);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return this.logger.isEnabled(LogLevel.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isEnabled(LogLevel.ERROR);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return this.logger.isEnabled(LogLevel.ERROR);
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
        var format = this.format(messagePattern, arguments);
        if (format == null) return;
        if (throwable != null) {
            if (format.getThrowable() != null) {
                throwable.addSuppressed(format.getThrowable());
            }
        } else if (format.getThrowable() != null) {
            throwable = format.getThrowable();
        }
        this.logger.log(System.currentTimeMillis(), this.level(level), LogCategory.DEFAULT, format.getMessage(), throwable, false, false);
    }

    private FormattingTuple format(String messagePattern, Object[] arguments) {
        try {
            return MessageFormatter.arrayFormat(messagePattern, arguments);
        } catch (Exception e) {
            FAIL_LOGGER.error(LogCategory.DEFAULT, "Failed to format message! " + e.getMessage());
            return null;
        }
    }

    private LogLevel level(Level level) {
        return switch (level) {
            case TRACE -> LogLevel.TRACE;
            case DEBUG -> LogLevel.DEBUG;
            case WARN -> LogLevel.WARN;
            case ERROR -> LogLevel.ERROR;
            default -> LogLevel.INFO;
        };
    }
}
