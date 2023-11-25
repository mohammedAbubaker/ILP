package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

public class LngLatSegment {

    public LngLat p;
    public LngLat q;
    public int order;
    public LngLatSegment(LngLat p, LngLat q, int order) {
        this.p = p;
        this.q = q;
        this.order = order;
    }
}