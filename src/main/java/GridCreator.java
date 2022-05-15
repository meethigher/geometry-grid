import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.Grids;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
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
     * 创建Grid
     *
     * @return Grid的wkt集合
     */
    public List<String> create() {
        if (geometry == null) {
            return null;
        }

        /**
         * 为啥要2/3扩展？如果maxY-minY比sideLen多出了哪怕1cm，额外的那1cm，就不会被分块了，因为分不了
         * max与min都扩展2/3，相当于多加了一个块，剩下了一点。
         */
        double expand = sideLen / 2;
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


}
