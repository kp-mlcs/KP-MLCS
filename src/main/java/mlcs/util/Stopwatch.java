/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mlcs.util;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Stopwatch {
  public static String format(long mills) {
    TimeUnit unit = chooseUnit(mills);
    double value = ((double) mills) / MILLISECONDS.convert(1, unit);
    return String.format("%.4g %s", value, abbreviate(unit));
  }

  private static TimeUnit chooseUnit(long nanos) {
    if (SECONDS.convert(nanos, MILLISECONDS) > 0) return SECONDS;
    else return MILLISECONDS;
  }

  private static String abbreviate(TimeUnit unit) {
    if (unit == MILLISECONDS) return "ms";
    else if (unit == SECONDS) return "s";
    else throw new AssertionError();
  }
}
