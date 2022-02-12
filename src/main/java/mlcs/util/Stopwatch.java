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
