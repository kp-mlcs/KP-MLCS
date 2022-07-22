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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Queues {

  public static List<int[]> split(int total, int n) {
    List<int[]> buffer = new ArrayList<int[]>();
    if (total <= n) {
      buffer.add(new int[] { 0, total });
    } else {
      int sliceCount = (total / n);
      int i = 0;
      while (i < n - 1) {
        buffer.add(new int[] { i * sliceCount, (i + 1) * sliceCount });
        i += 1;
      }
      buffer.add(new int[] { (n - 1) * sliceCount, total });
    }
    return buffer;
  }

  @SuppressWarnings("unchecked")
  public static <A> LinkedList<A>[] split(LinkedList<A> data, int n) {
    if (data.size() <= n) {
      LinkedList<A> rs = new LinkedList<A>(data);
      data.clear();
      return new LinkedList[] { rs };
    } else {
      LinkedList<A>[] datas = new LinkedList[n];
      for (int i = 0; i < n; i++) {
        datas[i] = new LinkedList<A>();
      }
      int total = data.size();
      for (int i = 0; i < total; i++) {
        datas[i % n].addLast(data.removeFirst());
      }
      return datas;
    }
  }

  public static List<int[]> split(List<?> locations, int parallelism, int threshhold) {
    if (locations.size() < threshhold) {
      return Queues.split(locations.size(), locations.size());
    } else {
      return Queues.split(locations.size(), parallelism);
    }
  }
}
