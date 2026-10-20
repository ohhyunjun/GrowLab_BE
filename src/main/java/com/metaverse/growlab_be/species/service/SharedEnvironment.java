package com.metaverse.growlab_be.species.service;

import com.metaverse.growlab_be.species.domain.Species;

import java.util.Collection;

/** One physical environment shared by every planted port of a device. */
public record SharedEnvironment(Range temperature, Range humidity, Range ph,
                                Range tds, Range lightHours) {
    public record Range(Double min, Double max) {
        public boolean outside(Number value) {
            if (value == null) return false;
            double measured = value.doubleValue();
            return (min != null && measured < min) || (max != null && measured > max);
        }
    }

    public static void validateSpecies(Species species) {
        validate("온도", species.getMinTemperature(), species.getMaxTemperature());
        validate("습도", species.getMinHumidity(), species.getMaxHumidity());
        validate("pH", species.getMinPh(), species.getMaxPh());
        validate("TDS", species.getMinTds(), species.getMaxTds());
        validate("조명 시간", species.getMinLightHours(), species.getMaxLightHours());
    }

    /** Mixed species need complete ranges so compatibility is actually known. */
    public static SharedEnvironment requireMixedCompatibility(Collection<Species> species) {
        for (Species item : species) {
            validateSpecies(item);
            requireComplete(item.getName(), "온도", item.getMinTemperature(), item.getMaxTemperature());
            requireComplete(item.getName(), "습도", item.getMinHumidity(), item.getMaxHumidity());
            requireComplete(item.getName(), "pH", item.getMinPh(), item.getMaxPh());
            requireComplete(item.getName(), "TDS", item.getMinTds(), item.getMaxTds());
            requireComplete(item.getName(), "조명 시간", item.getMinLightHours(), item.getMaxLightHours());
        }
        return fromSpecies(species);
    }

    public static SharedEnvironment fromSpecies(Collection<Species> species) {
        Range temperature = new Range(null, null);
        Range humidity = new Range(null, null);
        Range ph = new Range(null, null);
        Range tds = new Range(null, null);
        Range lightHours = new Range(null, null);
        for (Species item : species) {
            validateSpecies(item);
            temperature = intersect("온도", temperature, item.getMinTemperature(), item.getMaxTemperature());
            humidity = intersect("습도", humidity, item.getMinHumidity(), item.getMaxHumidity());
            ph = intersect("pH", ph, item.getMinPh(), item.getMaxPh());
            tds = intersect("TDS", tds, item.getMinTds(), item.getMaxTds());
            lightHours = intersect("조명 시간", lightHours, item.getMinLightHours(), item.getMaxLightHours());
        }
        return new SharedEnvironment(temperature, humidity, ph, tds, lightHours);
    }

    private static void requireComplete(String species, String label, Double min, Double max) {
        if (min == null || max == null) {
            throw new IllegalStateException(species + " 품종의 " + label + " 적정 범위가 없어 혼합 재배를 검증할 수 없습니다.");
        }
    }

    private static void validate(String label, Double min, Double max) {
        if ((min != null && !Double.isFinite(min)) || (max != null && !Double.isFinite(max))
                || (min != null && max != null && min > max)) {
            throw new IllegalArgumentException(label + " 적정 범위가 올바르지 않습니다.");
        }
    }

    private static Range intersect(String label, Range current, Double min, Double max) {
        Double lower = current.min();
        if (min != null) lower = lower == null ? min : Math.max(lower, min);
        Double upper = current.max();
        if (max != null) upper = upper == null ? max : Math.min(upper, max);
        if (lower != null && upper != null && lower > upper) {
            throw new IllegalStateException("심은 품종들의 " + label + " 적정 범위가 겹치지 않습니다.");
        }
        return new Range(lower, upper);
    }
}
