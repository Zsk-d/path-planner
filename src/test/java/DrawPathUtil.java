import com.spire.xls.*;
import com.spire.xls.core.IPrstGeomShape;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class DrawPathUtil {

    public static final double WIDTH_UNIT = 21.04;
    public static final double HEIGHT_UNIT = 18.04;

    public static void openFile(String filePath) {
        try {
            Desktop.getDesktop().open(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void drawLine(int srcRow, int srcCol, int dstRow, int dstCol, Worksheet sheet) {
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
                    (int) (WIDTH_UNIT * c), (int) (HEIGHT_UNIT * d), PrstGeomShapeType.Line);
            iPrstGeomShape.setVisible(true);
            if (needFlip) {
                iPrstGeomShape.setRotation(50);
            }
        } catch (Exception e) {

        }
    }

    public static void fillBg(CellRange cellRange, Color color) {
        cellRange.getStyle().setColor(color);
    }

    public static void drawPathLine(List<Point> path, Worksheet sheet) {
        for (int i = 0; i < path.size() - 1; i++) {
            int srcX = (int) path.get(i).getX();
            int srcY = (int) path.get(i).getY();
            int dstX = (int) path.get(i + 1).getX();
            int dstY = (int) path.get(i + 1).getY();
            drawLine(srcY, srcX, dstY, dstX, sheet);
        }
    }

    public static void drawPathPoint(List<Point> path, Worksheet sheet) {
        path.forEach(point -> fillBg(sheet.getCellRange((int) point.getY(), (int) point.getX()), Color.black));
    }

}
