package uk.ac.ed.inf.serializers.geojson;

/**
* Manual GeoJson formatting
* Responsible for containing the main FeatureCollection to be deserialized.
*/
public class GeoJson {
    public String type;
    public Feature[] features;

    public GeoJson(Feature[] features) {
        this.type = "FeatureCollection";
        this.features = features;
    }
}
