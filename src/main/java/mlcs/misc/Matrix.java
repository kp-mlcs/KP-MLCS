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

import java.util.HashMap;
import java.util.Map;

public class Matrix {
  Folder root = new Folder(true);

  long size = 0;

  /**
   * 是否包含该点
   *
   * @param id
   * @return
   */
  public boolean exist(Location id) {
    int d = 0;
    Folder folder = root;
    short[] index = id.index;
    int lastIdx = index.length - 1;
    while (d <= lastIdx) {
      folder = folder.get(index[d]);
      if (folder == null) return false;
    }
    return false;
  }

  public boolean put(Location id) {
    short[] index = id.index;
    Folder cur = root;
    int len = index.length;
    int last = index.length - 1;
    for (int i = 0; i < len; i++) {
      if (i < last) {
        cur = cur.getOrCreate(index[i], i < last);
      } else {
        if(null == cur.hold(index[i])){
          size++;
          return true;
        }else{
          return false;
        }
      }
    }
    return false;
  }

  private static class Folder {
    static Folder placeholder = new Folder(false);
    Map<Short, Folder> children;

    Folder(boolean createMap) {
      if (createMap) children = new HashMap<>();
    }

    public Folder hold(short idx) {
      return children.put(idx, placeholder);
    }

    public Folder getOrCreate(short idx, boolean initMap) {
      Folder folder = children.get(idx);
      if (null == folder) {
        folder = new Folder(initMap);
        children.put(idx, folder);
        return folder;
      } else {
        return folder;
      }
    }

    public Folder get(short idx) {
      return children.get(idx);
    }

    public boolean contains(short idx) {
      return children.containsKey(idx);
    }

  }
}
