package uk.ac.ed.inf.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.ilp.data.LngLat;

import java.util.ArrayList;

/*
Responsible for handling geometry
* */
public class Geometry {
    private String type;
    private Object coordinates;
    public Geometry(LngLat[] coordinates, String type) {
        this.type = type;
        ArrayList<double[]> arr = new ArrayList<>();
        for (LngLat coord: coordinates) {
            arr.add(new double[]{coord.lng(), coord.lat()});
        }
        if (type.equals("LineString")) {
            // linestring coordinates are in the format {c1, c2, c3, ...}
            this.coordinates = arr.toArray(new double[0][0]);
        }
        else {
            // otherwise for polygons it must be in the form {{c1, c2, c3, ..., c1}}
            arr.add(arr.get(0));
            this.coordinates = new double[][][]{arr.toArray(new double[][]{})};
        }
    }
}
