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

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.util.WeakHashMap;

public class SLF4JCompat implements SLF4JServiceProvider {
    public static String REQUESTED_API_VERSION = "2.0.99";

    private final WeakHashMap<String, ULogger> loggers = new WeakHashMap<>();
    private final WeakHashMap<String, ULoggerMarker> marker = new WeakHashMap<>();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return name -> this.loggers.computeIfAbsent(name, ULogger::new);
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return new IMarkerFactory() {

            @Override
            public Marker getMarker(String name) {
                return SLF4JCompat.this.marker.computeIfAbsent(name, ULoggerMarker::new);
            }

            @Override
            public boolean exists(String name) {
                return SLF4JCompat.this.marker.containsKey(name);
            }

            @Override
            public boolean detachMarker(String name) {
                var remove = SLF4JCompat.this.marker.remove(name);
                if (remove != null) {
                    // Welp fuck off then :3
                }
                return remove != null;
            }

            @Override
            public Marker getDetachedMarker(String name) {
                return null;
            }
        };
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return null;
    }

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    @Override
    public void initialize() {
        // Meow :3
    }
}
