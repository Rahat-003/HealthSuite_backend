package com.healthsuite.marketplace.dto.response;

import java.math.BigDecimal;

public record AdminMarketplaceStatsResponse(
    long pendingVerifications,
    long activeClinicians,
    BigDecimal escrowHeldBdt
) {}
