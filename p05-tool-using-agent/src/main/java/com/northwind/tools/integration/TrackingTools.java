package com.northwind.tools.integration;

import com.embabel.agent.api.annotation.LlmTool;

/** Tool 7: a REST API call, validated and bounded before it leaves the application. */
public class TrackingTools {

    private final CarrierClient carrier;

    public TrackingTools(CarrierClient carrier) {
        this.carrier = carrier;
    }

    @LlmTool(description = "Track a parcel with the carrier using its tracking number")
    public CarrierClient.TrackingInfo trackParcel(
            @LlmTool.Param(description = "Carrier tracking number, e.g. PF-883120") String trackingNumber) {
        if (!trackingNumber.matches("PF-\\d{6}")) {
            return new CarrierClient.TrackingInfo(trackingNumber, "INVALID", "Not a valid tracking number");
        }
        return carrier.track(trackingNumber);
    }
}
