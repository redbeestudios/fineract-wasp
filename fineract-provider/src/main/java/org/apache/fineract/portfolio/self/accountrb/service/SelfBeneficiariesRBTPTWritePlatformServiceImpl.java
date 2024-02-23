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
package org.apache.fineract.portfolio.self.accountrb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.self.accountrb.data.SelfBeneficiariesRBTPTDataValidator;
import org.apache.fineract.portfolio.self.accountrb.domain.SelfBeneficiariesRBTPT;
import org.apache.fineract.portfolio.self.accountrb.domain.SelfBeneficiariesRBTPTRepository;
import org.apache.fineract.portfolio.self.accountrb.exception.InvalidRBAccountInformationException;
import org.apache.fineract.portfolio.self.accountrb.exception.InvalidRBBeneficiaryException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.apache.fineract.portfolio.self.accountrb.api.SelfBeneficiariesRBTPTApiConstants.*;

@RequiredArgsConstructor
@Slf4j
public class SelfBeneficiariesRBTPTWritePlatformServiceImpl implements SelfBeneficiariesRBTPTWritePlatformService {

    private final PlatformSecurityContext context;
    private final SelfBeneficiariesRBTPTRepository repository;
    private final SelfBeneficiariesRBTPTDataValidator validator;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final SavingsAccountRepositoryWrapper savingRepositoryWrapper;

    @Transactional
    @Override
    public CommandProcessingResult add(JsonCommand command) {
        HashMap<String, Object> params = this.validator.validateForCreate(command.json());

        String name = (String) params.get(NAME_PARAM_NAME);
        Integer accountType = (Integer) params.get(ACCOUNT_TYPE_PARAM_NAME);
        String accountNumber = (String) params.get(ACCOUNT_NUMBER_PARAM_NAME);
        String accountName  = (String) params.get(ACCOUNT_NAME_PARAM_NAME);
        Long accountId = null;
        String institutionName = (String) params.get(INSTITUTION_NAME_PARAM_NAME);
        String institutionCode = (String) params.get(INSTITUTION_CODE_PARAM_NAME);
        String currencyCode = (String) params.get(CURRENCY_CODE_PARAM_NAME);
        Long transferLimit = (Long) params.get(TRANSFER_LIMIT_PARAM_NAME);

        //Long accountId = null;
        Long clientId = null;
        Long officeId = null;

        boolean validAccountDetails = true;
        if (LOCAL_INSTITUTION_NAME.equals(institutionName) &&
                accountType.equals(PortfolioAccountType.SAVINGS.getValue()) &&
                accountId != null) {
            SavingsAccount savings = this.savingRepositoryWrapper.findNonClosedAccountByAccountNumber(accountNumber);
            if (savings != null && savings.getClient() != null) {
                accountId = savings.getId();
                clientId = savings.getClient().getId();
                officeId = savings.getClient().getOffice().getId();
            } else {
                validAccountDetails = false;
            }
        }
        /* else {
            Loan loan = this.loanRepositoryWrapper.findNonClosedLoanByAccountNumber(accountNumber);
            if (loan != null && loan.getClientId() != null && loan.getOffice().getName().equals(officeName)) {
                accountId = loan.getId();
                officeId = loan.getOfficeId();
                clientId = loan.getClientId();
            } else {
                validAccountDetails = false;
            }
        }
        */

        if (validAccountDetails) {
            try {
                AppUser user = this.context.authenticatedUser();
                SelfBeneficiariesRBTPT beneficiary = new SelfBeneficiariesRBTPT(
                        user.getId(),
                        name,
                        accountName,
                        accountNumber,
                        accountId,
                        accountType,
                        institutionName,
                        transferLimit,
                        institutionCode,
                        currencyCode);
                this.repository.saveAndFlush(beneficiary);
                return new CommandProcessingResultBuilder().withEntityId(beneficiary.getId()).build();
            } catch (DataAccessException dae) {
                handleDataIntegrityIssues(command, dae);
            }
        }
        throw new InvalidRBAccountInformationException(accountNumber, PortfolioAccountType.fromInt(accountType).getCode());

    }

    @Transactional
    @Override
    public CommandProcessingResult update(JsonCommand command) {
        HashMap<String, Object> params = this.validator.validateForUpdate(command.json());
        AppUser user = this.context.authenticatedUser();
        Long beneficiaryId = command.entityId();
        SelfBeneficiariesRBTPT beneficiary = this.repository.findById(beneficiaryId).orElse(null);
        if (beneficiary != null && beneficiary.getAppUserId().equals(user.getId())) {
            String name = (String) params.get(NAME_PARAM_NAME);
            Long transferLimit = (Long) params.get(TRANSFER_LIMIT_PARAM_NAME);

            Map<String, Object> changes = beneficiary.update(name, transferLimit);
            if (!changes.isEmpty()) {
                try {
                    this.repository.saveAndFlush(beneficiary);

                    return new CommandProcessingResultBuilder() //
                            .withEntityId(beneficiary.getId()) //
                            .with(changes).build();
                } catch (DataAccessException dae) {
                    handleDataIntegrityIssues(command, dae);
                }

            }
        }
        throw new InvalidRBBeneficiaryException(beneficiaryId);
    }

    @Transactional
    @Override
    public CommandProcessingResult delete(JsonCommand command) {
        AppUser user = this.context.authenticatedUser();
        Long beneficiaryId = command.entityId();
        SelfBeneficiariesRBTPT beneficiary = this.repository.findById(beneficiaryId).orElse(null);
        if (beneficiary != null && beneficiary.getAppUserId().equals(user.getId())) {

            beneficiary.setActive(false);
            this.repository.save(beneficiary);

            return new CommandProcessingResultBuilder() //
                    .withEntityId(beneficiary.getId()) //
                    .build();
        }
        throw new InvalidRBBeneficiaryException(beneficiaryId);
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final DataAccessException dae) {
        final Throwable realCause = dae.getMostSpecificCause();
        if (realCause.getMessage().contains("name")) {

            final String name = command.stringValueOfParameterNamed(NAME_PARAM_NAME);
            throw new PlatformDataIntegrityException("error.msg.beneficiary.duplicate.name",
                    "Beneficiary with name `" + name + "` already exists", NAME_PARAM_NAME, name);
        }

        log.error("Error occured.", dae);
        throw ErrorHandler.getMappable(dae, "error.msg.beneficiary.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
