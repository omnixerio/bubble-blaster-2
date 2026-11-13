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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

public class MultiPrintStream extends PrintStream {
    private volatile PrintStream[] streams;

    public MultiPrintStream(PrintStream... streams) {
        super(streams[0]);
        this.streams = streams;
    }

    public MultiPrintStream(boolean autoFlush, PrintStream... streams) {
        super(streams[0], autoFlush);
        this.streams = streams;
    }

    public void addStream(PrintStream stream) {
        var old = this.streams;
        var updated = new PrintStream[old.length + 1];
        System.arraycopy(old, 0, updated, 0, old.length);
        updated[old.length] = stream;
        this.streams = updated;
    }

    @Override
    public void write(int b) {
        for (var stream : this.streams) {
            stream.write(b);
        }
    }

    @Override
    public void write(byte[] buf) throws IOException {
        for (var stream : this.streams) {
            stream.write(buf);
        }
    }

    @Override
    public void write(@NotNull byte[] buf, int off, int len) {
        for (var stream : this.streams) {
            stream.write(buf, off, len);
        }
    }

    @Override
    public void flush() {
        for (var stream : this.streams) {
            stream.flush();
        }
    }

    @Override
    public void close() {
        for (var stream : this.streams) {
            try {
                stream.close();
            } catch (Exception ignored) {
                // Ignore
            }
        }
    }

    @Override
    public void print(boolean b) {
        for (var stream : this.streams) {
            stream.print(b);
        }
    }

    @Override
    public void print(char c) {
        for (var stream : this.streams) {
            stream.print(c);
        }
    }

    @Override
    public void print(int i) {
        for (var stream : this.streams) {
            stream.print(i);
        }
    }

    @Override
    public void print(long l) {
        for (var stream : this.streams) {
            stream.print(l);
        }
    }

    @Override
    public void print(float f) {
        for (var stream : this.streams) {
            stream.print(f);
        }
    }

    @Override
    public void print(double d) {
        for (var stream : this.streams) {
            stream.print(d);
        }
    }

    @Override
    public void print(@NotNull char[] s) {
        for (var stream : this.streams) {
            stream.print(s);
        }
    }

    @Override
    public void print(@Nullable String s) {
        for (var stream : this.streams) {
            stream.print(s);
        }
    }

    @Override
    public void print(@Nullable Object obj) {
        for (var stream : this.streams) {
            stream.print(obj);
        }
    }

    @Override
    public void println() {
        for (var stream : this.streams) {
            stream.println();
        }
    }

    @Override
    public void println(boolean x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(char x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(int x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(long x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(float x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(double x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(@NotNull char[] x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(@Nullable String x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(@Nullable Object x) {
        for (var stream : this.streams) {
            stream.println(x);
        }
    }

    @Override
    public PrintStream printf(@NotNull String format, Object... args) {
        for (var stream : this.streams) {
            stream.printf(format, args);
        }
        return this;
    }

    @Override
    public PrintStream printf(Locale l, @NotNull String format, Object... args) {
        for (var stream : this.streams) {
            stream.printf(l, format, args);
        }
        return this;
    }

    @Override
    public PrintStream format(@NotNull String format, Object... args) {
        for (var stream : this.streams) {
            stream.format(format, args);
        }
        return this;
    }

    @Override
    public PrintStream format(Locale l, @NotNull String format, Object... args) {
        for (var stream : this.streams) {
            stream.format(l, format, args);
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq) {
        for (var stream : this.streams) {
            stream.append(csq);
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        for (var stream : this.streams) {
            stream.append(csq, start, end);
        }
        return this;
    }

    @Override
    public PrintStream append(char c) {
        for (var stream : this.streams) {
            stream.append(c);
        }
        return this;
    }


}
