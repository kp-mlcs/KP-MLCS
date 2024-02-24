package mlcs.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class Env {
  public static void print() {
    var osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    // 获取操作系统名称
    System.out.println("操作系统：" + System.getProperty("os.name"));
    // 获取CPU核心数
    System.out.println("CPU核心数：" + osBean.getAvailableProcessors());
    // 获取CPU架构
    System.out.println("CPU架构：" + System.getProperty("os.arch"));
    System.out.println("内存:" + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB");
    System.out.println("Processors:" + Runtime.getRuntime().availableProcessors());
  }
}
