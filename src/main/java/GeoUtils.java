import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * 工具类
 *
 * @author chenchuancheng
 * @since 2022/5/19 16:54
 */
public class GeoUtils {
    public static Geometry wktToGeo(String wkt) {
        WKTReader reader = new WKTReader();
        try {
            return reader.read(wkt);
        } catch (Exception e) {
            return null;
        }
    }
}
