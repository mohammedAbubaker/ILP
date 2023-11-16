package uk.ac.ed.inf.geojson;

/*
Manual GeoJson formatting
Responsible for containing the main FeatureCollection
* */
public class GeoJson {
    private String type;
    private Feature[] features;

    public GeoJson(Feature[] features) {
        this.type = "FeatureCollection";
        this.features = features;
    }
}
