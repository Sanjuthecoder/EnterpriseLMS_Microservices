package com.edtech.lms.identity.models.dtos.responses;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
    private Long companyId;
    private String name;
    private Long orgId;
    private String logoUrl;
    private JsonNode themeConfig;
    private String status;
}
