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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileSearcher {

  public static String getFileShortName(File file) {
    String fileName = file.getName();
    int extensionIdx = fileName.lastIndexOf(".");
    if (extensionIdx == -1) {
      return fileName;
    } else {
      return fileName.substring(0, extensionIdx);
    }
  }

  /**
   * find all files prefixed with name under the specified path
   */
  public static List<File> find(String pathPattern) {
    String path = pathPattern.replace('/', File.separatorChar);
    path = path.replace('/', File.separatorChar);
    int sepIdx = path.lastIndexOf(File.separatorChar);
    File dir = (sepIdx == -1) ? new File(".") : new File(path.substring(0, sepIdx));
    String namePattern = path.substring(sepIdx + 1);
    List<File> filePathList = new ArrayList<File>();
    File[] files = dir.listFiles();
    AntPathPattern pattern = null;
    if (namePattern.contains("?") || namePattern.contains("*")) pattern = new AntPathPattern(namePattern);
    for (File f : files) {
      if (f.isFile()) {
        if (null == pattern) {
          if (f.getName().startsWith(namePattern)) filePathList.add(f);
        } else {
          if (pattern.match(f.getName())) filePathList.add(f);
        }
      }
    }
    return filePathList;
  }

  public static String getOutFile(File sourceFile, String outFileName) throws IOException {
    String sep = File.separator;
    String path = new File(sourceFile.getParent() + sep + ".." + sep + "out" + sep).getCanonicalPath();
    return path + sep + outFileName;
  }
}
