package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.agent.AgentContractSpec;

import java.util.List;

public record AgentContractsResponse(List<ContractDto> contracts) {
    public static AgentContractsResponse from(List<AgentContractSpec> contracts) {
        return new AgentContractsResponse(contracts.stream().map(ContractDto::from).toList());
    }

    public record ContractDto(
            String agentType,
            String role,
            String inputSchema,
            String outputSchema,
            String explanationFormat,
            List<String> warningsSchema,
            List<String> recommendationSchema,
            List<String> guardrails
    ) {
        static ContractDto from(AgentContractSpec contract) {
            return new ContractDto(
                    contract.agentType().name(),
                    contract.role(),
                    contract.inputSchema(),
                    contract.outputSchema(),
                    contract.explanationFormat(),
                    contract.warningsSchema(),
                    contract.recommendationSchema(),
                    contract.guardrails()
            );
        }
    }
}
