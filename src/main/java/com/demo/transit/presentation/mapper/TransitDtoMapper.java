package com.demo.transit.presentation.mapper;

import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.RoutePoints;
import com.demo.transit.presentation.dto.RouteDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransitDtoMapper {

    @Mapping(target = "stops", source = "points.stops")
    @Mapping(target = "vias", source = "points.vias")
    @Mapping(target = "navigation", source = "navigation")
    RouteDetailResponse toRouteDetailResponse(RoutePoints points, RouteNavigation navigation);
}
