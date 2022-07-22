package mlcs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Razor3Test {

  public static void main(String[] args) {
    List<Location> locations = new ArrayList<>();
    List<short[]> points = List.of(new short[]{20, 19, 14}, new short[]{16, 18, 18},
      new short[]{16, 16, 17}, new short[]{17, 15, 15}, new short[]{19, 18, 17},
      new short[]{20, 19, 17}, new short[]{20, 18, 17});
    for (int i = 0; i < points.size(); i++) {
      locations.add(new Location(points.get(i)));
    }
    Set<Location> keyLocs = IndexTree.findKeyLocs(locations);
    System.out.println(keyLocs);
  }
}
