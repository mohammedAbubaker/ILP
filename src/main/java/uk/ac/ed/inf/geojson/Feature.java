package uk.ac.ed.inf.geojson;
/*
Responsible for representing features
* */
public class Feature {
    private String type;
    private Properties properties;
    private Geometry geometry;

    public Feature(Geometry geometry, String propertyName) {
        this.type = "feature";
        this.geometry = geometry;
        this.properties = new Properties(propertyName);
    }
}
