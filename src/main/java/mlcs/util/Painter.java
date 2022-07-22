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

import mlcs.Graph;
import mlcs.Location;
import mlcs.Node;
import mlcs.Result;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Display the mlcs result using a dependency graph.
 */
public class Painter {

  /**
   * Visualize the result in frame
   * @param result
   */
  public static void visualize(Result result) {
    Graph graph = result.graph;
    int height = (graph.height() - 1) * 60 + 20 + 30;// node + margin
    int width = graph.maxLevel * 60 + 20;// node
    int captionHeight = 22;
    int miscHeight = 40;//title
    int scrollWidth = 15;

    int frameWidth = width + scrollWidth;
    int frameHeight = height + captionHeight + miscHeight;
    int graphPaneWidth = frameWidth;
    int graphPaneHeight = frameHeight - miscHeight - captionHeight;

    boolean needScollPanel = false;
    Dimension dimMax = Toolkit.getDefaultToolkit().getScreenSize();
    if (frameWidth < 300) frameWidth = 300;
    else if (frameWidth > dimMax.width) {
      frameWidth = dimMax.width;
      graphPaneWidth = frameWidth - scrollWidth;
      needScollPanel = true;
    }
    if (frameHeight > dimMax.height - 25) {
      frameHeight = dimMax.height - 25;//windows bar
      graphPaneHeight = frameHeight - miscHeight - captionHeight;
      needScollPanel = true;
    }
    JFrame frame = new JFrame();
    frame.setLayout(null);
    frame.setLocationByPlatform(true);
    frame.setBounds(0, 0, frameWidth, frameHeight); //titlebar and scrollbar
    //caption
    JPanel captionPanel = new CaptionPanel(result);
    captionPanel.setBounds(0, 0, 500, captionHeight);
    frame.getContentPane().add(captionPanel);

    TreePanel panel1 = new TreePanel(result, !needScollPanel);
    panel1.setPreferredSize(new Dimension(width, height));

    JScrollPane sp = null;
    if (needScollPanel) {
      JScrollPane scrollPane = new JScrollPane(panel1);   //window sliding
      sp = scrollPane;
      scrollPane.setBounds(0, captionHeight, graphPaneWidth, graphPaneHeight);
      frame.getContentPane().add(scrollPane);
    } else {
      panel1.setBounds(0, captionHeight, graphPaneWidth, graphPaneHeight);
      frame.getContentPane().add(panel1);
    }

    frame.setTitle("MLCS Result Visualizer");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    //frame.setResizable(needScollPanel);
    frame.setVisible(true);
    if (null != sp) {
      BoundedRangeModel a = sp.getVerticalScrollBar().getModel();
      sp.getVerticalScrollBar().setValue(a.getMaximum() / 4);
    }
  }

  /**
   * The CaptionPannel describes mlcs count and maxLength
   */
  static class CaptionPanel extends JPanel {
    private Result result;

    public CaptionPanel(Result result) {
      this.result = result;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graph graph = result.graph;
      g.drawString("Find " + result.mlcsCount + " mlcs (" + graph.maxLevel + ") in " +
        graph.mlcs.seqs.size() + " sequences (length " + graph.mlcs.maxLength + ") within " + result.getTime(), 5, 20);
    }
  }

  /**
   * The TreePannel contains the whole key nodes of mlcs and connections between them.
   */
  static class TreePanel extends JPanel {
    private int nodeHeight = 20; // the height of each node
    private int hGap = 60; // horizontal distance of every two nodes
    private int vGap = 60; // vertical distance of every two nodes

    private boolean painted = false;
    private boolean drawHr;
    private Result result;
    private Font nodeFont = new Font("Microsoft YaHei", Font.BOLD, 8); // font of node
    private Font locFont = new Font("Microsoft YaHei", Font.PLAIN, 8); // font of node location

    public TreePanel(Result result, boolean drawHr) {
      this.result = result;
      this.drawHr = drawHr;
    }

    // draw
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      drawNodes(g);
    }

    /**
     * Draw key nodes level by level.
     * @param g
     */
    public void drawNodes(Graphics g) {
      Graph graph = result.graph;
      if (drawHr) g.drawLine(2, 2, getWidth(), 2);
      int startY = getHeight() / 2;
      HashMap<Location, Integer> preIndexes = new HashMap<>();
      for (short layer = 1; layer <= graph.maxLevel; layer++) {
        //sort loc by score
        Node.SumSorter sorter = new Node.SumSorter(layer);
        ArrayList<Node> layerNodes = new ArrayList<>(graph.nodes[layer].values());
        Collections.sort(layerNodes, sorter);
        HashMap<Location, Integer> indexes = new HashMap<>();
        int i = 0;
        for (Node n : layerNodes) {
          indexes.put(n.id, i++);
        }
        i = 0;
        int preX = (layer - 1) * hGap - 30;
        for (Node ln : layerNodes) {
          Location loc = ln.id;
          int x = layer * hGap - 30;
          int y = startY + (i++) * vGap - ((indexes.size() - 1) * vGap / 2 + 10);
          int fontY = y + nodeHeight - 5;
          g.setColor(Color.lightGray);
          g.setFont(nodeFont);
          g.fillOval(x, y, 20, 20);
          g.setColor(Color.BLACK);
          g.drawString(String.valueOf(graph.mlcs.charAt(loc)), x + 7, fontY);
          g.setFont(locFont);
          String locstr = loc.toString();
          if (locstr.length() > 10) {
            locstr = locstr.substring(0, 10) + "..)";
          }
          g.drawString(locstr, x - 5, fontY + 15);

          int preSize = preIndexes.size();
          for (Location preLoc : graph.nodes[layer].get(loc).pres) {
            if (preIndexes.containsKey(preLoc)) {
              int k = preIndexes.get(preLoc);
              Color color = (k == 0 && i == 1) ? Color.red : Color.black;
              drawArrowLine(g, preX + 20, startY + k * vGap - ((preSize - 1) * vGap / 2 + 10) + 10, x, y + 10, color);
            }
          }
        }
        preIndexes = indexes;
      }
    }

    /**
     * Draw a line with arrow from(x1,y1) to (x2,y2).
     * @param g
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param color
     */
    private void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, Color color) {
      int d = 5;
      int h = 3;
      int dx = x2 - x1, dy = y2 - y1;
      double D = Math.sqrt(dx * dx + dy * dy);
      double xm = D - d, xn = xm, ym = h, yn = -h, x;
      double sin = dy / D, cos = dx / D;

      x = xm * cos - ym * sin + x1;
      ym = xm * sin + ym * cos + y1;
      xm = x;

      x = xn * cos - yn * sin + x1;
      yn = xn * sin + yn * cos + y1;
      xn = x;

      int[] xpoints = {x2, (int) xm, (int) xn};
      int[] ypoints = {y2, (int) ym, (int) yn};

      Color originColor = g.getColor();
      g.setColor(color);
      g.drawLine(x1, y1, x2, y2);
      g.fillPolygon(xpoints, ypoints, 3);
      if (null != originColor) g.setColor(originColor);
    }
  }
}
