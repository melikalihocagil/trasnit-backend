package com.demo.transit.presentation.dto;

import com.demo.transit.application.model.Stop;

import java.util.List;

public record StopsResponse(
        List<Stop> published,
        List<Stop> draft
) {}
