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
package mlcs.misc;

import mlcs.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;

/**
 * 查找第一级的点
 * 该算法改良自best-order-sorter但是发现性能不能满足要求，没有使用
 */
public class FirstOrderSorter {
  short datas[][];
  int[][] allrank;
  MultipleObjectiveRanker sorter;
  int[] rank;
  ArrayList<Integer>[] list; //rank0[j]

  int n;
  int m;

  private void initialize(short[][] datas) {
    this.datas = datas;
    n = this.datas.length;
    m = this.datas[0].length;
    sorter = new MultipleObjectiveRanker(datas);
  }

  public void first_sort() {
    int n = this.n;
    int m = this.m;
    int i, j, total = 0;

    rank = new int[n];
    boolean[] set = new boolean[n];
    list = new ArrayList[m];

    for (j = 0; j < m; j++) {
      list[j] = new ArrayList<>();
    }

    allrank = sorter.sorting();
    for (i = 0; i < n; i++) {
      for (short obj = 0; obj < m; obj++) {
        int s = allrank[i][obj];
        if (set[s]) {// s is already ranked
          if (rank[s] == 0) {//just collect rank 0
            list[obj].add(s);
          }
          continue;
        }
        set[s] = true;
        total++;
        boolean dominated = false;
        for (Integer h : list[obj]) {
          if (dominates(h, s)) {
            dominated = true;
            break;
          }
        }
        if (!dominated) {// not dominated
          list[obj].add(s);
          rank[s] = 0;
        } else {// dominated
          rank[s] = 1;
        }
      }
      if (total == n) break;
    }
  }

  /**
   * 强支配检查
   * 是由于相同维护不认为是强支配，因此如果采取了缩小比较维度，会导致错误的判断。例如
   * {2,3,4} 不支配{5,3,4}.如果再第二、三维度比较完后，{2,3,4}仅仅剩余第一个维度，而该维度则是强制配关系，因为2<5。
   */
  public boolean dominates(int p1, int p2) {
    for (int i = 0; i < m; i++) {
      if (datas[p1][i] >= datas[p2][i]) return false;
    }
    return true;
  }


  public static int[] shave(short level, ForkJoinPool pool, ArrayList<Location> locs) {
    long startTime = System.currentTimeMillis();
    FirstOrderSorter sort = new FirstOrderSorter();
    int disabled = 0;
    for (Location id : locs) {
      if (id.isDiscard()) disabled++;
    }
    int n = locs.size() - disabled;
    short[][] population = new short[n][];
    int i = 0;
    HashMap<Integer, Integer> indexes = new HashMap<>();
    for (int j = 0; j < locs.size(); j++) {
      Location loc = locs.get(j);
      if (!loc.isDiscard()) {
        population[i] = loc.index;
        indexes.put(i, j);
        i++;
      }
    }
    sort.initialize(population);
    sort.first_sort();// change this value to m1 = log(n) when m is very high

    int marked = 0;
    for (i = 0; i < n; i++) {
      if (sort.rank[i] != 0) {
        locs.get(indexes.get(i)).setReserved(false);
        marked++;
      } else {
        locs.get(indexes.get(i)).setReserved(true);
      }
    }
    System.out.println(level + " sort(" + (locs.size() - disabled) + "-" + marked + ") using " + (System.currentTimeMillis() - startTime));
    return new int[]{disabled, marked};
  }
}
