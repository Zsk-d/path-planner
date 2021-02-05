import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 禁行区域路径规划
 *
 * @author daishaoshu
 */
public class PathPlanner {

    private static final String DIRECTION_LEFT = "L";
    private static final String DIRECTION_RIGHT = "R";
    private static final String DIRECTION_UP = "U";
    private static final String DIRECTION_DOWN = "D";
    private static final String DIRECTION_NONE = "N";
    private static final String DEFAULT_DIRECTION_Y = DIRECTION_UP;
    private static final String DEFAULT_DIRECTION_X = DIRECTION_LEFT;
    private int offset;

    private volatile Map<String, ForbiddenZone> forbiddenZoneMap = new ConcurrentHashMap<>();
    private volatile List<ForbiddenZone> forbiddenZones = new ArrayList<>();

    /**
     * 边缘点
     */
    private Point a;
    private Point b;

    public Map<String, ForbiddenZone> getForbiddenZoneMap() {
        return forbiddenZoneMap;
    }

    /**
     * 路径规划对象
     *
     * @param a      规划区域左上角坐标
     * @param b      规划区域右下角坐标
     * @param offset 禁行区域余量
     */
    public PathPlanner(Point a, Point b, int offset) {
        this.a = a;
        this.b = b;
        this.offset = offset + 1;
    }

    /**
     * 开始路径规划
     *
     * @param src 起点对象
     * @param dst 终点对象
     * @return 规划结果
     */
    public List<Point> getPath(Point src, Point dst) throws Exception {
        int srcX = (int) src.getX();
        int srcY = (int) src.getY();
        int dstX = (int) dst.getX();
        int dstY = (int) dst.getY();
        List<Point> resultPath = new ArrayList<>();
        resultPath.add(src);
        int maxStackOverflow = 50;
        go(srcX, srcY, dstX, dstY, resultPath, maxStackOverflow);
        // 斜线优化
        diagonalOptimization(resultPath);
        // 清理多余点
        cleanSurplusPoint(resultPath);
        // 最终优化
        reDiagonalOptimization(resultPath);
        return resultPath;
    }

    /**
     * 优化斜线
     *
     * @param resultPath 结果点集合
     */
    private void diagonalOptimization(List<Point> resultPath) {
        if (resultPath.size() < 3) {
            return;
        }
        Point removePoint = null;
        for (int i = 0; i < resultPath.size() - 2; i++) {
            Point srcPoint = resultPath.get(i);
            Point dstPoint = resultPath.get(i + 2);
            if (srcPoint.getY() == dstPoint.getY()) {
                // 不跳过会删掉斜线起点
                continue;
            }
            if (!isCross(srcPoint, dstPoint)) {
                // 可优化
                removePoint = resultPath.get(i + 1);
                break;
            }
        }
        if (removePoint != null) {
            resultPath.remove(removePoint);
            diagonalOptimization(resultPath);
        }
    }

    private void reDiagonalOptimization(List<Point> resultPath) {
        if (resultPath.size() < 3) {
            return;
        }
        List<Point> removePoints = null;
        head:
        for (int i = 0; i < resultPath.size() - 2; i++) {
            Point srcPoint = resultPath.get(i);
            for (int j = i + 2; j < resultPath.size() - 2; j++) {
                Point dstPoint = resultPath.get(j);
                if (!isCross(srcPoint, dstPoint)) {
                    // 可优化
                    for (int k = i + 1; k < j - i; k++) {
                        if (removePoints == null) {
                            removePoints = new ArrayList<>();
                        }
                        removePoints.add(resultPath.get(k));
                    }
                    break head;
                }
            }
        }
        if (removePoints != null) {
            removePoints.forEach(resultPath::remove);
            reDiagonalOptimization(resultPath);
        }
    }

    /**
     * 清理多余点<br>
     * 注意：此方法与优化斜线方法运行先后顺序会影响结果<br>
     * 先优化后清理：可清理优化后多余的中间点<br>
     * 先清理后优化：优化失效，因为没有拐点中间点
     *
     * @param resultPath 结果点集合
     */
    private void cleanSurplusPoint(List<Point> resultPath) {
        Point removePoint = null;
        for (int i = 0; i < resultPath.size() - 2; i++) {
            Point pointA = resultPath.get(i);
            Point pointB = resultPath.get(i + 1);
            Point pointC = resultPath.get(i + 2);
            if (pointA.getY() == pointB.getY() && pointA.getY() == pointC.getY()) {
                removePoint = pointB;
                break;
            }
        }
        if (removePoint != null) {
            resultPath.remove(removePoint);
            cleanSurplusPoint(resultPath);
        }
    }

    private boolean isCross(Point srcPoint, Point dstPoint) {
        boolean isCross = false;
        Set<String> zonNames = this.forbiddenZoneMap.keySet();
        for (String zonName : zonNames) {
            ForbiddenZone zone = this.forbiddenZoneMap.get(zonName);
            if (!zone.isEnable()) {
                continue;
            }
            if (isLineIntersectRectangle(srcPoint.getX(), srcPoint.getY(),
                    dstPoint.getX(), dstPoint.getY(), zone.getxMin(),
                    zone.getyMin(), zone.getxMax(), zone.getyMax(), this.offset)) {
                isCross = true;
                break;
            }
        }
        return isCross;
    }

    /**
     * 获取禁行区域
     *
     * @param zoneName
     * @return
     */
    public ForbiddenZone getForbiddenZone(String zoneName) {
        return this.forbiddenZoneMap.get(zoneName);
    }

    /**
     * 修改安全区可用状态
     *
     * @param zoneName
     * @param status
     */
    public void changeZoneStatus(String zoneName, boolean status) {
        ForbiddenZone forbiddenZone = this.forbiddenZoneMap.get(zoneName);
        if (forbiddenZone != null) {
            forbiddenZone.setEnable(status);
        }
    }

    /**
     * 添加禁行区域
     *
     * @param zoneName      区域名
     * @param forbiddenZone 禁行区对象
     * @return 是否添加成功
     */
    public boolean addForbiddenZone(String zoneName, ForbiddenZone forbiddenZone) {
        if (this.forbiddenZoneMap.containsKey(zoneName) || this.forbiddenZones.indexOf(forbiddenZone) != -1) {
            return false;
        } else {
            // 设置边界
            try {
                forbiddenZone.setAb(this.a, this.b, this.offset);
                // 判断是否需要合并
                Set<String> zoneNames = this.forbiddenZoneMap.keySet();
                boolean isMerge = false;
                for (String name : zoneNames) {
                    ForbiddenZone zone = this.forbiddenZoneMap.get(name);
                    if (zone.isOverlap(forbiddenZone, this.offset)) {
                        zone.mergeZone(forbiddenZone);
                        // 需要全体已存在的区域进行merge todo
                        isMerge = true;
                        break;
                    }
                }
                if (!isMerge) {
                    this.forbiddenZoneMap.put(zoneName, forbiddenZone);
                }
                this.forbiddenZones.add(forbiddenZone);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    /**
     * 向终点查找路径
     *
     * @param srcX   起点x
     * @param srcY   起点y
     * @param dstX   终点x
     * @param dstY   终点y
     * @param result 路径结果
     */
    private void go(int srcX, int srcY, int dstX, int dstY, List<Point> result, int stackOverflow) throws
            Exception {
        if (--stackOverflow < 0) {
            throw new Exception("路径查找异常");
        }
        // 1. 判断运行方向
        String xDirection = getDirection(srcX, dstX, DIRECTION_RIGHT, DIRECTION_LEFT);
        String yDirection = getDirection(srcY, dstY, DIRECTION_DOWN, DIRECTION_UP);
        // 2. 开始判断x方向
        ForbiddenZone firstXZone = null;
        ForbiddenZone firstYZone = null;
        if (!DIRECTION_NONE.equals(xDirection)) {
            int tempSrcX = -1;
            for (ForbiddenZone zone : this.forbiddenZones) {
                if (zone.isEnable()) {
                    if (DIRECTION_LEFT.equals(xDirection)) {
                        if (isXWillIn(srcX, dstX, zone)) {
                            if (srcY > zone.getyMin() - offset && srcY < zone.getyMax() + offset) {
                                // 相交
                                if (firstXZone == null) {
                                    firstXZone = zone;
                                    tempSrcX = firstXZone.getxMax() + offset;
                                } else {
                                    // 对比距离
                                    firstXZone = zone.getxMax() > firstXZone.getxMax() ? zone : firstXZone;
                                    tempSrcX = firstXZone.getxMax() + offset;
                                }
                            }
                        }
                    } else {
                        if (isXWillIn(srcX, dstX, zone)) {
                            if (srcY > zone.getyMin() - offset && srcY < zone.getyMax() + offset) {
                                // 相交
                                if (firstXZone == null) {
                                    firstXZone = zone;
                                    tempSrcX = firstXZone.getxMin() - offset;
                                } else {
                                    // 对比距离
                                    firstXZone = zone.getxMin() < firstXZone.getxMin() ? zone : firstXZone;
                                    tempSrcX = firstXZone.getxMin() - offset;
                                }
                            }
                        }
                    }
                }
            }
            // x方向一个点确定
            if (firstXZone == null) {
                // 没有找到阻拦的点，说明可直达目的地y
                srcX = dstX;
            } else {
                srcX = tempSrcX;
                if (DIRECTION_NONE.equals(yDirection)) {
                    yDirection = DEFAULT_DIRECTION_Y;
                }
            }
            this.addResultPoint(srcX, srcY, result);
        }
        // 开始判断y方向，取最近
        if (!DIRECTION_NONE.equals(yDirection)) {
            Point optimizationPoint = null;
            if (firstXZone != null) {
                // 已经贴在安全区边上，尽快绕过
                /*if (DIRECTION_DOWN.equals(yDirection)) {
                    srcY = firstZone.getyMax() + offset;
                } else {
                    srcY = firstZone.getyMin() - offset;
                }*/
                if (firstXZone.isUpEdge()) {
                    srcY = firstXZone.getyMax() + offset;
                } else if (firstXZone.isDownEdge()) {
                    srcY = firstXZone.getyMin() - offset;
                } else if (DIRECTION_DOWN.equals(yDirection)) {
                    srcY = firstXZone.getyMax() + offset;
                } else if (DIRECTION_UP.equals(yDirection)) {
                    srcY = firstXZone.getyMin() - offset;
                } else if (Math.abs(firstXZone.getyMax() - srcY) < Math.abs(firstXZone.getyMin() - srcY)) {
                    srcY = firstXZone.getyMax() + offset;
                } else {
                    srcY = firstXZone.getyMin() - offset;
                }
                optimizationPoint = new Point(DIRECTION_LEFT.equals(xDirection) ? firstXZone.getxMin() - this.offset : firstXZone.getxMax() + this.offset, srcY);
            } else {
                // 旁边没有安全区，表明x方向无需移动，此时直接按目的方向移动，遍历安全区
                int tempSrcY = -1;
                for (ForbiddenZone zone : this.forbiddenZones) {
                    if (zone.isEnable()) {
                        if (DIRECTION_DOWN.equals(yDirection)) {
                            if (isYWillIn(srcY, dstY, zone)) {
                                if (srcX > zone.getxMin() - offset && srcX < zone.getxMax() + offset) {
                                    // 撞上
                                    if (firstYZone == null) {
                                        firstYZone = zone;
                                        tempSrcY = firstYZone.getyMin() - offset;
                                    } else {
                                        // 比较看谁离得更近
                                        firstYZone = zone.getyMin() < firstYZone.getyMin() ? zone : firstYZone;
                                        tempSrcY = firstYZone.getyMin() - offset;
                                    }
                                }
                            }
                        } else {
                            if (isYWillIn(srcY, dstY, zone)) {
                                if (srcX > zone.getxMin() - offset && srcX < zone.getxMax() + offset) {
                                    // 撞上
                                    if (firstYZone == null) {
                                        firstYZone = zone;
                                        tempSrcY = firstYZone.getyMax() + offset;
                                    } else {
                                        // 比较看谁离得更近
                                        firstYZone = zone.getxMax() > firstYZone.getxMax() ? zone : firstYZone;
                                        tempSrcY = firstYZone.getyMax() + offset;
                                    }
                                }
                            }
                        }
                    }
                }
                // y方向一个点确定
                if (firstYZone == null) {
                    // 没有找到阻拦的点，说明可直达目的地y
                    srcY = dstY;
                } else {
                    srcY = tempSrcY;
                }
            }
            this.addResultPoint(srcX, srcY, result);
            if (firstXZone == null && firstYZone != null) {
                srcX = DIRECTION_LEFT.equals(DEFAULT_DIRECTION_X) ? firstYZone.getxMin() - this.offset : firstYZone.getxMax() - this.offset;
                this.addResultPoint(srcX, srcY, result);
                // 最后绕一次
                String direction = getDirection(srcY, dstY, DIRECTION_DOWN, DIRECTION_UP);
                if (DIRECTION_DOWN.equals(direction)) {
                    srcY = firstYZone.getyMax() + offset;
                } else {
                    srcY = firstYZone.getyMin() - offset;
                }
                this.addResultPoint(srcX, srcY, result);
            }
            if (optimizationPoint != null) {
                this.addResultPoint(optimizationPoint, result);
            }
        }
        // 到达目的地
        if (DIRECTION_NONE.equals(xDirection) && DIRECTION_NONE.equals(yDirection)) {
            return;
        }
        go(srcX, srcY, dstX, dstY, result, stackOverflow);
    }

    private boolean isXWillIn(int srcX, int dstX, ForbiddenZone zone) {
        return zone.getxMax() + this.offset > Math.min(srcX, dstX) &&
                zone.getxMax() + this.offset < Math.max(srcX, dstX) ||
                zone.getxMin() - this.offset > Math.min(srcX, dstX) &&
                        zone.getxMin() - this.offset < Math.max(srcX, dstX) ||
                zone.getxMax() + this.offset <= Math.max(srcX, dstX) &&
                        zone.getxMin() - this.offset >= Math.min(srcX, dstX);
    }

    private boolean isYWillIn(int srcY, int dstY, ForbiddenZone zone) {
        return zone.getyMax() + this.offset > Math.min(srcY, dstY) &&
                zone.getyMax() + this.offset < Math.max(srcY, dstY) ||
                zone.getyMin() - this.offset > Math.min(srcY, dstY) &&
                        zone.getyMin() - this.offset < Math.max(srcY, dstY) ||
                zone.getyMax() + this.offset <= Math.max(srcY, dstY) &&
                        zone.getyMin() - this.offset >= Math.min(srcY, dstY);
    }

    private String getDirection(int srcX, int dstX, String directionRight, String directionLeft) {
        return srcX < dstX ? directionRight : srcX == dstX ? DIRECTION_NONE : directionLeft;
    }

    private void addResultPoint(int srcX, int srcY, List<Point> resultPath) {
        this.addResultPoint(new Point(srcX, srcY), resultPath);
    }

    private void addResultPoint(Point point, List<Point> resultPath) {
        resultPath.add(point);
    }

    /**
     * 判断线段是否穿过区域
     *
     * @param linePointX1           线段起点x
     * @param linePointY1           线段起点y
     * @param linePointX2           线段终点x
     * @param linePointY2           线段终点y
     * @param rectangleLeftTopX     区域左上角x
     * @param rectangleLeftTopY     区域左上角y
     * @param rectangleRightBottomX 区域右下角x
     * @param rectangleRightBottomY 区域右下角y
     * @param offset                区域扩充值
     * @return 判断结果
     */
    private static boolean isLineIntersectRectangle(double linePointX1,
                                                    double linePointY1,
                                                    double linePointX2,
                                                    double linePointY2,
                                                    double rectangleLeftTopX,
                                                    double rectangleLeftTopY,
                                                    double rectangleRightBottomX,
                                                    double rectangleRightBottomY, int offset) {
        offset -= 1;
        rectangleLeftTopX -= offset;
        rectangleLeftTopY -= offset;
        rectangleRightBottomX += offset;
        rectangleRightBottomY += offset;

        double lineHeight = linePointY1 - linePointY2;
        double lineWidth = linePointX2 - linePointX1;
        double t1 = lineHeight * rectangleLeftTopX + lineWidth * rectangleLeftTopY;
        double t2 = lineHeight * rectangleRightBottomX + lineWidth * rectangleRightBottomY;
        double t3 = lineHeight * rectangleLeftTopX + lineWidth * rectangleRightBottomY;
        double t4 = lineHeight * rectangleRightBottomX + lineWidth * rectangleLeftTopY;
        double c = linePointX1 * linePointY2 - linePointX2 * linePointY1;
        if ((t1 + c >= 0 && t2 + c <= 0)
                || (t1 + c <= 0 && t2 + c >= 0)
                || (t3 + c >= 0 && t4 + c <= 0)
                || (t3 + c <= 0 && t4 + c >= 0)) {
            if (rectangleLeftTopX > rectangleRightBottomX) {
                double temp = rectangleLeftTopX;
                rectangleLeftTopX = rectangleRightBottomX;
                rectangleRightBottomX = temp;
            }

            if (rectangleLeftTopY < rectangleRightBottomY) {
                double temp1 = rectangleLeftTopY;
                rectangleLeftTopY = rectangleRightBottomY;
                rectangleRightBottomY = temp1;
            }

            if ((linePointX1 < rectangleLeftTopX && linePointX2 < rectangleLeftTopX)
                    || (linePointX1 > rectangleRightBottomX && linePointX2 > rectangleRightBottomX)
                    || (linePointY1 > rectangleLeftTopY && linePointY2 > rectangleLeftTopY)
                    || (linePointY1 < rectangleRightBottomY && linePointY2 < rectangleRightBottomY)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public Point getA() {
        return a;
    }

    public void setA(Point a) {
        this.a = a;
    }

    public Point getB() {
        return b;
    }

    public void setB(Point b) {
        this.b = b;
    }

    public void setAb(Point a, Point b) {
        this.a = a;
        this.b = b;
    }

    public int getOffset() {
        return offset - 1;
    }
}
