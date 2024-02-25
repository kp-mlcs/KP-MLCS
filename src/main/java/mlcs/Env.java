package mlcs;

import java.util.Map;

public class Env {
  final int parallelism;

  final int cores;

  /**
   * how many MB
   */
  final long memory;

  final String os;

  final String jre;

  final Map<String, String> args;

  public Env(int parallelism, int cores, long memory, String os, String jre, Map<String, String> args) {
    this.parallelism = parallelism;
    this.cores = cores;
    this.memory = memory;
    this.os = os;
    this.jre = jre;
    this.args = args;
  }

  public static Env get(Map<String, String> arguments) {
    var cores = Runtime.getRuntime().availableProcessors();
    var parallelism = Integer.parseInt(arguments.getOrDefault("parallelism", "0"));
    int p = (parallelism <= 0) ? cores : parallelism;
    var os = System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch");
    var memory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
    var jre = System.getProperty("java.vm.version") + "(" + System.getProperty("java.vendor")+")";
    return new Env(p, cores, memory, os, jre, arguments);
  }

  public static Env parse(String s) {
    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : args.entrySet()) {
      sb.append(" " + entry.getKey() + "=" + entry.getValue());
    }
    return "  parallelism:" + parallelism + "\n  cores:" + cores + "\n  memory:" + memory +
        "MB\n  os:" + os + "\n  jre:" + jre + "\n  args:" + sb.toString();
  }

}
