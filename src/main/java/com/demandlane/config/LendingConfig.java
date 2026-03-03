package com.demandlane.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "lending")
public interface LendingConfig {

    @WithDefault("3")
    int maxActiveLoans();

    @WithDefault("14")
    int maxLoanDurationDays();
}
