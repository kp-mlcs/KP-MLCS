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

/**
 * Sort data by multiple objective
 */
public class MultipleObjectiveRanker {
  // local variables
  private final int[] helper;
  private final short[][] datas;
  private final int n;
  private final short m;
  private short obj;
  private final int[][] rank;
  private final int[] lex_order;

  public MultipleObjectiveRanker(short[][] datas) {
    this.datas = datas;
    this.n = datas.length;
    this.m = (short) datas[0].length;
    helper = new int[n];
    rank = new int[n][m];

    for (short j = 0; j < m; j++) {
      for (int i = 0; i < n; i++) {
        rank[i][j] = i;
      }
    }
    lex_order = new int[n];
  }

  /**
   * Sorting population by each objective
   */
  public int[][] sorting() {
    this.obj = 0;
    mergesort(0, n - 1);
    for (int i = 1; i < n; i++) {
      lex_order[rank[i][0]] = i;
    }
    for (short j = 1; j < m; j++) {
      this.obj = j;
      mergesort_obj(0, n - 1);
    }
    return rank;
  }

  private void mergesort(int low, int high) {
    if (low < high) {
      int middle = low + (high - low) / 2;
      mergesort(low, middle);
      mergesort(middle + 1, high);
      merge(low, middle, high);
    }
  }

  private void merge(int low, int middle, int high) {
    // Copy both parts into the helper array
    for (int i = low; i <= high; i++) {
      helper[i] = rank[i][obj];
    }

    int i = low;
    int j = middle + 1;
    int k = low;
    while (i <= middle && j <= high) {
      if (datas[helper[i]][obj] < datas[helper[j]][obj]) {
        rank[k][obj] = helper[i];
        i++;
      } else if (datas[helper[i]][obj] > datas[helper[j]][obj]) {
        rank[k][obj] = helper[j];
        j++;
      } else {// two values are equal
        boolean check = lex_compare(helper[i], helper[j]);
        if (check) {
          rank[k][obj] = helper[i];
          i++;
        } else {
          rank[k][obj] = helper[j];
          j++;
        }
      }
      k++;
    }
    while (i <= middle) {
      rank[k][obj] = helper[i];
      k++;
      i++;
    }
    while (j <= high) {
      rank[k][obj] = helper[j];
      k++;
      j++;
    }
  }

  private void mergesort_obj(int low, int high) {
    if (low < high) {
      int middle = low + (high - low) / 2;
      mergesort_obj(low, middle);
      mergesort_obj(middle + 1, high);
      merge_obj(low, middle, high);
    }
  }

  private void merge_obj(int low, int middle, int high) {
    for (int i = low; i <= high; i++) {
      helper[i] = rank[i][obj];
    }
    int i = low;
    int j = middle + 1;
    int k = low;
    while (i <= middle && j <= high) {
      if (datas[helper[i]][obj] < datas[helper[j]][obj]) {
        rank[k][obj] = helper[i];
        i++;
      } else if (datas[helper[i]][obj] > datas[helper[j]][obj]) {
        rank[k][obj] = helper[j];
        j++;
      } else {// two values are equal
        if (lex_order[helper[i]] < lex_order[helper[j]]) {
          rank[k][obj] = helper[i];
          i++;
        } else {
          rank[k][obj] = helper[j];
          j++;
        }
      }
      k++;
    }
    while (i <= middle) {
      rank[k][obj] = helper[i];
      k++;
      i++;
    }
    while (j <= high) {
      rank[k][obj] = helper[j];
      k++;
      j++;
    }
  }

  public boolean lex_compare(int p1, int p2) {
    for (int i = 0; i < m; i++) {
      if (datas[p1][i] < datas[p2][i]) return true;
      else if (datas[p1][i] > datas[p2][i]) return false;
    }
    return true;
  }
}
