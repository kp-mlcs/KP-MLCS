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
