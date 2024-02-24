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

import mlcs.util.Queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * A approximation algorithm
 * It obtains the approximate length of the MLCS as soon as possible by retaining some points.
 * The strategy for filtering nodes is to rank by the respective sum values and then take a certain number.
 */
public class APCrawler extends AbstractCrawler {
  private int maxReservedCount;
  private float percent;
  private boolean approximate = false;

  public APCrawler(Mlcs mlcs, LocationStore store, Limit limit, float percent, int maxReservedCount) {
    super(mlcs, store, limit);
    this.maxReservedCount = maxReservedCount;
    this.percent = percent;
  }

  public int calcReservedCount(int size) {
    return Math.max(maxReservedCount, (int) percent * size);
  }

  /**
   * Concurrent search the MLCS graph.
   */
  public Graph search() {
    this.currentLevel = 0;
    ArrayList<Location> fronts = new ArrayList<>();
    fronts.add(start.id);

    Location.ScoreSorter sorter = new Location.ScoreSorter(mlcs);
    ForkJoinPool pool = this.mlcs.newPool();
    while (!fronts.isEmpty()) {
      this.currentLevel = (short) (this.currentLevel + 1);
      if (this.currentLevel > limit.mlcsLength) limit.mlcsLength = this.currentLevel;
      // all nodes in this round
      HashMap<Location, Location> nodes = new HashMap<>();

      // step 1. search successors locations using multiple threaded
      // Each successor met the conditions for the maximum length assessment
      List<int[]> segs = Queues.split(fronts.size(), pool.getParallelism());
      LinkedList<ForkJoinTask<List<Links>>> tasks = new LinkedList<>();
      for (int[] seg : segs) {
        tasks.add(pool.submit(new SearchCrawler(this, fronts, seg[0], seg[1])));
      }
      for (ForkJoinTask<List<Links>> task : tasks) {
        List<Links> pairs = task.join();
        for (Links p : pairs) {
          for (Location l : p.nexts) {
            // don't drop the if,It avoids that key and value are not the same object.
            if (!nodes.containsKey(l)) nodes.put(l, l);
          }
        }
      }

      // step 2. traverse the new location using multiple threaded
      // find and mark 1)immediate successor,2) predecessors of end
      fronts = new ArrayList<>(nodes.keySet());
      segs = Queues.split(fronts.size(), pool.getParallelism());
      LinkedList<ForkJoinTask<Integer>> cleanTasks = new LinkedList<>();
      for (int[] seg : segs) {
        cleanTasks.add(pool.submit(new ImmediateCleaner(this.mlcs, fronts, nodes, seg[0], seg[1])));
      }
      int endPredecessorsCnt = 0;
      for (ForkJoinTask<Integer> task : cleanTasks) {
        endPredecessorsCnt += task.join();
      }

      //Is the last layer
      boolean finished = nodes.size() == endPredecessorsCnt;
      if (finished) {
        HashMap<Location, Location> last = new HashMap<>();
        for (Location l : nodes.keySet()) {
          l.setReserved(true);
          last.put(l, l);
        }
        locStore.add(this.currentLevel, last);
        break;
      }

      // step 3. Multi-thread dominant filtering was used to exclude an indirect successors.
      int immediateRemoveCnt = 0;
      int nonImmediateRemoveCnt = 0;
      if (fronts.size() > 1) {
//        Razor razor = new Razor(this.mlcs, this.currentLevel);
        Razor3 razor = new Razor3(this.mlcs,this.currentLevel);
        int[] rs = razor.shave(pool, fronts);
        // int[] rs = FirstOrderSorter.shave(this.minLevel,pool,fronts);
        immediateRemoveCnt = rs[0];
        nonImmediateRemoveCnt = rs[1];
      } else {
        for (Location loc : nodes.keySet()) {
          if (loc.isDiscard()) immediateRemoveCnt++;
        }
      }
      // step 4. Register the remaining points to the store
      int allNodeCnt = nodes.size();
      int removeCnt = immediateRemoveCnt + nonImmediateRemoveCnt;
      int reservedCount = calcReservedCount(allNodeCnt - removeCnt);

      if ((allNodeCnt - removeCnt) <= reservedCount) {
        nodes.entrySet().removeIf(entry -> entry.getKey().isDiscard());
        locStore.add(this.currentLevel, nodes);
        //System.out.println(this.minLevel + " remove nodes(" + allNodeCnt + " - " + removeCnt + "(" + immediateRemoveCnt + "+" + nonImmediateRemoveCnt + "))");
      } else {// sorting and filtering
        approximate = true;
        fronts.sort(sorter);
        int i = 0;
        for (Location l : fronts) {
          if (i < reservedCount) {
            if (!l.isDiscard()) i += 1;
          } else {
            l.setReserved(false);
          }
        }
        nodes.entrySet().removeIf(entry -> entry.getKey().isDiscard());
        locStore.add(this.currentLevel, nodes);
        System.out.print("\rmining level " + currentLevel + " " + (currentLevel * 100.0 / limit.mlcsLength) + "% ");
      }
    }
    pool.shutdown();
    //restore the graph from back to forward.
    Graph graph = locStore.restore(this.currentLevel);
    System.out.println("\rmining complete.");
    return graph;
  }

  public boolean isApproximate() {
    return approximate;
  }
}
