/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright @ 2005, The Beangle Software.
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
package mlcs.misc;

import mlcs.EPCrawler;
import mlcs.Location;
import mlcs.Node;
import mlcs.util.Queues;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class TopologicalSorter {

  private static Map<Short, List<Node>> findHeads(EPCrawler crawler, List<Node> starts) {
    Map<Short, List<Node>> headLevels = new HashMap<>();
    for (Node head : starts) {
      short level = head.level;
      if (head.isPresEmpty() && level >= 0) {
        List<Node> levelLocs = headLevels.get(level);
        if (null == levelLocs) {
          levelLocs = new ArrayList<>();
          headLevels.put(level, levelLocs);
        }
        levelLocs.add(head);
      }
    }
    return headLevels;
  }

  /**
   * According to the indegree of node, 1) setting the hierarchy, 2) setting the successors of nodes.
   *
   * @return 瀛愬浘鐨勬渶灏忓拰鏈€澶у眰绾
   */
  public static short[] sort(EPCrawler crawler, HashMap<Location, Node> nodes, List<Node> starts) {
    Map<Short, List<Node>> heads = findHeads(crawler, starts);
    if (heads.isEmpty()) return new short[]{0, 0};

    short nextLevel = Short.MAX_VALUE;
    short maxLevel = 0;
    for (Short level : heads.keySet()) {
      if (level > maxLevel) maxLevel = level;
      if (level < nextLevel) nextLevel = level;
    }
    final short minLevel = nextLevel;

    ForkJoinPool pool = crawler.mlcs.newPool();
    int parallelism = pool.getParallelism();

    ArrayList<Node> levelQueue = (ArrayList<Node>) heads.get(nextLevel);
    while (!levelQueue.isEmpty() || nextLevel <= maxLevel) {
      nextLevel += 1;
      List<int[]> segs = Queues.split(levelQueue.size(), parallelism);
      LinkedList<ForkJoinTask<Map<Short, List<Node>>>> tasks = new java.util.LinkedList<>();
      for (int[] seg : segs) {
        tasks.add(pool.submit(new LevelCrawler(crawler, nodes, nextLevel, levelQueue, seg[0], seg[1])));
      }
      levelQueue = new ArrayList<>();
      if (heads.containsKey(nextLevel))
        levelQueue.addAll(heads.get(nextLevel));

      for (ForkJoinTask<Map<Short, List<Node>>> task : tasks) {
        Map<Short, List<Node>> nexts = task.join();
        // Some of the nodes with 0 indegree after each traversal are just used next time, and some are only used later.
        for (Map.Entry<Short, List<Node>> e : nexts.entrySet()) {
          if (e.getKey() > maxLevel) {
            maxLevel = e.getKey();
          }
          if (e.getKey().shortValue() == nextLevel) {
            levelQueue.addAll(e.getValue());
          } else {
            List<Node> otherLevelLocs = heads.get(e.getKey());
            if (null == otherLevelLocs) {
              if (e.getValue() instanceof ArrayList<?>) {
                heads.put(e.getKey(), e.getValue());
              } else {
                heads.put(e.getKey(), new ArrayList<>(e.getValue()));
              }
            } else {
              otherLevelLocs.addAll(e.getValue());
            }
          }
        }
      }
    }
    pool.shutdown();
    return new short[]{minLevel, (short) (crawler.end.level - 1)};
  }

  /**
   * Topological Sorting Tasks for Each Node
   */
  @SuppressWarnings("serial")
  static class LevelCrawler extends RecursiveTask<Map<Short, List<Node>>> {
    EPCrawler crawler;
    HashMap<Location, Node> nodes;
    ArrayList<Node> locations;
    int from, to, level;

    public LevelCrawler(EPCrawler crawler, HashMap<Location, Node> nodes, int level, ArrayList<Node> locations, int from, int to) {
      super();
      this.crawler = crawler;
      this.level = level;
      this.locations = locations;
      this.from = from;
      this.nodes = nodes;
      this.to = to;
    }

    @Override
    public Map<Short, List<Node>> compute() {
      Map<Short, List<Node>> nexts = new HashMap<>();
      for (int i = from; i < to; i++) {
        Node fromNode = locations.get(i);
        List<Location> successors = crawler.mlcs.nextLocations(fromNode.id);
        boolean hasSuccessor = false;
        for (Location to : successors) {
          Node toNode = nodes.get(to);
          if (null != toNode) {
            //After adding the precursor, the indegree decreases by one, and whether the indegree is 0 or not
            if (toNode.addPredecessor(crawler, fromNode, level)) {
              List<Node> levelNexts = nexts.get(toNode.level);
              if (null == levelNexts) {
                levelNexts = new ArrayList<>();
                nexts.put(toNode.level, levelNexts);
              }
              levelNexts.add(toNode);
            }
            hasSuccessor = true;
          }
        }
        if (!hasSuccessor) crawler.end.addPredecessor(crawler, fromNode, level);
      }
      return nexts;
    }
  }
}
