package uk.ac.ed.inf.serializers;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.Order;

public class DeliverySerializer {
    public String orderNo;
    public String orderStatus;
    public String orderValidationCode;
    public String costInPence;
    public double srcLongitude;
    public double srcLatitude;
    public double destLongitude;
    public double destLatitude;

    public DeliverySerializer(Order order, LngLat src, LngLat dest) {
        this.orderNo = order.getOrderNo();
        this.orderStatus = order.getOrderStatus().toString();
        this.orderValidationCode = order.getOrderValidationCode().toString();
        this.costInPence = "" + order.getPriceTotalInPence();
        this.srcLongitude = src.lng();
        this.srcLatitude = src.lat();
        this.destLongitude = dest.lng();
        this.destLatitude = dest.lat();
    }
}
