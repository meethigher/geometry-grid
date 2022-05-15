import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.grid.GridElement;
import org.geotools.grid.GridFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import java.util.Map;

/**
 * 自定义实现，附加切块的条件过滤
 *
 * @author chenchuancheng github.com/meethigher
 * @since 2022/5/15 21:40
 */
public class GridCreatorFeatureBuilder extends GridFeatureBuilder {


    private final Geometry originGeometry;

    /**
     * Creates a {@code GridFeatureBuilder} to work with the given feature type.
     *
     * @param type the feature type
     * @param originGeometry
     */
    public GridCreatorFeatureBuilder(SimpleFeatureType type, Geometry originGeometry) {
        super(type);
        this.originGeometry = originGeometry;
    }

    /** Default feature TYPE name: "grid" */
    public static final String DEFAULT_TYPE_NAME = "grid";

    /** Name used for the integer id attribute: "id" */
    public static final String ID_ATTRIBUTE_NAME = "id";

    private int id;

    /**
     * Creates the feature TYPE
     *
     * @param typeName name for the feature TYPE; if {@code null} or empty, {@linkplain
     *     #DEFAULT_TYPE_NAME} will be used
     * @param crs coordinate reference system (may be {@code null})
     * @return the feature TYPE
     */
    protected static SimpleFeatureType createType(String typeName, CoordinateReferenceSystem crs) {
        final String finalName;
        if (typeName != null && typeName.trim().length() > 0) {
            finalName = typeName;
        } else {
            finalName = DEFAULT_TYPE_NAME;
        }

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(finalName);
        tb.add(DEFAULT_GEOMETRY_ATTRIBUTE_NAME, Polygon.class, crs);
        tb.add("id", Integer.class);
        return tb.buildFeatureType();
    }

    public GridCreatorFeatureBuilder(Geometry originGeometry,CoordinateReferenceSystem crs) {
        super(createType(DEFAULT_TYPE_NAME, crs));
        this.originGeometry = originGeometry;
    }

    @Override
    public void setAttributes(GridElement el, Map<String, Object> attributes) {
        attributes.put("id", ++id);
    }

    /**
     * 过滤条件
     *
     * @param el
     * @return
     */
    @Override
    public boolean getCreateFeature(GridElement el) {
        /**
         * intersects两种情况满足其一即为true
         * 1. 相交
         * 2. 包含
         */
        return originGeometry.intersects(el.toGeometry());
    }
}
