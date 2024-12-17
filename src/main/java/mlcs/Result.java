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
package mlcs;

import mlcs.util.Painter;
import mlcs.util.Stopwatch;

import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MLCS Result
 * It contains result graph,mlcs max level and counts;
 */
public class Result {
  private static final AtomicInteger generator = new AtomicInteger(1);

  public final String id;
  public final Graph graph;
  public final Setting setting;
  public final Setting.Env env;
  public final BigDecimal mlcsCount; // number of matched results
  public final int maxLevel; // length of matching results
  public final long nodeCount;// key node counts
  public final long startAt;// the algorithm start time
  public final long endAt;// the algorithm finish time
  public final long totalCreateCount; // total create node count in this process
  public final long highestCapacity; // highest node count in this process

  public Result(String id, Setting setting, Setting.Env env, Graph graph, BigDecimal count, long nodeCount, int maxLevel, long totalCreateCount,
                long highestCapacity, long startAt, long endAt) {
    this.id = id;
    this.setting = setting;
    this.env = env;
    this.graph = graph;
    this.mlcsCount = count;
    this.maxLevel = maxLevel;
    this.nodeCount = nodeCount;
    this.startAt = startAt;
    this.endAt = endAt;
    this.totalCreateCount = totalCreateCount;
    this.highestCapacity = highestCapacity;
  }

  public static String generateId(Long timeMillis) {
    var d = new java.util.Date();
    d.setTime(timeMillis);
    var df = new SimpleDateFormat("yyyyMMddHHmmss");
    var timestamp = df.format(d);

    return "kp-mlcs-"+timestamp +"-"+ generator.getAndIncrement();
  }


  public String getTime() {
    return Stopwatch.format(endAt - startAt);
  }

  public String buildResultString() {
    StringWriter fw = new StringWriter();
    Mlcs mlcs = graph.mlcs;
    fw.append("id:\n").append(this.id).append("\n");
    fw.append("env:\n").append(env.toString()).append("\n");
    fw.append("setting:\n").append(setting.toString()).append("\n");
    fw.append("sequences:\n");
    for (Sequence seq : mlcs.seqs) {
      fw.append("  ");
      fw.append(seq.toString());
      fw.append('\n');
    }
    fw.append("maxLevel: ").append(String.valueOf(maxLevel)).append('\n');
    fw.append("mlcsCount: ").append(String.valueOf(mlcsCount)).append('\n');
    fw.append("nodeCount: ").append(String.valueOf(nodeCount)).append('\n');
    fw.append("totalCreateCount: ").append(String.valueOf(totalCreateCount)).append('\n');
    fw.append("highestCapacity: ").append(String.valueOf(highestCapacity)).append('\n');
    fw.append("time: ").append(getTime()).append('\n');
    fw.append("startAt: ").append(formatTimeMills(startAt)).append('\n');
    fw.append("endAt: ").append(formatTimeMills(endAt)).append('\n');
    List<List<Location>> paths = null;
    if (null != mlcsCount) {
      if (mlcsCount.compareTo(new BigDecimal(100)) <= 0) {
        fw.append("mlcs:\n");
        paths = graph.paths();
      } else {
        fw.append("mlcs(top100):\n");
        paths = graph.paths(100);
      }
      for (List<Location> path : paths) {
        fw.append("  ");
        for (Location loc : path) {
          fw.append(mlcs.charAt(loc));
        }
        fw.append('\n');
      }
    }
    fw.append("nodes:\n");
    for (int i = 1; i <= maxLevel; i++) {
      HashMap<Location, Node> levelNodes = graph.getLevel(i);
      fw.append(" ");
      for (Location loc : levelNodes.keySet()) {
        fw.append(' ');
        fw.append(loc.toString());
      }
      fw.append('\n');
    }
    return fw.toString();
  }

  private static String formatTimeMills(long milli) {
    var d = new Date();
    d.setTime(milli);
    var df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return df.format(d);
  }

  private static long parseToTimeMills(String str) {
    var df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      var d = df.parse(str);
      return d.getTime();
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Dump the result into a file
   * It contains the original sequences and many statistics in the result.
   *
   * @param fileName
   */
  public void dumpTo(String fileName) {
    try {
      File f = new File(fileName);
      f.getParentFile().mkdirs();
      FileWriter fw = new FileWriter(fileName);
      fw.append(buildResultString());
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Show the result with one view
   */
  public void visualize() {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    if (ge.isHeadlessInstance()) {
      System.out.println("Cannot display window under headless model.");
    } else {
      Painter.visualize(this);
    }
  }

  /**
   * Parse a given dump file, and restore the result.
   *
   * @param fileName
   * @return
   */
  public static Result parse(String fileName) {
    String id = null;
    int maxLevel = 0;
    BigDecimal mlcsCount = null;
    int nodeCount = 0;
    long totalCreateCount = 0;
    long highestCapacity = 0;
    long startAt = 0;
    long endAt = 0;
    Mlcs mlcs = null;
    Graph graph = null;
    Setting setting = null;
    Setting.Env env = null;
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));
      String line = reader.readLine();
      while (null != line) {
        if (line.startsWith("id")) {
          id = contentOf(line);
        } else if (line.startsWith("maxLevel")) {
          maxLevel = Integer.parseInt(contentOf(line));
        } else if (line.startsWith("mlcsCount")) {
          mlcsCount = new BigDecimal(contentOf(line).toCharArray());
        } else if (line.startsWith("nodeCount")) {
          nodeCount = Integer.parseInt(contentOf(line));
        } else if (line.startsWith("totalCreateCount")) {
          totalCreateCount = Long.parseLong(contentOf(line));
        } else if (line.startsWith("highestCapacity")) {
          highestCapacity = Long.parseLong(contentOf(line));
        } else if (line.startsWith("startAt")) {
          startAt = parseToTimeMills(contentOf(line));
        } else if (line.startsWith("endAt")) {
          endAt = parseToTimeMills(contentOf(line));
        } else if (line.startsWith("setting")) {
          List<String> datas = new ArrayList<>();
          line = reader.readLine();
          while (line != null && line.charAt(0) == ' ') {
            datas.add(line);
            line = reader.readLine();
          }
          setting = Setting.parse(datas.toArray(new String[0]));
          continue;
        } else if (line.startsWith("env")) {
          List<String> datas = new ArrayList<>();
          line = reader.readLine();
          while (line != null && line.charAt(0) == ' ') {
            datas.add(line);
            line = reader.readLine();
          }
          env = Setting.Env.parse(datas.toArray(new String[0]));
          continue;
        } else if (line.startsWith("sequences")) {
          List<String> datas = new ArrayList<>();
          line = reader.readLine();
          while (line != null && line.charAt(0) == ' ') {
            datas.add(line);
            line = reader.readLine();
          }
          mlcs = Mlcs.build(datas.toArray(new String[datas.size()]));
          continue;
        } else if (line.startsWith("nodes")) {
          short level = 1;
          line = reader.readLine();
          graph = new Graph(mlcs);
          Node end = new Node(mlcs.end);
          graph.maxLevel = maxLevel;
          graph.end = end;
          graph.add((short) 0, mlcs.start, new Node(mlcs.start));
          graph.add((short) (maxLevel + 1), mlcs.end, end);

          while (line != null && line.charAt(0) == ' ') {
            HashMap<Location, Node> nodes = new HashMap<>();
            line = line.trim();
            String[] nodeStrs = line.split(" ");
            for (String nodestr : nodeStrs) {
              String[] locstr = nodestr.substring(1, nodestr.length() - 1).split(",");
              short[] index = new short[locstr.length];
              for (int i = 0; i < index.length; i++) {
                index[i] = Short.parseShort(locstr[i]);
              }
              Location loc = new Location(index);
              Node node = new Node(loc);
              node.level = level;
              nodes.put(loc, node);
            }
            graph.addLevel(level, nodes);
            level += 1;
            line = reader.readLine();
          }
          graph.link();
          continue;
        }
        line = reader.readLine();
      }
      reader.close();
      return new Result(id, setting, env, graph, mlcsCount, nodeCount, maxLevel, totalCreateCount, highestCapacity, startAt, endAt);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String contentOf(String line) {
    return line.substring(line.indexOf(':') + 1).trim();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return id;
  }

  /**
   * Display a result file.
   *
   * @param args
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: Result /path/to/result/file");
      return;
    }
    Result.parse(args[0]).visualize();
  }

  public Graph getGraph() {
    return graph;
  }

  public Setting getSetting() {
    return setting;
  }

  public Setting.Env getEnv() {
    return env;
  }

  public BigDecimal getMlcsCount() {
    return mlcsCount;
  }

  public int getMaxLevel() {
    return maxLevel;
  }

  public long getNodeCount() {
    return nodeCount;
  }

  public long getStartAt() {
    return startAt;
  }

  public long getEndAt() {
    return endAt;
  }

  public long getTotalCreateCount() {
    return totalCreateCount;
  }

  public long getHighestCapacity() {
    return highestCapacity;
  }
}
