import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


/**
 * 工具类
 * 仅适用于中国的点位坐标系转换
 * 各地图API坐标系统比较与转换;
 * <p>
 * WGS84坐标系(EPSG4326)：即地球坐标系，国际上通用的坐标系。设备一般包含GPS芯片或者北斗芯片获取的经纬度为WGS84地理坐标系,
 * 谷歌地图采用的是WGS84地理坐标系（中国范围除外）;
 * <p>
 * GCJ02坐标系：即火星坐标系，是由中国国家测绘局制订的地理信息系统的坐标系统。由WGS84坐标系经加密后的坐标系。
 * 高德地图、腾讯地图、谷歌中国地图和搜搜中国地图采用的是GCJ02地理坐标系;
 * <p>
 * BD09坐标系：即百度坐标系，GCJ02坐标系经加密后的坐标系;
 * <p>
 * 搜狗坐标系、图吧坐标系等，估计也是在GCJ02基础上加密而成的。
 * <p>
 *
 * @author <a href="https://meethigher.top">chenchuancheng</a>
 * @see <a href="https://blog.csdn.net/lc_2014c/article/details/125878730">JAVA实现WGS84、百度坐标系、高德坐标系转化工具类_javawgs84 坐标系 转 gcj02 坐标系-CSDN博客</a>
 * @see <a href="https://blog.csdn.net/chenzai1946/article/details/100718099">C# 计算一个点围绕另一个点旋转指定弧度后的坐标-CSDN博客</a>
 * @since 2022/5/19 16:54
 */
public class GeoUtils {
    private final static GeometryFactory geometryFactory = new GeometryFactory();
    private static final CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
    private final static double pi = Math.PI;
    private final static double a = 6378245.0;
    private final static double ee = 0.00669342162296594323;
    private final static WKTReader wktReader = new WKTReader();

    /**
     * 是否超出中国范围以外
     *
     * @param lat 纬度
     * @param lon 经度
     * @return true表示中国外
     */
    private static boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        return lat < 0.8293 || lat > 55.8271;
    }


    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }


    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
                * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0
                * pi)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 转换为84坐标系
     */
    private static double[] transform(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return new double[]{lat, lon};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;

        return new double[]{mgLat, mgLon};
    }


    /**
     * 84 to 火星坐标系 (GCJ-02)
     * <p>
     * World Geodetic System ==> Mars Geodetic System
     */
    public static double[] wgs84togcj02(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return null;
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;

        return new double[]{mgLat, mgLon};
    }

    /**
     * 火星坐标系 (GCJ-02) to 84
     */
    public static double[] gcj02towgs84(double lat, double lon) {
        double[] gps = transform(lat, lon);
        double longitude = lon * 2 - gps[0];
        double latitude = lat * 2 - gps[1];
        return new double[]{latitude, longitude};
    }


    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法
     * <p>
     * 将 GCJ-02 坐标转换成 BD-09 坐标
     */
    public static double[] gcj02tobd09(double gg_lat, double gg_lon) {
        double x = gg_lon, y = gg_lat;

        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * pi);

        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * pi);

        double bd_lon = z * Math.cos(theta) + 0.0065;

        double bd_lat = z * Math.sin(theta) + 0.006;

        return new double[]{bd_lat, bd_lon};
    }

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法
     * <p>
     * 将 BD-09 坐标转换成GCJ-02 坐标
     */
    public static double[] bd09togcj02(double bd_lat, double bd_lon) {
        double x = bd_lon - 0.0065, y = bd_lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * pi);

        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * pi);

        double gg_lon = z * Math.cos(theta);

        double gg_lat = z * Math.sin(theta);

        return new double[]{gg_lat, gg_lon};
    }


    /**
     * WGS84坐标系(EPSG:4326)转换为墨卡托投影坐标系(EPSG:3857)
     */
    public static double[] wgs84tomercator(double lon, double lat) {
        //wgs84下的地球半径
        double earthRadius = 6378137.0;
        double arc = lat * pi / 180;
        return new double[]{
                lon * pi / 180 * earthRadius,
                earthRadius / 2 * Math.log((1.0 + Math.sin(arc)) / (1.0 - Math.sin(arc)))
        };
    }

    /**
     * 墨卡托投影坐标系(EPSG:3857)转换为wgs84坐标系(EPSG:4326)
     */
    public static double[] mercatortowgs84(double lon, double lat) {
        //wgs84下的地球半径
        double earthRadius = 6378137.0;
        //周长
        double round = 2 * pi * earthRadius / 2.0;
        return new double[]{
                lat / round * 180,
                180 / pi * (2 * Math.atan(Math.exp((lon / round * 180) * Math.PI / 180)) - Math.PI / 2)
        };
    }

    /**
     * wkt转换为geo
     */
    public static Geometry geomFromText(String wkt) {
        try {
            return wktReader.read(wkt);
        } catch (Exception e) {
            return null;
        }
    }

    public static String asText(Geometry g) {
        return g.toText();
    }

    /**
     * 3857转换坐标系4326
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
     */
    public static Polygon createPolygon(Coordinate... coordinates) {
        return geometryFactory.createPolygon(coordinates);
    }

    /**
     * 创建LineString
     */
    public static LineString createLine(Coordinate... coordinates) {
        return geometryFactory.createLineString(coordinates);
    }

    /**
     * 长度
     * 这个的计算略慢，如果考虑性能问题，建议直接使用三角函数来就算。
     */
    public static double pointLength(Point a, Point b) {
        return GeoUtils.createLine(a.getCoordinate(), b.getCoordinate()).getLength();
    }

    /**
     * 获取角度
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

    /**
     * 校验空间数据是否合法，比如自相交等问题
     */
    public static boolean isValid(Geometry geometry) {
        return geometry.isValid();
    }

    public static boolean isValid(String wkt) {
        Geometry geometry = geomFromText(wkt);
        return geometry != null && isValid(geometry);
    }

    /**
     * 检查拓扑错误
     * 比如自相交等问题。其实通过创建任意一个Geometry, 然后进行union错误的Geometry，也能实现
     *
     * @return 若为null表示拓扑正常
     */
    public static TopologyValidationError validate(Geometry geometry) {
        IsValidOp isValidOp = new IsValidOp(geometry);
        return isValidOp.getValidationError();
    }
}
