import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.LinkedList;
import java.util.List;

/**
 * 单元测试
 *
 * @author chenchuancheng github.com/meethigher
 * @since 2022/5/15 22:08
 */
public class GeometryGridTest {

    @Test
    public void testGrid() throws Exception {

        double sideLen = 0.001;

        String wkt = "LINESTRING(106.67105340852869 29.530636809322942,106.66803216986591 29.52328796943324)";
//        String wkt = "LINESTRING(106.66641855239867 29.52517003980182,106.67002344131468 29.532727843679083,106.67507028736871 29.530487446637224,106.67675256676739 29.533175917132013)";
//        String wkt = "POLYGON((106.65735483169556 29.530158851923517,106.65258264620205 29.52902369502023,106.64671182632448 29.530965403423963,106.64609384562937 29.52785865163041,106.64588785250089 29.524781676802462,106.65330362477108 29.522899600833085,106.6530289655202 29.524542686760086,106.65141534831493 29.527111822862878,106.6533379565226 29.527380681416773,106.65756082482405 29.526514355331983,106.65776682057185 29.52403482645859,106.66192102537026 29.522122860471057,106.67208337731424 29.521585112423097,106.67551660747264 29.522929476044652,106.67881250276693 29.52531940968403,106.67943048477169 29.528814585054363,106.68025446252427 29.532459000021845,106.67874383926389 29.53428115740634,106.6783661873778 29.532548613742065,106.67850351438389 29.530218596454873,106.6765122413635 29.531114763609096,106.67575693235264 29.5339824452562,106.67469262972004 29.536192894729282,106.67187738418576 29.53732797116615,106.67012643866471 29.53583444659172,106.67246103443901 29.5338330889642,106.67290735244748 29.531831692071208,106.66882181115214 29.53189143550047,106.66703653387954 29.53278758783246,106.66264200158184 29.535655222388314,106.65986108779906 29.536909786679814,106.65821314015191 29.5349084499357,106.65838480152888 29.532309639998616,106.65735483169556 29.530158851923517))";

        WKTReader reader = new WKTReader();
        Geometry geometry = reader.read(wkt);

        //以东西为x轴，南北为y轴，获取包含此几何图形中最小和最大x和y值
        //如果是一条斜线，就重组x、y坐标构成一个矩形。
        Envelope envelopeInternal = geometry.getEnvelopeInternal();
        double maxX = envelopeInternal.getMaxX();
        double minX = envelopeInternal.getMinX();
        double maxY = envelopeInternal.getMaxY();
        double minY = envelopeInternal.getMinY();

        //如果sideLen超过了最大值，就进行拓宽。
        if ((maxX - minX) < sideLen) {
            double expand = sideLen / 2;
            maxX += expand;
            minX -= expand;
        }
        if ((maxY - minY) < sideLen) {
            double expand = sideLen / 2;
            maxY += expand;
            minY -= expand;
        }
        //构建边界，使用84坐标系即可。
        ReferencedEnvelope gridBounds =
                new ReferencedEnvelope(minX, maxX, minY, maxY, DefaultGeographicCRS.WGS84);
        SimpleFeatureSource grid = Grids.createSquareGrid(gridBounds, sideLen);
        List<String> gridList = new LinkedList<>();
        SimpleFeatureIterator iterator = grid.getFeatures().features();
        while (iterator.hasNext()) {
            SimpleFeature feature = iterator.next();
            Object defaultGeometry = feature.getDefaultGeometry();
            gridList.add(defaultGeometry.toString());
        }
        gridList.add(wkt);
        System.out.println(gridList);

        System.out.println(geometry.intersects(reader.read("POLYGON ((106.66588785250099 29.522585112423098, 106.66588785250099 29.5235851124231, 106.66688785250099 29.5235851124231, 106.66688785250099 29.522585112423098, 106.66588785250099 29.522585112423098))")));
        System.out.println(geometry.intersects(reader.read(gridList.get(1))));
        List<String> test = new LinkedList<>();
        test.add(geometry.toString());
        test.add(gridList.get(1));
        System.out.println(test);
    }

    @Test
    public void testCreator() throws Exception {
        //String wkt = "LINESTRING(106.67105340852869 29.530636809322942,106.66803216986591 29.52328796943324)";
        //String wkt = "LINESTRING(106.66641855239867 29.52517003980182,106.67002344131468 29.532727843679083,106.67507028736871 29.530487446637224,106.67675256676739 29.533175917132013)";
        String wkt = "POLYGON((106.65735483169556 29.530158851923517,106.65258264620205 29.52902369502023,106.64671182632448 29.530965403423963,106.64609384562937 29.52785865163041,106.64588785250089 29.524781676802462,106.65330362477108 29.522899600833085,106.6530289655202 29.524542686760086,106.65141534831493 29.527111822862878,106.6533379565226 29.527380681416773,106.65756082482405 29.526514355331983,106.65776682057185 29.52403482645859,106.66192102537026 29.522122860471057,106.67208337731424 29.521585112423097,106.67551660747264 29.522929476044652,106.67881250276693 29.52531940968403,106.67943048477169 29.528814585054363,106.68025446252427 29.532459000021845,106.67874383926389 29.53428115740634,106.6783661873778 29.532548613742065,106.67850351438389 29.530218596454873,106.6765122413635 29.531114763609096,106.67575693235264 29.5339824452562,106.67469262972004 29.536192894729282,106.67187738418576 29.53732797116615,106.67012643866471 29.53583444659172,106.67246103443901 29.5338330889642,106.67290735244748 29.531831692071208,106.66882181115214 29.53189143550047,106.66703653387954 29.53278758783246,106.66264200158184 29.535655222388314,106.65986108779906 29.536909786679814,106.65821314015191 29.5349084499357,106.65838480152888 29.532309639998616,106.65735483169556 29.530158851923517))";

        WKTReader reader = new WKTReader();
        Geometry geometry = reader.read(wkt);
        System.out.println(geometry.getEnvelope());
        GridCreator creator = new GridCreator(new GridCreatorFeatureBuilder(geometry, DefaultGeographicCRS.WGS84), 0.001, geometry);
        List<String> list = creator.create();

        //添加自身，方便显示
        list.add(wkt);
        System.out.println(list);
    }

    @Test
    public void name() {
        System.out.println(12%5);
    }
}