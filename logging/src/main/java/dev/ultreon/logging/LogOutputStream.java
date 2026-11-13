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
import java.io.OutputStream;
import java.io.PrintStream;

public class LogOutputStream extends OutputStream {
    private final Logger logger;
    private final StringBuffer buffer = new StringBuffer();
    private final PrintStream out;

    public LogOutputStream(LoggerManager loggerManager, PrintStream out, String name) {
        super();
        this.out = out;

        this.logger = loggerManager.createLogger(name);
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (this) {
            if (b == '\n' || b == '\r') {
                this.flush();
            } else {
                this.buffer.append((char) b);
            }
        }
    }

    @Override
    public void flush() {
        if (this.buffer.length() == 0) {
            return;
        }
        try {
            this.logger.log(System.currentTimeMillis(), LogLevel.INFO, LogCategory.DEFAULT, this.buffer.toString(), null, false, false);
        } catch (Throwable e) {
            this.out.println("Failed to log message: " + e.getMessage());
        }
        this.buffer.setLength(0);
    }
}
