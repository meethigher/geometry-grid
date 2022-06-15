import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * 工具类
 * [C# 计算一个点围绕另一个点旋转指定弧度后的坐标_chenzai1946的博客-CSDN博客](https://blog.csdn.net/chenzai1946/article/details/100718099)
 *
 * @author chenchuancheng
 * @since 2022/5/19 16:54
 */
public class GeoUtils {
    private final static GeometryFactory geometryFactory = new GeometryFactory();
    private static final CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);

    /**
     * wkt转换为geo
     *
     * @param wkt
     * @return
     */
    public static Geometry wktToGeo(String wkt) {
        WKTReader reader = new WKTReader();
        try {
            return reader.read(wkt);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 3857转换坐标系4326
     *
     * @param geometry
     * @return
     */
    public static Geometry toEPSG4326(Geometry geometry) {
        try {
            CoordinateReferenceSystem source = factory.createCoordinateReferenceSystem("EPSG:4326");
            CoordinateReferenceSystem target = factory.createCoordinateReferenceSystem("EPSG:3857");
            MathTransform transform = CRS.findMathTransform(target, source);
            return JTS.transform(geometry, transform);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 4326转换坐标系为3857
     *
     * @param geometry
     * @return
     */
    public static Geometry toEPSG3857(Geometry geometry) {
        try {
            CoordinateReferenceSystem source = factory.createCoordinateReferenceSystem("EPSG:4326");
            CoordinateReferenceSystem target = factory.createCoordinateReferenceSystem("EPSG:3857");
            MathTransform transform = CRS.findMathTransform(source, target);
            return JTS.transform(geometry, transform);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 传入精度维度生成点
     *
     * @param lon 精度
     * @param lat 维度
     * @return
     */
    public static Point createPoint(Double lon, Double lat) {
        Coordinate coordinate = new Coordinate();
        coordinate.x = lon;
        coordinate.y = lat;
        //myPoint.setSRID(4326); 创建的时候不能指定为4326,也可能是其他的坐标系，返回对象由用户自定义
        return geometryFactory.createPoint(coordinate);
    }


    /**
     * 创建Polygon
     *
     * @param coordinates
     * @return
     */
    public static Polygon createPolygon(Coordinate... coordinates) {
        return geometryFactory.createPolygon(coordinates);
    }

    /**
     * 创建LineString
     *
     * @param coordinates
     * @return
     */
    public static LineString createLine(Coordinate... coordinates) {
        return geometryFactory.createLineString(coordinates);
    }

    /**
     * 长度
     *
     * @param a
     * @param b
     * @return
     */
    public static double pointLength(Point a, Point b) {
        return GeoUtils.createLine(a.getCoordinate(), b.getCoordinate()).getLength();
    }

    /**
     * 获取角度
     *
     * @param x
     * @param y
     * @return
     */
    public static double degree(double x, double y) {
        //P在(0,0)的情况
        if (x == 0 && y == 0) {
            return 0;
        }

        //P在四个坐标轴上的情况：x正、x负、y正、y负
        if (y == 0 && x > 0) {
            return 0;
        }
        if (y == 0 && x < 0) {
            return Math.PI;
        }
        if (x == 0 && y > 0) {
            return Math.PI / 2;
        }
        if (x == 0 && y < 0) {
            return Math.PI / 2 * 3;
        }

        //点在第一、二、三、四象限时的情况
        if (x > 0 && y > 0) {
            return Math.atan(y / x);
        }
        if (x < 0 && y > 0) {
            return Math.PI - Math.atan(y / -x);
        }
        if (x < 0 && y < 0) {
            return Math.PI + Math.atan(-y / -x);
        }
        if (x > 0 && y < 0) {
            return Math.PI * 2 - Math.atan(-y / x);
        }

        return 0;
    }

    /**
     * 旋转
     * 在平面坐标系算会比较准，建议旋转时使用3857坐标系
     * <p>
     * 数学题，A以P为中心，求旋转degree后B的坐标
     *
     * @param P           旋转中心点
     * @param A           待旋转的点
     * @param degree      度数
     * @param isClockwise true为顺时针，false为逆时针
     * @return A以P为中心旋转degree后B的坐标
     */
    public static Point rotatePoint(Point A, Point P,
                                    double degree, boolean isClockwise) {
        //将点A平移到点a。点A以P为中心<==>点a以原点o为中心
        Point a = GeoUtils.createPoint(A.getX() - P.getX(), A.getY() - P.getY());
        //以原点o为中心的圆半径<==>以P为中心的圆的半径
        double radius = pointLength(a, GeoUtils.createPoint(0.0, 0.0));
        //线段oa与x轴的角度
        double degreeWithX = degree(a.getX(), a.getY());
        //线段oa旋转后与x轴的角度
        double degreeAfterRotateWithX = degreeWithX - (isClockwise ? 1 : -1) * degree;
        //获取到旋转后的点b
        Point b = GeoUtils.createPoint(radius * Math.cos(degreeAfterRotateWithX),
                radius * Math.sin(degreeAfterRotateWithX));
        //平移获取最终B点
        return GeoUtils.createPoint(b.getX() + P.getX(), b.getY() + P.getY());
    }

    /**
     * 在平面算会比较准，建议旋转时使用3857坐标系
     *
     * @param a
     * @param c
     * @param degree
     * @param isClockwise
     * @return
     */
    public static Geometry rotateGeometry(Geometry a, Point c, double degree, boolean isClockwise) {
        Coordinate[] coordinates = a.getCoordinates();
        for (Coordinate coordinate : coordinates) {
            Point point = rotatePoint(GeoUtils.createPoint(coordinate.x, coordinate.y), c, degree, isClockwise);
            coordinate.setX(point.getX());
            coordinate.setY(point.getY());
        }
        return a;
    }
}
