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

import java.util.Objects;

public class LogCategory {
    public static final LogCategory DEFAULT = new LogCategory("DEFAULT");
    private final String name;

    public LogCategory(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) return false;
        var that = (LogCategory) o;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name);
    }

    public String getName() {
        return this.name;
    }
}
