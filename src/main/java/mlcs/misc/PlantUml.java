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

import mlcs.Graph;
import mlcs.Location;
import mlcs.Mlcs;
import mlcs.Node;

import java.util.*;

public class PlantUml {

  final Graph graph;
  final Mlcs mlcs;

  public PlantUml(Graph graph) {
    this.graph = graph;
    this.mlcs = graph.mlcs;
  }

  public String umltexts() {
    int level = (graph.maxLevel + 1);
    Collection<Node> levelNodes = new ArrayList<>();
    levelNodes.add(graph.end);
    HashSet<Location> allLocs = new HashSet<>();
    StringBuilder rules = new StringBuilder();
    while (level > 0) {
      HashMap<Location, Node> pres = new HashMap<>();
      for (Node n : levelNodes) {
        allLocs.add(n.id);
        Map<Location, Node> ps = graph.getPredecessors(n, n.level);
        for (Node p : ps.values()) {
          rules.append(identifier(mlcs, p.id) + " --> " + identifier(mlcs, n.id) + "\n");
          pres.put(p.id, p);
        }
      }
      levelNodes = pres.values();
      level -= 1;
    }
    StringBuilder alias = new StringBuilder();
    allLocs.remove(graph.end.id);
    for (Location loc : allLocs) {
      alias.append("state \"" + mlcs.charAt(loc) + "\\n" + loc.toString() + "\" as ").append(identifier(mlcs, loc)).append("\n");
    }

    String[] headers = new String[]{"", "To generate a graph,copy texts under this line to http://www.plantuml.com/plantuml/uml.",
        "@startuml", "hide empty description"};//"!theme reddress-lightgreen"
    StringBuilder contents = new StringBuilder();
    for (String header : headers) {
      contents.append(header).append("\n");
    }
    if (graph.maxLevel * 100 > 4000) {
      contents.append("scale 4000/" + graph.maxLevel * 100 + "\n");
    }
    contents.append(alias.toString());
    contents.append(rules.toString());
    contents.append("@enduml");
    return contents.toString();
  }

  private String identifier(Mlcs mlcs, Location n) {
    short[] index = n.index;
    if (index[0] == 0 || index[0] > mlcs.maxLength) {
      return "[*]";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(mlcs.charAt(n));
    for (short a : index) {
      sb.append(a).append("_");
    }
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }
}
