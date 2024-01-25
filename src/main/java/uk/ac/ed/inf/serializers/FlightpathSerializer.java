package uk.ac.ed.inf.serializers;

import uk.ac.ed.inf.pathfinder.DronePathFinder;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.Order;


public class FlightpathSerializer {
    public String orderNo;
    public double fromLongitude;
    public double fromLatitude;
    public double angle;
    public double toLongitude;
    public double toLatitude;

    public FlightpathSerializer(Order order, LngLat p, LngLat q) {
        this.orderNo = order.getOrderNo();
        this.fromLongitude = p.lng();
        this.fromLatitude = p.lat();
        this.angle = DronePathFinder.getAngle(p, q);
        this.toLongitude = q.lng();
        this.toLatitude = q.lat();
    }

    public FlightpathSerializer() {
        this.orderNo = "";
        this.fromLongitude = 0.0;
        this.fromLatitude = 0.0;
        this.angle = 0.0;
        this.toLongitude = 0.0;
        this.toLatitude = 0.0;
    }
}

