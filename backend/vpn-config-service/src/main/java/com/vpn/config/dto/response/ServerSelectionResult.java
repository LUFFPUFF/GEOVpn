package com.vpn.config.dto.response;

import com.vpn.common.dto.ServerDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Результат выбора сервера с метриками
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerSelectionResult {

    private ServerDto server;
    private Double totalScore;
    private ScoreBreakdown scoreBreakdown;
    private String selectionReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        private Double latencyScore;
        private Double loadScore;
        private Double geographyScore;
        private Double healthScore;
        private Double protocolScore;
    }
}
