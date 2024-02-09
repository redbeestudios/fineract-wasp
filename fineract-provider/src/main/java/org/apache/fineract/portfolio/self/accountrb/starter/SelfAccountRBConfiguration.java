/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.self.accountrb.starter;

import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.self.accountrb.data.SelfBeneficiariesRBTPTDataValidator;
import org.apache.fineract.portfolio.self.accountrb.domain.SelfBeneficiariesRBTPTRepository;
import org.apache.fineract.portfolio.self.accountrb.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SelfAccountRBConfiguration {

    @Bean
    @ConditionalOnMissingBean(SelfAccountRBTransferReadService.class)
    public SelfAccountRBTransferReadService selfAccountRBTransferReadService(JdbcTemplate jdbcTemplate) {
        return new SelfAccountRBTransferReadServiceImpl(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(SelfBeneficiariesRBTPTReadPlatformService.class)
    public SelfBeneficiariesRBTPTReadPlatformService selfBeneficiariesRBTPTReadPlatformService(PlatformSecurityContext context,
                                                                                             JdbcTemplate jdbcTemplate) {
        return new SelfBeneficiariesRBTPTReadPlatformServiceImpl(context, jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(SelfBeneficiariesRBTPTWritePlatformService.class)
    public SelfBeneficiariesRBTPTWritePlatformService selfBeneficiariesRBTPTWritePlatformService(PlatformSecurityContext context,
                                                                                               SelfBeneficiariesRBTPTRepository repository, SelfBeneficiariesRBTPTDataValidator validator,
                                                                                               LoanRepositoryWrapper loanRepositoryWrapper, SavingsAccountRepositoryWrapper savingRepositoryWrapper) {
        return new SelfBeneficiariesRBTPTWritePlatformServiceImpl(context, repository, validator, loanRepositoryWrapper,
                savingRepositoryWrapper);
    }
}
