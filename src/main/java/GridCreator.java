import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.Grids;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.LinkedList;
import java.util.List;

/**
 * 网格创建工具类
 * 默认使用84坐标系
 *
 * @author chenchuancheng github.com/meethigher
 * @since 2022/5/15 21:36
 */
public class GridCreator {

    /**
     * 坐标系
     * 默认使用84坐标系
     */
    private CoordinateReferenceSystem crs;

    /**
     * 构建器
     */
    private GridFeatureBuilder gridFeatureBuilder;


    /**
     * 边长
     */
    private Double sideLen;

    /**
     * 区域
     */
    private Geometry geometry;


    public GridCreator(Double sideLen, Geometry geometry) {
        this(DefaultGeographicCRS.WGS84, null, sideLen, geometry);
    }

    public GridCreator(CoordinateReferenceSystem crs, Double sideLen, Geometry geometry) {
        this(crs, null, sideLen, geometry);
    }


    public GridCreator(GridFeatureBuilder gridFeatureBuilder, Double sideLen, Geometry geometry) {
        this(DefaultGeographicCRS.WGS84, gridFeatureBuilder, sideLen, geometry);
    }

    public GridCreator(CoordinateReferenceSystem crs, GridFeatureBuilder gridFeatureBuilder, Double sideLen, Geometry geometry) {
        this.crs = crs;
        this.gridFeatureBuilder = gridFeatureBuilder;
        this.sideLen = sideLen;
        this.geometry = geometry;
    }

    /**
     * LineSting拆分方格逻辑
     *
     * @param geometry
     * @return
     */
    private List<String> linestringSplit(Geometry geometry) {
        List<String> list = new LinkedList<>();
        Coordinate[] coordinates = geometry.getCoordinates();
        for (int i = 0; i < coordinates.length; i++) {
            if ((i + 1) > geometry.getCoordinates().length - 1) {
                break;
            } else {
                Coordinate start = coordinates[i];
                Coordinate end = coordinates[i + 1];
                double deltaY = (end.y - start.y);
                double deltaX = (end.x - start.x);
                //加该判断是为了针对奇葩问题。比如手一抖，连续点了好几个一样的点。
                //至于特殊情况，像π、π/2，通过Math.atan取绝对值就够了
                if (!(deltaX == 0 && deltaY == 0)) {
                    double degree = Math.atan(deltaY / deltaX);
                    //如果距离不足sideLen时，至少保证有一个点被渲染
                    Geometry grid = GeoUtils.rotateGeometry(GeoUtils.createPoint(start.getX(), start.getY()).buffer(sideLen / 2, 1, BufferOp.CAP_SQUARE),
                            GeoUtils.createPoint(start.getX(), start.getY()), degree, false);
                    list.add(grid.toText());
                    //此处选用1/2的原因，看图。只要<俺>到<终点>的距离大于边长的一半，就给再补一个块出来
                    // ____________
                    //|   __|__俺__|_终点
                    //|_____|_____|
                    while (GeoUtils.createLine(start, end).getLength() > sideLen * 1 / 2) {
                        double x = sideLen * Math.cos(degree);
                        double y = sideLen * Math.sin(degree);
                        double realX;
                        double realY;
                        /*如果线的走向是往西，需要取相反数*/
                        if (end.x >= start.x) {
                            realX = (start.x + x);
                            realY = (start.y + y);
                        } else {
                            realX = (start.x - x);
                            realY = (start.y - y);
                        }
                        Point point = GeoUtils.createPoint(realX, realY);
                        grid = GeoUtils.rotateGeometry(GeoUtils.createPoint(point.getX(), point.getY()).buffer(sideLen / 2, 1, BufferOp.CAP_SQUARE),
                                GeoUtils.createPoint(point.getX(), point.getY()), degree, false);
                        list.add(grid.toText());
                        start = new Coordinate(point.getX(), point.getY());

                    }
                }
            }
        }
        return list;
    }

    /**
     * Polygon拆分方格逻辑
     *
     * @param geometry
     * @return
     */
    private List<String> polygonSplit(Geometry geometry) {
        //以东西为x轴，南北为y轴，获取包含此几何图形中最小和最大x和y值
        //如果是一条斜线，就重组x、y坐标构成一个矩形。
        Envelope envelopeInternal = geometry.getEnvelopeInternal();
        double maxX = envelopeInternal.getMaxX();
        double minX = envelopeInternal.getMinX();
        double maxY = envelopeInternal.getMaxY();
        double minY = envelopeInternal.getMinY();

        /**
         * 多加一块的原因有两个
         * 1. 如果sideLen超过了最大的差值，此时会缺块
         * 2. 如果区域多出来了一部分，不足以用sideLen进行分块，此时也会缺块
         */
        if ((maxX - minX) < sideLen || (maxX - minX) % sideLen < sideLen) {
            maxX += sideLen;
        }
        if ((maxY - minY) < sideLen || (maxY - minY) % sideLen < sideLen) {
            maxY += sideLen;
        }

        try {
            //构建边界，使用84坐标系即可。
            ReferencedEnvelope gridBounds =
                    new ReferencedEnvelope(minX, maxX, minY, maxY, DefaultGeographicCRS.WGS84);
            SimpleFeatureSource grid;
            if (gridFeatureBuilder == null) {
                grid = Grids.createSquareGrid(gridBounds, sideLen);
            } else {
                grid = Grids.createSquareGrid(gridBounds, sideLen, -1, gridFeatureBuilder);
            }
            List<String> gridList = new LinkedList<>();
            SimpleFeatureIterator iterator = grid.getFeatures().features();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
                gridList.add(defaultGeometry.toString());
            }
            return gridList;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建方格
     *
     * @return 方格的wkt集合
     */
    public List<String> create() {
        if (geometry == null) {
            return null;
        }
        if (geometry instanceof LineString) {
            return linestringSplit(geometry);
        } else {
            return polygonSplit(geometry);
        }
    }


}
