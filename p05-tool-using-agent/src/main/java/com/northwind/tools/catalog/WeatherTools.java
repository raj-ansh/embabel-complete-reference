package com.northwind.tools.catalog;

import com.embabel.agent.api.annotation.LlmTool;

import java.util.Locale;
import java.util.Map;

/**
 * Tool 2: conditions at a trailhead, so the assistant can recommend suitable gear.
 * A stand-in for a weather API client; the tool contract is what matters here.
 */
public class WeatherTools {

    public record TrailConditions(String location, int lowCelsius, int highCelsius, String outlook) {
    }

    private final Map<String, TrailConditions> forecasts = Map.of(
            "mount rainier", new TrailConditions("Mount Rainier", -4, 7, "snow showers"),
            "zion", new TrailConditions("Zion", 9, 24, "clear"));

    @LlmTool(description = "Get the 3-day forecast summary for a hiking area, to help recommend gear")
    public TrailConditions forecast(@LlmTool.Param(description = "Name of the hiking area, e.g. Zion") String area) {
        return forecasts.getOrDefault(area.toLowerCase(Locale.ROOT).strip(),
                new TrailConditions(area, 0, 0, "no forecast available for this area"));
    }
}
