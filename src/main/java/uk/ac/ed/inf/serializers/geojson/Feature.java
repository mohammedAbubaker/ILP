package uk.ac.ed.inf.serializers.geojson;

/**
* Responsible for representing features
*/
public class Feature {
    public String type;
    public Properties properties;
    public Geometry geometry;

    public Feature(Geometry geometry, String propertyName) {
        this.type = "Feature";
        this.geometry = geometry;
        this.properties = new Properties(propertyName);
    }
}
