import org.junit.Test;

public class GeoUtilsTest {

    @Test
    public void wgs84ToGcj02ByWKT() {
        //String wkt = "POLYGON((121.28906249999879 31.05293398570481,121.28906249999879 31.353636941500653,121.6406249999987 31.353636941500653,121.6406249999987 31.05293398570481,121.28906249999879 31.05293398570481))";
        //String wkt = "POINT(121.26812704520235 31.386244370762967)";
        String wkt = "LINESTRING(121.26813304764624 31.3862510167303,121.27211597994506 31.384389501469087)";
        String newWkt = GeoUtils.wgs84ToGcj02ByWKT(wkt);
        System.out.println(newWkt);

    }

    @Test
    public void gcj02ToWgs84ByWKT() {
    }

    @Test
    public void gcj02ToBd09ByWKT() {
    }

    @Test
    public void bd09ToGcj02ByWKT() {
    }

    @Test
    public void wgs84ToMercatorByWKT() {
    }

    @Test
    public void mercatorToWgs84ByWKT() {
    }
}