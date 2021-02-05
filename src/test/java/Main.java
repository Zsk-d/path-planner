
import com.spire.xls.*;
import com.spire.xls.core.IPrstGeomShape;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    private static List<ForbiddenZone> FORBIDDEN_ZONES = new ArrayList<>();

    private static void initMap2() {
        FORBIDDEN_ZONES.add(new ForbiddenZone(10, 6, 20, 16));
        FORBIDDEN_ZONES.add(new ForbiddenZone(29, 6, 39, 16));
        FORBIDDEN_ZONES.add(new ForbiddenZone(48, 6, 58, 16));
        FORBIDDEN_ZONES.add(new ForbiddenZone(67, 6, 77, 16));

        FORBIDDEN_ZONES.add(new ForbiddenZone(10, 25, 20, 35));
        FORBIDDEN_ZONES.add(new ForbiddenZone(29, 25, 39, 35));
        FORBIDDEN_ZONES.add(new ForbiddenZone(48, 25, 58, 35));
        FORBIDDEN_ZONES.add(new ForbiddenZone(67, 25, 77, 35));

        FORBIDDEN_ZONES.add(new ForbiddenZone(10, 44, 20, 54));
        FORBIDDEN_ZONES.add(new ForbiddenZone(29, 44, 39, 54));
        FORBIDDEN_ZONES.add(new ForbiddenZone(48, 44, 58, 54));
        FORBIDDEN_ZONES.add(new ForbiddenZone(67, 44, 77, 54));
    }

    private static void initMap1() {
        FORBIDDEN_ZONES.add(new ForbiddenZone(13, 7, 16, 31));
        FORBIDDEN_ZONES.add(new ForbiddenZone(39, 27, 46, 49));
        FORBIDDEN_ZONES.add(new ForbiddenZone(52, 14, 61, 41));
        FORBIDDEN_ZONES.add(new ForbiddenZone(25, 11, 33, 41));
        FORBIDDEN_ZONES.add(new ForbiddenZone(41, 6, 45, 20));
        FORBIDDEN_ZONES.add(new ForbiddenZone(79, 8, 90, 43));

        FORBIDDEN_ZONES.add(new ForbiddenZone(67, 20, 72, 48));
        FORBIDDEN_ZONES.add(new ForbiddenZone(11, 33, 19, 36));
    }

    private static void initMap3() {
        FORBIDDEN_ZONES.add(new ForbiddenZone(6, 41, 15, 47));
        FORBIDDEN_ZONES.add(new ForbiddenZone(72, 35, 85, 36));
        FORBIDDEN_ZONES.add(new ForbiddenZone(36, 14, 52, 37));
        FORBIDDEN_ZONES.add(new ForbiddenZone(12, 23, 21, 30));
        FORBIDDEN_ZONES.add(new ForbiddenZone(20, 43, 42, 47));
        FORBIDDEN_ZONES.add(new ForbiddenZone(82, 40, 83, 47));
        FORBIDDEN_ZONES.add(new ForbiddenZone(83, 6, 90, 22));
        FORBIDDEN_ZONES.add(new ForbiddenZone(13, 5, 22, 13));
        FORBIDDEN_ZONES.add(new ForbiddenZone(58, 41, 75, 44));
        FORBIDDEN_ZONES.add(new ForbiddenZone(89, 45, 90, 47));
        FORBIDDEN_ZONES.add(new ForbiddenZone(32, 4, 49, 10));
        FORBIDDEN_ZONES.add(new ForbiddenZone(60, 9, 64, 20));
        FORBIDDEN_ZONES.add(new ForbiddenZone(59, 24, 73, 28));
        FORBIDDEN_ZONES.add(new ForbiddenZone(4, 15, 5, 29));
        FORBIDDEN_ZONES.add(new ForbiddenZone(47, 44, 53, 46));
        FORBIDDEN_ZONES.add(new ForbiddenZone(88, 28, 90, 30));
        FORBIDDEN_ZONES.add(new ForbiddenZone(68, 9, 70, 18));
        FORBIDDEN_ZONES.add(new ForbiddenZone(6, 5, 9, 9));
        FORBIDDEN_ZONES.add(new ForbiddenZone(29, 16, 31, 19));
    }


    public static void main(String[] args) throws Exception {
        initMap3();
        Point a = new Point(1, 1);
        Point b = new Point(93, 50);
        Point src = new Point(11, 16);
        Point dst = new Point(73, 46);
        PathPlanner pathPlanner = new PathPlanner(a, b, 1);
        for (int i = 0; i < FORBIDDEN_ZONES.size(); i++) {
            ForbiddenZone zone = FORBIDDEN_ZONES.get(i);
            pathPlanner.addForbiddenZone("fz-" + i, zone);
        }
        List<Point> path = pathPlanner.getPath(src, dst);
        printPath(path, a, b);
    }

    private static void printPath(List<Point> path, Point a, Point b) {
        String mapPath = "E:\\A_DepoManager\\trunk\\doc\\map-base.xlsx";
        Workbook wb = new Workbook();
        wb.loadFromFile(mapPath);
        // 获取工作表
        Worksheet sheet = wb.getWorksheets().get(0);
        // 绘制全区域边框
        CellRange cellRange = sheet.getCellRange((int) a.getY(), (int) a.getX());
        cellRange.getBorders().setLineStyle(LineStyleType.Double);
        cellRange.getBorders().setColor(Color.BLUE);

        CellRange cellRange1 = sheet.getCellRange((int) b.getY(), (int) b.getX());
        cellRange1.getBorders().setLineStyle(LineStyleType.Double);
        cellRange1.getBorders().setColor(Color.BLUE);

        // 绘制安全区
        FORBIDDEN_ZONES.forEach(zone -> {
            if (zone.isEnable()) {
                fillBg(sheet.getCellRange(zone.getyMin(), zone.getxMin(), zone.getyMax(), zone.getxMax()), Color.red);
            } else {
                fillBg(sheet.getCellRange(zone.getyMin(), zone.getxMin(), zone.getyMax(), zone.getxMax()), Color.green);
            }
        });
        drawPathLine(path, sheet);
        drawPathPoint(path, sheet);
        // 添加单独点
        /*List<Point> drawPoints = new ArrayList<>();
        Point temp = null;
        for (Point point : path) {
            if (temp != null) {
                if (temp.getX() == point.getX()) {
                    for (int i = 1; i < Math.abs((int) temp.getY() - (int) point.getY()); i++) {
                        drawPoints.add(new Point((int) temp.getX(), Math.min((int) temp.getY(), (int) point.getY()) + i));
                    }
                } else if (temp.getY() == point.getY()) {
                    for (int i = 1; i < Math.abs(temp.getX() - point.getX()); i++) {
                        drawPoints.add(new Point(Math.min((int) temp.getX(), (int) point.getX()) + i, (int) temp.getY()));
                    }
                }
            }
            temp = point;
            drawPoints.add(point);
        }*/
        // 绘制其他点
        /*drawPoints.forEach(point -> fillBg(sheet.getCellRange((int) point.getY(), (int) point.getX()), Color.black));*/
        String filePath = "result.xlsx";
        wb.saveToFile(filePath);
        wb.dispose();
        openFile(filePath);
    }

    private static void drawPathPoint(List<Point> path, Worksheet sheet) {
        path.forEach(point -> fillBg(sheet.getCellRange((int) point.getY(), (int) point.getX()), Color.black));
    }

    private static void drawPathLine(List<Point> path, Worksheet sheet) {
        for (int i = 0; i < path.size() - 1; i++) {
            int srcX = (int) path.get(i).getX();
            int srcY = (int) path.get(i).getY();
            int dstX = (int) path.get(i + 1).getX();
            int dstY = (int) path.get(i + 1).getY();
            drawLine(srcY, srcX, dstY, dstX, sheet);
        }
    }

    private static void fillBg(CellRange cellRange, Color color) {
        cellRange.getStyle().setColor(color);
    }

    private static void drawLine(int srcRow, int srcCol, int dstRow, int dstCol, Worksheet sheet) {
        int a = -1;
        int b = -1;
        int c = -1;
        int d = -1;
        boolean needFlip = false;
        // 判断线的方向
        if (srcRow == dstRow) {
            // 水平方向
            if (srcCol < dstCol) {
                // 向右
                a = srcRow;
                b = srcCol;
                c = Math.abs(dstCol - srcCol);
                d = 0;
            } else {
                // 向左
                a = dstRow;
                b = dstCol;
                c = Math.abs(dstCol - srcCol);
                d = 0;
            }
        } else if (srcCol == dstCol) {
            // 垂直方向
            if (srcRow < dstRow) {
                // 向下
                a = srcRow;
                b = srcCol;
                c = 0;
                d = Math.abs(dstRow - srcRow);
            } else {
                // 向上
                a = dstRow;
                b = dstCol;
                c = 0;
                d = Math.abs(dstRow - srcRow);
            }
        } else {
            // 倾斜直线，判断终点在起点的哪个方向
            if (dstRow > srcRow && dstCol > srcCol) {
                // 正常方向,右下
                a = srcRow;
                b = srcCol;
            } else if (dstRow < srcRow && dstCol < srcCol) {
                // 左上
                a = dstRow;
                b = dstCol;
            } else {
                // 左下、右上，求中点x
                // 求左上角
                /*Point aPoint = new Point(Math.min(srcCol, dstCol), Math.min(dstRow, srcRow));
//                Point bPoint = new Point(Math.max(srcCol, dstCol), Math.max(dstRow, srcRow));
                a = (int) aPoint.getY();
                b = (int) aPoint.getY();
                needFlip = true;*/
            }
            c = Math.abs(dstCol - srcCol);
            d = Math.abs(dstRow - srcRow);

        }
        try {
            IPrstGeomShape iPrstGeomShape = sheet.getPrstGeomShapes().addPrstGeomShape(a, b,
                    (int) (21.04 * c), (int) (18.04 * d), PrstGeomShapeType.Line);
            iPrstGeomShape.setVisible(true);
            if (needFlip) {
                iPrstGeomShape.setRotation(50);
            }
        } catch (Exception e) {

        }
    }

    public static void openFile(String filePath) {
        try {
            Desktop.getDesktop().open(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
