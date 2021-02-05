import com.spire.ms.System.Collections.IEnumerator;
import com.spire.xls.CellRange;
import com.spire.xls.Workbook;
import com.spire.xls.Worksheet;
import com.spire.xls.collections.WorksheetsCollection;
import com.spire.xls.core.IXLSRange;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomPathSimulator {

    /**
     * 基础空白地图文件
     */
    private static final String BASE_MAP_FILE_PATH = "E:\\A_DepoManager\\trunk\\doc\\map-base.xlsx";

    private static final String OUT_FILE_PATH = "result-sim.xlsx";

    private static final int RANDOM_TRY_COUNT = 500;

    private static final int RANDOM_WIDTH_HEIGHT = 30;

    public static void main(String[] args) {
        RandomPathSimulator simulator = new RandomPathSimulator();
        Point mapSrc = new Point(1, 1);
        Point mapDst = new Point(93, 50);
        int minSpace = 3;
        simulator.run(mapSrc, mapDst, minSpace, 1, 50);
    }

    /**
     * @param mapSrc   地图边界左上角
     * @param mapDst   地图边界右下角
     * @param minSpace 禁行区最小间隔
     * @param offset   路径与禁行区偏移量
     * @param outCount 总模拟次数
     */
    public void run(Point mapSrc, Point mapDst, int minSpace, int offset, int outCount) {
        /*if (offset * 2 + 1 > minSpace) {
            System.out.println("禁行区间隔不足，路径规划失败");
            return;
        }*/
        Workbook wb = new Workbook();
        wb.loadFromFile(BASE_MAP_FILE_PATH);
        Worksheet baseMapSheet = loadBaseMapSheet(wb);
        copyBaseMapSheet(wb, baseMapSheet, outCount - 1);
        WorksheetsCollection worksheets = wb.getWorksheets();
        IEnumerator iterator = worksheets.iterator();
        // 开始模拟
        while (iterator.hasNext()) {
            Point[] randomSrcDstPoint = getRandomSrcDstPoint(mapSrc, mapDst);
            Point pathSrc = randomSrcDstPoint[0];
            Point pathDst = randomSrcDstPoint[1];
            Worksheet sheet = (Worksheet) iterator.next();
            // 绘制边界
            DrawPathUtil.fillBg(sheet.getCellRange((int) mapSrc.getY(), (int) mapSrc.getX()), Color.blue);
            DrawPathUtil.fillBg(sheet.getCellRange((int) mapDst.getY(), (int) mapDst.getX()), Color.blue);

            PathPlanner pathPlanner = new PathPlanner(mapSrc, mapDst, offset);
            // 添加随机禁行区
            List<ForbiddenZone> forbiddenZones = addRandomZone(mapSrc, mapDst, pathSrc, pathDst, minSpace, offset);
            for (int i = 0; i < forbiddenZones.size(); i++) {
                pathPlanner.addForbiddenZone("zone-" + i, forbiddenZones.get(i));
            }
            pathPlanner.getForbiddenZoneMap().forEach((name, zone) -> {
                if (zone.isEnable()) {
                    DrawPathUtil.fillBg(sheet.getCellRange(zone.getyMin(), zone.getxMin(), zone.getyMax(), zone.getxMax()), Color.red);
                } else {
                    DrawPathUtil.fillBg(sheet.getCellRange(zone.getyMin(), zone.getxMin(), zone.getyMax(), zone.getxMax()), Color.green);
                }
            });
            // 开始路径规划
            try {
                List<Point> path = pathPlanner.getPath(pathSrc, pathDst);
                // 画点线
                DrawPathUtil.drawPathLine(path, sheet);
                DrawPathUtil.drawPathPoint(path, sheet);
            } catch (Exception e) {
                // 画起点终点 test
                DrawPathUtil.fillBg(sheet.getCellRange((int) pathSrc.getY(), (int) pathSrc.getX()), Color.black);
                DrawPathUtil.fillBg(sheet.getCellRange((int) pathDst.getY(), (int) pathDst.getX()), Color.black);
                sheet.setName(sheet.getName() + " <X>");
            }
            // 输入代码
            writeSimCode(sheet.get(1, 2), mapSrc, mapDst, pathSrc, pathDst, forbiddenZones);
        }
        wb.saveToFile(OUT_FILE_PATH);
        wb.dispose();
        DrawPathUtil.openFile(OUT_FILE_PATH);
    }

    private static Worksheet loadBaseMapSheet(Workbook wb) {
        return wb.getWorksheets().get(0);
    }

    private static void copyBaseMapSheet(Workbook wb, Worksheet baseMapSheet, int count) {
        while (count-- > 0) {
            Worksheet emptySheet = wb.createEmptySheet(String.format("result-%s", count + 1));
            emptySheet.copyFrom(baseMapSheet);
        }
    }

    private static List<ForbiddenZone> addRandomZone(Point mapSrc, Point mapDst, Point pathSrc, Point pathDst, int minSpace, int offset) {
        int mapXMin = (int) mapSrc.getX() + minSpace;
        int mapYMin = (int) mapSrc.getY() + minSpace;
        int mapXMax = (int) mapDst.getX() - minSpace;
        int mapYMax = (int) mapDst.getY() - minSpace;
        Random random = new Random();
        List<ForbiddenZone> forbiddenZones = new ArrayList<>();
        for (int i = 0; i < RANDOM_TRY_COUNT; i++) {
            int xMin = mapXMin + random.nextInt(mapXMax - mapXMin);
            int yMin = mapYMin + random.nextInt(mapYMax - mapYMin);
            int xMax = Math.min(xMin + random.nextInt(RANDOM_WIDTH_HEIGHT) + 1, mapXMax);
            int yMax = Math.min(yMin + random.nextInt(RANDOM_WIDTH_HEIGHT) + 1, mapYMax);
            ForbiddenZone forbiddenZone = new ForbiddenZone(xMin, yMin, xMax, yMax);
            if (checkOverlap(forbiddenZones, forbiddenZone, minSpace) && checkPointInZone(forbiddenZone, pathSrc, offset) && checkPointInZone(forbiddenZone, pathDst, offset)) {
                forbiddenZones.add(forbiddenZone);
            }
        }
        return forbiddenZones;
    }

    /**
     * 检查禁行区是否重叠
     *
     * @param allForbiddenZones
     * @param forbiddenZone
     * @param minSpace
     * @return
     */
    private static boolean checkOverlap(List<ForbiddenZone> allForbiddenZones, ForbiddenZone forbiddenZone, int minSpace) {
        for (ForbiddenZone zone : allForbiddenZones) {
            if (zone.isZoneOverlap(forbiddenZone, minSpace)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断区域是否覆盖扩展点区域
     *
     * @param forbiddenZone
     * @param point
     * @param offset
     * @return
     */
    private static boolean checkPointInZone(ForbiddenZone forbiddenZone, Point point, int offset) {
        ForbiddenZone pointZone = new ForbiddenZone((int) point.getX(), (int) point.getY(), (int) point.getX(), (int) point.getY());
        return !forbiddenZone.isZoneOverlap(pointZone, offset);
    }

    private static Point[] getRandomSrcDstPoint(Point mapSrc, Point mapDst) {
        int mapXMin = (int) mapSrc.getX();
        int mapYMin = (int) mapSrc.getY();
        int mapXMax = (int) mapDst.getX();
        int mapYMax = (int) mapDst.getY();
        Random random = new Random();
        int tryCount = 0;
        int baseLineLength;
        Point pathDst;
        Point pathSrc;
        do {
            pathSrc = new Point(mapXMin + 1 + random.nextInt(mapXMax - mapXMin - 1), mapYMin + 1 + random.nextInt(mapYMax - mapYMin - 1));
//            if (tryCount++ > 1000) {
//                System.out.println("终点创建失败");
//                System.exit(0);
//            }
            pathDst = new Point(mapXMin + 1 + random.nextInt(mapXMax - mapXMin - 1), mapYMin + 1 + random.nextInt(mapYMax - mapYMin - 1));
            if (pathDst.getX() == pathSrc.getX()) {
                baseLineLength = (int) Math.abs(pathDst.getY() - pathSrc.getY());
            } else {
                double v = Math.pow((pathDst.getY() - pathSrc.getY()), 2) + Math.pow((pathDst.getX() - pathSrc.getX()), 2);
                baseLineLength = (int) Math.pow(v, 0.5);
            }
        } while (Math.abs(mapDst.getX() - mapSrc.getX()) * 2 / 3 > baseLineLength);
        return new Point[]{pathSrc, pathDst};
    }

    public static void writeSimCode(IXLSRange cell, Point a, Point b, Point src, Point dst, List<ForbiddenZone> forbiddenZones) {
        String mapTemp = String.format("Point a = new Point(%d,%d);\nPoint b = new Point(%d,%d);\nPoint src = new Point(%d,%d);\nPoint dst = new Point(%d,%d);\n",
                (int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY(), (int) src.getX(), (int) src.getY(), (int) dst.getX(), (int) dst.getY());
        String zoneCodeTemp = "FORBIDDEN_ZONES.add(new ForbiddenZone(%d,%d,%d,%d));\n";
        StringBuilder stringBuilder = new StringBuilder(mapTemp);
        for (ForbiddenZone zone : forbiddenZones) {
            stringBuilder.append(String.format(zoneCodeTemp, zone.getxMin(), zone.getyMin(), zone.getxMax(), zone.getyMax()));
        }
        cell.setText(stringBuilder.toString());
        cell.setRowHeight(DrawPathUtil.HEIGHT_UNIT);
    }

}
