趁着周末学习一下大佬的写法，发现大佬的写法也是有Bug的，于是便想着优化一下。

工作中的一个需求，功能就是落点分析，要精确的分析，一开始使用的拓宽后合并，然后根据合并的结果是否包含指定结果，来确定是否完全落点。但是要精确到某个地方没落点就不行了。

于是公司大佬就研究下了geotools实现了一个分块功能，根据分块来落点，然后最后取出没落点的块，实现漏点分析。

# 一、废话

介绍正文之前，先记录下周六玩的东西。苏见信的《顽魂》是真的好听，降噪耳机，单曲循环24小时+码字，俩字**浪漫**！

<audio src="https://meethigher.top/media/wanhun.mp3" style="height:30px" controls loop></audio>

**“悬崖上花开，半空仍纯白，逆境中有什么不能重来，天地的熔炉，烧炼我凡胎，烧不尽顽固血脉，别想我敬拜”**

**“尽管烧成埃，以血肉灭黑白，凭一身顽魂不会坏”**

![](https://meethigher.top/blog/2022/geometry-grid/1.jpg)


直接抓包下了MV，想自己打磨下——**提高分辨率**和**补帧**，这里发现了几个好用的算法/工具。

1. [ailab: B站开源算法，超吃显存，我的1650显存4G带不动，不过出来的效果吊打下面两个，只能说B站调教还是牛逼](https://github.com/bilibili/ailab)
2. [Squirrel-RIFE: 效果更好的补帧软件，显存占用更小，是DAIN速度的10-25倍，包含抽帧处理，去除动漫卡顿感](https://github.com/Justin62628/Squirrel-RIFE)
3. [Waifu2x-Extension-GUI: 提高分辨率+补帧的GUI工具，好用，显存占用少，但是默认引擎出来效果垃圾，当然了可以选用B站的引擎](https://github.com/AaronFeng753/Waifu2x-Extension-GUI)

丢到B站上去了，[【完美世界】aiLab渲染的纯享版《顽魂》_哔哩哔哩_bilibili](https://www.bilibili.com/video/BV1a3411A7oH?spm_id_from=333.999.0.0)，瑕疵还是有的吧，也就60帧还可以，不过没有B站大会员也体会不到，哈哈。

# 二、正文

老规矩，放出抄袭来源！

1. [Vector grids — GeoTools 27-SNAPSHOT User Guide](https://docs.geotools.org/stable/userguide/extension/grid.html#polygon-grids)
2. [clydedacruz/openstreetmap-wkt-playground: Plot and visualize WKT shapes on OpenStreetMap](https://github.com/clydedacruz/openstreetmap-wkt-playground)
3. [asapelkin/wkt_3d_viewer: WKT (Well-known text) viewer on the 3D Earth](https://github.com/asapelkin/wkt_3d_viewer)
4. [wkt在线绘制展示_EPSG4326](https://meethigher.top/wkt/)
5. [meethigher/wkt-show-on-openlayers: 基于openlayers的wkt绘制展示功能](https://github.com/meethigher/wkt-show-on-openlayers)
6. [本文源码](https://github.com/meethigher/geometry-grid)

工作中的一个需求，功能就是**落点分析**。要精确的分析，一开始使用的缓存拓宽后合并，然后根据合并的结果是否包含指定区域，来确定是否完全落点。但是要精确到某个地方没落点就不行了。于是公司大佬就研究下了geotools实现了一个分块功能，根据分块来落点，然后最后取出没落点的块，实现漏点分析。

趁着周末学习一下大佬的写法，发现大佬的写法也是有Bug的，于是便想着优化一下。

先不扯大佬怎么写的，自己比着文档一步一步来。

## 2.1 依赖

拉取依赖如果出问题，可以参照我之前的[文章](https://meethigher.top/blog/2022/maven-tips/)

maven依赖pom

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>top.meethigher.geometrygrid</groupId>
    <artifactId>geometry-grid</artifactId>
    <version>1.0.0</version>

    <properties>
        <project.build.sourceEncoding>utf-8</project.build.sourceEncoding>
        <compiler.encoding>utf-8</compiler.encoding>
        <java.source.version>1.8</java.source.version>
        <java.target.version>1.8</java.target.version>
        <geotools.version>22-RC</geotools.version>
    </properties>

    <repositories>
        <repository>
            <id>osgeo</id>
            <name>OSGeo Release Repository</name>
            <url>https://repo.osgeo.org/repository/release/</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <releases>
                <enabled>true</enabled>
            </releases>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-grid</artifactId>
            <version>${geotools.version}</version>
        </dependency>
        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-opengis</artifactId>
            <version>${geotools.version}</version>
        </dependency>


        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-epsg-hsql</artifactId>
            <version>${geotools.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-main</artifactId>
            <version>${geotools.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-geojson</artifactId>
            <version>${geotools.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

## 2.2 实现

思路

1. 获取边界，构建矩形
2. 根据要切的边长，将矩形切成小块。

![](https://meethigher.top/blog/2022/geometry-grid/6.jpg)

下面展示Geotools如何获取边界

```java
Geometry envelope = geometry.getEnvelope();
```

下面展示官方文档给的例子，其实结果已经接近我所想要的90%了。

```java
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

        //String wkt = "LINESTRING(106.67105340852869 29.530636809322942,106.66803216986591 29.52328796943324)";
        //String wkt = "LINESTRING(106.66641855239867 29.52517003980182,106.67002344131468 29.532727843679083,106.67507028736871 29.530487446637224,106.67675256676739 29.533175917132013)";
        String wkt = "POLYGON((106.65735483169556 29.530158851923517,106.65258264620205 29.52902369502023,106.64671182632448 29.530965403423963,106.64609384562937 29.52785865163041,106.64588785250089 29.524781676802462,106.65330362477108 29.522899600833085,106.6530289655202 29.524542686760086,106.65141534831493 29.527111822862878,106.6533379565226 29.527380681416773,106.65756082482405 29.526514355331983,106.65776682057185 29.52403482645859,106.66192102537026 29.522122860471057,106.67208337731424 29.521585112423097,106.67551660747264 29.522929476044652,106.67881250276693 29.52531940968403,106.67943048477169 29.528814585054363,106.68025446252427 29.532459000021845,106.67874383926389 29.53428115740634,106.6783661873778 29.532548613742065,106.67850351438389 29.530218596454873,106.6765122413635 29.531114763609096,106.67575693235264 29.5339824452562,106.67469262972004 29.536192894729282,106.67187738418576 29.53732797116615,106.67012643866471 29.53583444659172,106.67246103443901 29.5338330889642,106.67290735244748 29.531831692071208,106.66882181115214 29.53189143550047,106.66703653387954 29.53278758783246,106.66264200158184 29.535655222388314,106.65986108779906 29.536909786679814,106.65821314015191 29.5349084499357,106.65838480152888 29.532309639998616,106.65735483169556 29.530158851923517))";

        WKTReader reader = new WKTReader();
        Geometry geometry = reader.read(wkt);

        //以东西为x轴，南北为y轴，获取包含此几何图形中最小和最大x和y值
        //如果是一条斜线，就重组x、y坐标构成一个矩形。
        Envelope envelopeInternal = geometry.getEnvelopeInternal();
        double maxX = envelopeInternal.getMaxX();
        double minX = envelopeInternal.getMinX();
        double maxY = envelopeInternal.getMaxY();
        double minY = envelopeInternal.getMinY();

        //构建边界，坐标系随便传啦。
        //这个坐标系其实也无所谓，作用就是创建的时候，塞进去，读取的时候在查出来。你乱传都不影响结果
        //没啥作用，要非要说作用，那就是起个标识的作用啦，方便你断点的时候知道这是个啥
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
    }
}
```

这三个wkt，最后的结果依次是下面的三张图。

![](https://meethigher.top/blog/2022/geometry-grid/2.jpg)

![](https://meethigher.top/blog/2022/geometry-grid/3.jpg)

![](https://meethigher.top/blog/2022/geometry-grid/4.jpg)

由图可知，就取第一张图来说，根据我的需求，我应该要的是下面这种效果。白色表示丢弃的部分。过滤掉白色这部分，线只需要通过相交判断，如果是面，还需要包含才可。

![](https://meethigher.top/blog/2022/geometry-grid/5.jpg)

下面进行修改，修改的过程还是要多断点查看，从createSquareGrid断点进入，查看里面的具体逻辑。此处不多赘述！

关键的地方如下，通过此处我们可以进行过滤。

![](https://meethigher.top/blog/2022/geometry-grid/7.jpg )

接下来，自己实现一个Builder，添加自己的逻辑，其他的直接源码拷一份！

```java
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
```

实现一个网格Creator

```java
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
     * 默认使用WGS
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
```

进行测试

```java
@Test
public void testCreator() throws Exception {
    String wkt = "LINESTRING(106.67105340852869 29.530636809322942,106.66803216986591 29.52328796943324)";
    //String wkt = "LINESTRING(106.66641855239867 29.52517003980182,106.67002344131468 29.532727843679083,106.67507028736871 29.530487446637224,106.67675256676739 29.533175917132013)";
    //String wkt = "POLYGON((106.65735483169556 29.530158851923517,106.65258264620205 29.52902369502023,106.64671182632448 29.530965403423963,106.64609384562937 29.52785865163041,106.64588785250089 29.524781676802462,106.65330362477108 29.522899600833085,106.6530289655202 29.524542686760086,106.65141534831493 29.527111822862878,106.6533379565226 29.527380681416773,106.65756082482405 29.526514355331983,106.65776682057185 29.52403482645859,106.66192102537026 29.522122860471057,106.67208337731424 29.521585112423097,106.67551660747264 29.522929476044652,106.67881250276693 29.52531940968403,106.67943048477169 29.528814585054363,106.68025446252427 29.532459000021845,106.67874383926389 29.53428115740634,106.6783661873778 29.532548613742065,106.67850351438389 29.530218596454873,106.6765122413635 29.531114763609096,106.67575693235264 29.5339824452562,106.67469262972004 29.536192894729282,106.67187738418576 29.53732797116615,106.67012643866471 29.53583444659172,106.67246103443901 29.5338330889642,106.67290735244748 29.531831692071208,106.66882181115214 29.53189143550047,106.66703653387954 29.53278758783246,106.66264200158184 29.535655222388314,106.65986108779906 29.536909786679814,106.65821314015191 29.5349084499357,106.65838480152888 29.532309639998616,106.65735483169556 29.530158851923517))";

    WKTReader reader = new WKTReader();
    Geometry geometry = reader.read(wkt);
    GridCreator creator = new GridCreator(new GridCreatorFeatureBuilder(geometry, DefaultGeographicCRS.WGS84), 0.001, geometry);
    List<String> list = creator.create();

    //添加自身，方便显示，https://meethigher.top/wkt/
    list.add(wkt);
    System.out.println(list);
}
```

最后结果展示如图，符合需求了。

![](https://meethigher.top/blog/2022/geometry-grid/8.jpg)

![](https://meethigher.top/blog/2022/geometry-grid/9.jpg)

![](https://meethigher.top/blog/2022/geometry-grid/10.jpg)

总体上，没什么特别复杂的，就是调用geotools提供的api就可以了。

最大的收获，还是发现了这些大佬写的wkt展示的页面吧，页面有点难用，有时间再优化一下，现在先将就用着。