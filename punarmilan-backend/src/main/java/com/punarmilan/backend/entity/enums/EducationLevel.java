package com.punarmilan.backend.entity.enums;

import lombok.Getter;

@Getter
public enum EducationLevel {
    PHD("PhD/Doctorate", 7),
    MASTERS("Masters/Post Graduate", 6),
    BACHELORS("Bachelors/Graduate", 5),
    DIPLOMA("Diploma", 4),
    TWELFTH("12th/Intermediate", 3),
    TENTH("10th/Matriculation", 2),
    NOT_SPECIFIED("Not Specified", 0);

    private final String label;
    private final int rank;

    EducationLevel(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    public static EducationLevel fromLabel(String label) {
        if (label == null || label.isEmpty())
            return NOT_SPECIFIED;

        String cleanLabel = label.toLowerCase();
        for (EducationLevel level : values()) {
            if (level.label.toLowerCase().contains(cleanLabel) ||
                    level.name().toLowerCase().contains(cleanLabel)) {
                return level;
            }
        }
        return NOT_SPECIFIED;
    }
}
