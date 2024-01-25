package uk.ac.ed.inf.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.serializers.geojson.Feature;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.serializers.geojson.GeoJson;
import uk.ac.ed.inf.serializers.geojson.Geometry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;


public class Serializer {
    // this arraylist will be written out to flightpath[date].json
    ArrayList<FlightpathSerializer> flightPathSerializers;

    // this arraylist will be written out to deliveries[date].json
    ArrayList<DeliverySerializer> deliveriesSerializers;

    // this arraylist will be written out to drone[date].geojson
    ArrayList<Feature> featureCollection;

    /**
     * Responsible for writing the result of executing the program into 3 separate files: flightpath[date].json,
     * deliveries[date].json and drone[date].json.
     * */
    public Serializer() {
        // initialise
        this.flightPathSerializers = new ArrayList<>();
        this.deliveriesSerializers = new ArrayList<>();
        this.featureCollection = new ArrayList<>();
    }

    // after every order is processed, add processed information to the respective buckets
    public void addToFlightPathSerializer(Order order, LngLat[] flightpath) {
        for (int i = 0; i < flightpath.length - 1; i++) {
            this.flightPathSerializers
                    .add(new FlightpathSerializer(order, flightpath[i], flightpath[i+1]));
        }
    }
    public void addToDeliverySerializer(Order order, LngLat src, LngLat dest) {
        DeliverySerializer deliverySerializer = new DeliverySerializer(order, src, dest);
        this.deliveriesSerializers.add(deliverySerializer);
    }
    public void addToFeatureCollection(LngLat[] dronePath) {
        featureCollection.add(new Feature(new Geometry(dronePath, "LineString"), "dronePath"));
    }

    /**
     * This function is responsible for writing the result files, appending the date to the filename.
     * */
    public void serialize(String date) {
        // write json string to file with date
        outputFlightpath(date);
        outputDeliveries(date);
        outputMap(date);
    }

    /**
     * This function is used to write an object out to JSON format.
     * @param path Determines the filename of the outputted file
     * */
    private static void outputFile(String path, Object o) {
        File f = new File(path);
        try {
            // initialise
            FileWriter fileWriter = new FileWriter(f, false);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            ObjectMapper objectMapper = new ObjectMapper();
            //write
            writer.write(objectMapper.writeValueAsString(o));
            // close
            writer.close();
            fileWriter.close();
        }

        catch (Exception e) {
            System.err.println("Error writing to: " + path + ". Program terminating...");
        }
    }

    private void outputFlightpath(String date) {
        // if no flightpath data exists, don't write.
        if (this.flightPathSerializers.isEmpty())  {
            return;
        }

        String path = "flightpath";
        if (!date.isBlank()) {
            path += ("-" + date);
        }
        path += ".json";
        outputFile(path, this.flightPathSerializers);
    }

    private void outputMap(String date) {
        // if no geojson data exists, don't write.
        if (this.featureCollection.isEmpty()) {
            return;
        }

        String path = "drone";
        if (!date.isBlank()) {
            path += ("-" + date);
        }
        path += ".json";
        GeoJson geoJson = new GeoJson(this.featureCollection.toArray(new Feature[0]));
        outputFile(path, geoJson);
    }

    private void outputDeliveries(String date) {
        // if no delivery data exists, don't write
        if (this.deliveriesSerializers.isEmpty()) {
            return;
        }

        String path = "deliveries";
        if (!date.isBlank()) {
            path += ("-" + date);
        }
        path += ".json";
        outputFile(path, this.deliveriesSerializers);
    }
}
