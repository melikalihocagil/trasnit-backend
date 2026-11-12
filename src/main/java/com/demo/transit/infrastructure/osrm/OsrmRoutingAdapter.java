package com.demo.transit.infrastructure.osrm;

import com.demo.transit.application.model.Coordinate;
import com.demo.transit.application.model.Leg;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.port.RoutingEngine;
import com.demo.transit.infrastructure.osrm.dto.OsrmLeg;
import com.demo.transit.infrastructure.osrm.dto.OsrmResponse;
import com.demo.transit.infrastructure.osrm.dto.OsrmRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * OSRM dış servisi için Anti-Corruption Layer (ACL) Adaptörü.
 * OSRM'yi 'overview=by_legs&geometries=geojson&steps=false' parametreleriyle çağırarak
 * her bacağın (leg) kendi içinde gelen geometrisini alır ve
 * domain modeli olan RouteNavigation'a dönüştürür.
 */
@Component
public class OsrmRoutingAdapter implements RoutingEngine {

    private static final Logger log = LoggerFactory.getLogger(OsrmRoutingAdapter.class);

    private final RestClient restClient;
    private final OsrmProperties properties;

    @Autowired
    public OsrmRoutingAdapter(OsrmProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.timeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.timeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    // Testlerde izole RestClient mock enjeksiyonu için
    OsrmRoutingAdapter(RestClient restClient, OsrmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public RouteNavigation calculateNavigation(List<Coordinate> coordinates) {
        if (coordinates == null || coordinates.size() < 2) {
            throw new IllegalArgumentException("Rota hesaplama için en az 2 koordinat gereklidir.");
        }

        // lon,lat;lon,lat;... formatı
        String coordsParam = coordinates.stream()
                .map(c -> String.format(Locale.US, "%.6f,%.6f", c.lon(), c.lat()))
                .collect(Collectors.joining(";"));

        String uri = String.format("/route/v1/driving/%s?overview=by_legs&geometries=geojson&steps=false", coordsParam);

        try {
            OsrmResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OsrmResponse.class);

            if (response == null || !"Ok".equalsIgnoreCase(response.code()) || response.routes() == null || response.routes().isEmpty()) {
                throw new OsrmException("OSRM geçersiz yanıt döndü veya rota bulunamadı. Code: " + (response != null ? response.code() : "null"));
            }

            OsrmRoute primaryRoute = response.routes().get(0);
            List<Leg> legs = new ArrayList<>();

            if (primaryRoute.legs() != null) {
                for (OsrmLeg osrmLeg : primaryRoute.legs()) {
                    List<Coordinate> legCoords = new ArrayList<>();
                    if (osrmLeg.geometry() != null && osrmLeg.geometry().coordinates() != null) {
                        for (List<Double> pt : osrmLeg.geometry().coordinates()) {
                            if (pt.size() >= 2) {
                                // GeoJSON: [lon, lat]
                                legCoords.add(new Coordinate(pt.get(1), pt.get(0)));
                            }
                        }
                    }
                    legs.add(new Leg(legCoords, osrmLeg.distance(), osrmLeg.duration()));
                }
            }

            return new RouteNavigation(primaryRoute.distance(), primaryRoute.duration(), legs);
        } catch (OsrmException e) {
            throw e;
        } catch (Exception e) {
            log.error("OSRM servisine erişilirken hata oluştu: {}", e.getMessage(), e);
            throw new OsrmException("OSRM yönlendirme servisi çağrılırken hata oluştu: " + e.getMessage(), e);
        }
    }
}
