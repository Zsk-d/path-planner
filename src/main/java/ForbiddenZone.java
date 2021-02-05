import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 禁行区域
 *
 * @author daishaoshu
 */
public class ForbiddenZone {

    private static final int ZONE_ENABLE_CHANGE = 0;
    private static final int ZONE_REMOVE = 1;

    private int xMin;
    private int yMin;
    private int xMax;
    private int yMax;
    private boolean enable = true;
    private List<ForbiddenZone> mergeZones;
    private Point a;
    private Point b;
    private int offset;
    /**
     * 作为被合并区域时，区域状态改变后通知合并后的区域对象来重新合并子区域
     */
    private ForbiddenZone notifyMergeZone;

    public void setNotifyMergeZone(ForbiddenZone notifyMergeZone) {
        this.notifyMergeZone = notifyMergeZone;
    }

    /**
     * 设置区域边界及偏移距离
     *
     * @param a
     * @param b
     * @param offset
     * @throws Exception
     */
    public void setAb(Point a, Point b, int offset) throws Exception {
        this.a = a;
        this.b = b;
        this.offset = offset;
        this.setEdge();
    }

    private void setEdge() throws Exception {
        // 判断是否为边缘禁行区
        if (this.getyMin() - this.offset <= this.a.getY()) {
            this.isUpEdge = true;
        }
        if (this.getyMax() + this.offset >= this.b.getY()) {
            if (this.isUpEdge()) {
                // 上下贯通，错误区域
                throw new Exception("禁行区域上下贯通");
            } else {
                this.isDownEdge = true;
            }
        }
    }

    /**
     * 不允许isDownEdge = isUpEdge = true
     */
    private boolean isDownEdge;
    private boolean isUpEdge;

    public ForbiddenZone() {
    }

    public ForbiddenZone(int xMin, int yMin, int xMax, int yMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public int getxMin() {
        return xMin;
    }

    public void setxMin(int xMin) {
        this.xMin = xMin;
    }

    public int getyMin() {
        return yMin;
    }

    public void setyMin(int yMin) {
        this.yMin = yMin;
    }

    public int getxMax() {
        return xMax;
    }

    public void setxMax(int xMax) {
        this.xMax = xMax;
    }

    public int getyMax() {
        return yMax;
    }

    public void setyMax(int yMax) {
        this.yMax = yMax;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
        if (this.notifyMergeZone != null) {
            notifyMergeZone.zoneChanged(this, ZONE_ENABLE_CHANGE);
        }
    }

    public void notifyRemove() {
        if (this.notifyMergeZone != null) {
            notifyMergeZone.zoneChanged(this, ZONE_REMOVE);
        }
    }

    public boolean isDownEdge() {
        return isDownEdge;
    }

    public void setDownEdge(boolean downEdge) {
        isDownEdge = downEdge;
    }

    public boolean isUpEdge() {
        return isUpEdge;
    }

    public void setUpEdge(boolean upEdge) {
        isUpEdge = upEdge;
    }

    /**
     * 合并所有子区域
     */
    public void mergeZone(ForbiddenZone firstMergeZone) {
        if (this.mergeZones == null) {
            // 初次合并
            this.mergeZones = new ArrayList<>();
            this.mergeZones.add(this.clone());
        }
        if (firstMergeZone != null) {
            firstMergeZone.setNotifyMergeZone(this);
            this.mergeZones.add(firstMergeZone);
        }
        // 重新合并
        ForbiddenZone onEnableZone = null;
        for (ForbiddenZone mergeZone : this.mergeZones) {
            if (mergeZone.enable) {
                onEnableZone = mergeZone;
            }
        }
        if (onEnableZone != null) {
            this.xMin = onEnableZone.getxMin();
            this.yMin = onEnableZone.getyMin();
            this.xMax = onEnableZone.getxMax();
            this.yMax = onEnableZone.getyMax();
            for (int i = 0; i < this.mergeZones.size(); i++) {
                ForbiddenZone zone = this.mergeZones.get(i);
                if (zone.enable) {
                    this.xMin = Math.min(this.xMin, zone.getxMin());
                    this.yMin = Math.min(this.yMin, zone.getyMin());
                    this.xMax = Math.max(this.xMax, zone.getxMax());
                    this.yMax = Math.max(this.yMax, zone.getyMax());
                }
            }
        } else {
            this.enable = false;
        }

        if (this.a != null && this.b != null) {
            try {
                setEdge();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 被合并的区域状态变化后触发
     *
     * @param zoneChange 触发的区域
     * @param flag       0：显示隐藏 1:移除
     */
    public void zoneChanged(ForbiddenZone zoneChange, int flag) {
        switch (flag) {
            case ZONE_ENABLE_CHANGE:
                break;
            case ZONE_REMOVE:
                this.mergeZones.remove(zoneChange);
                break;
            default:
                break;
        }
        mergeZone(null);
    }

    /**
     * 判断是否重叠
     *
     * @param forbiddenZone
     * @return
     */
    public boolean isOverlap(ForbiddenZone forbiddenZone, int offset) {
        int xMin = this.xMin - (2 * offset - 1);
        int xMax = this.xMax + (2 * offset - 1);
        int yMin = this.yMin - (2 * offset - 1);
        int yMax = this.yMax + (2 * offset - 1);
        return !(yMin > forbiddenZone.yMax || xMax < forbiddenZone.xMin || yMax < forbiddenZone.yMin || xMin > forbiddenZone.xMax);
    }

    /**
     * 单倍间隔判断重叠
     *
     * @param forbiddenZone
     * @param offset
     * @return
     */
    public boolean isZoneOverlap(ForbiddenZone forbiddenZone, int offset) {
        int xMin = this.xMin - offset;
        int xMax = this.xMax + offset;
        int yMin = this.yMin - offset;
        int yMax = this.yMax + offset;
        return !(yMin > forbiddenZone.yMax || xMax < forbiddenZone.xMin || yMax < forbiddenZone.yMin || xMin > forbiddenZone.xMax);
    }

    @Override
    protected ForbiddenZone clone() {
        return new ForbiddenZone(this.xMin, this.yMin, this.xMax, this.yMax);
    }

    @Override
    public int hashCode() {
        return String.format("%s,%s,%s,%s", xMin, yMin, xMax, yMax).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ForbiddenZone) {
            ForbiddenZone zone = (ForbiddenZone) obj;
            return zone.xMin == this.xMin && zone.yMin == this.yMin &&
                    zone.xMax == this.xMax && zone.yMax == this.yMax;
        }
        return false;
    }
}
