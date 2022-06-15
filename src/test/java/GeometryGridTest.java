import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 单元测试
 *
 * @author chenchuancheng github.com/meethigher
 * @since 2022/5/15 22:08
 */
public class GeometryGridTest {

    private final Logger log = LoggerFactory.getLogger(GeometryGridTest.class);


    /**
     * 测试创建耗费时间
     * 20万个耗费4秒
     */
    @Test
    public void testCreate() throws Exception {
        String wkt = "LINESTRING(103.87454139709853 36.905681915310296,122.30502671500062 37.10651291796832)";
        Geometry geometry = GeoUtils.toEPSG3857(GeoUtils.wktToGeo(wkt));
        GridCreator creator = new GridCreator(new GridCreatorFeatureBuilder(geometry, DefaultGeographicCRS.WGS84), 10.0, geometry);
        log.info("start");
        List<String> list = creator.create();
        log.info("end-{}", list.size());
    }

    /**
     * 创建块及自身，按米来切分，最后转成度
     * 获取结果方便地图展示
     * 地图https://meethigher.top/wkt
     *
     * @throws Exception
     */
    @Test
    public void getResultShowOnEarth() throws Exception {
//        String wkt = "LINESTRING(106.67105340852869 29.530636809322942,106.66803216986591 29.52328796943324)";
        String wkt = "LINESTRING(106.66641855239867 29.52517003980182,106.67002344131468 29.532727843679083,106.67507028736871 29.530487446637224,106.67675256676739 29.533175917132013)";
        //String wkt = "POLYGON((106.65735483169556 29.530158851923517,106.65258264620205 29.52902369502023,106.64671182632448 29.530965403423963,106.64609384562937 29.52785865163041,106.64588785250089 29.524781676802462,106.65330362477108 29.522899600833085,106.6530289655202 29.524542686760086,106.65141534831493 29.527111822862878,106.6533379565226 29.527380681416773,106.65756082482405 29.526514355331983,106.65776682057185 29.52403482645859,106.66192102537026 29.522122860471057,106.67208337731424 29.521585112423097,106.67551660747264 29.522929476044652,106.67881250276693 29.52531940968403,106.67943048477169 29.528814585054363,106.68025446252427 29.532459000021845,106.67874383926389 29.53428115740634,106.6783661873778 29.532548613742065,106.67850351438389 29.530218596454873,106.6765122413635 29.531114763609096,106.67575693235264 29.5339824452562,106.67469262972004 29.536192894729282,106.67187738418576 29.53732797116615,106.67012643866471 29.53583444659172,106.67246103443901 29.5338330889642,106.67290735244748 29.531831692071208,106.66882181115214 29.53189143550047,106.66703653387954 29.53278758783246,106.66264200158184 29.535655222388314,106.65986108779906 29.536909786679814,106.65821314015191 29.5349084499357,106.65838480152888 29.532309639998616,106.65735483169556 29.530158851923517))";

        Geometry epsg3857 = GeoUtils.toEPSG3857(GeoUtils.wktToGeo(wkt));
        GridCreator gridCreator = new GridCreator(100.0, epsg3857);
        log.info("start");
        List<String> list = gridCreator.create();
        log.info("end-{}", list.size());
        List<String> newList = new LinkedList<>();
        newList.add(wkt);
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            newList.add(GeoUtils.toEPSG4326(GeoUtils.wktToGeo(iterator.next())).toText());
            iterator.remove();
        }
        System.out.println(newList.toString().replaceAll("\\[|\\]", ""));
    }

}
