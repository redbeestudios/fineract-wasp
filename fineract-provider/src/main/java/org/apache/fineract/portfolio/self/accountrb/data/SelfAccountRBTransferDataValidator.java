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
package org.apache.fineract.portfolio.self.accountrb.data;

import com.google.gson.JsonElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.self.accountrb.service.SelfAccountRBTransferReadService;
import org.apache.fineract.portfolio.self.accountrb.service.SelfBeneficiariesRBTPTReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

import static org.apache.fineract.portfolio.account.AccountDetailConstants.*;
import static org.apache.fineract.portfolio.account.api.AccountTransfersApiConstants.*;
import static org.apache.fineract.portfolio.search.SearchConstants.API_PARAM_COLUMN;

@Component
public class SelfAccountRBTransferDataValidator {

    private final PlatformSecurityContext context;
    private final SelfAccountRBTransferReadService selfAccountRBTransferReadService;
    private final SelfBeneficiariesRBTPTReadPlatformService tptBeneficiaryReadPlatformService;
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public SelfAccountRBTransferDataValidator(final PlatformSecurityContext context,
                                              final SelfAccountRBTransferReadService selfAccountRBTransferReadService,
                                              final SelfBeneficiariesRBTPTReadPlatformService tptBeneficiaryReadPlatformService, final FromJsonHelper fromApiJsonHelper) {
        this.context = context;
        this.selfAccountRBTransferReadService = selfAccountRBTransferReadService;
        this.tptBeneficiaryReadPlatformService = tptBeneficiaryReadPlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public Map<String, Object> validateCreate(String type, String apiRequestBodyAsJson) {
        if (StringUtils.isBlank(apiRequestBodyAsJson)) {
            throw new InvalidJsonException();
        }

        JsonElement element = this.fromApiJsonHelper.parse(apiRequestBodyAsJson);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ACCOUNT_TRANSFER_RESOURCE_NAME);

        final BigDecimal transactionAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(transferAmountParamName, element);
        baseDataValidator.reset().parameter(transferAmountParamName).value(transactionAmount).notNull().positiveAmount();

        final String transactionDescription = this.fromApiJsonHelper.extractStringNamed(transferDescriptionParamName, element);
        baseDataValidator.reset().parameter(transferDescriptionParamName).value(transactionDescription).notBlank()
                .notExceedingLengthOf(200);

        final String toAccountNumber = this.fromApiJsonHelper.extractStringNamed("toAccountNumber", element);
        baseDataValidator.reset().parameter(transferDescriptionParamName).value(transactionDescription).notBlank()
                .notExceedingLengthOf(22);

//        final Integer fromAccountType = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(fromAccountTypeParamName, element);
//        baseDataValidator.reset().parameter(fromAccountTypeParamName).value(fromAccountType).notNull()
//                .isOneOfTheseValues(Integer.valueOf(1), Integer.valueOf(2));

//        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(transferDateParamName, element);
//        baseDataValidator.reset().parameter(transferDateParamName).value(transactionDate).notNull();

        AppUser user = this.context.authenticatedUser();
        Collection<SelfAccountRBTemplateData> validFromAccounts = this.selfAccountRBTransferReadService.retrieveSelfAccountTemplateData(user);
        if (validFromAccounts.size() != 1) {
            dataValidationErrors.add(ApiParameterError.generalError("account.not.found", "User should have only one account, has: " + validFromAccounts.size()));
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        Collection<SelfAccountRBTemplateData> validToAccounts = validFromAccounts;
        if (type.equals("tpt")) {
            validToAccounts = this.tptBeneficiaryReadPlatformService.retrieveTPTSelfAccountTemplateData(user);
        }

        Iterator<SelfAccountRBTemplateData> iterator = validFromAccounts.iterator();
        SelfAccountRBTemplateData fromAccountTD = iterator.next();

        // Find destination account
        // Can be local (has accountId setted) or external (accountId is null)
        SelfAccountRBTemplateData validToAccount = null;
        for (SelfAccountRBTemplateData toAccount : validToAccounts) {
            if (toAccountNumber.equals(toAccount.getAccountNumber()) && toAccount.getAccountId() != null) {
                validToAccount = toAccount;
                break;
            }
        }

        if (validToAccount == null) {
            dataValidationErrors.add(ApiParameterError.parameterError("invalid.to.account.details", "toAccount is not internal, external accounts not implemented yet", "toAccount"));
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        SelfAccountRBTemplateData fromAccount = new SelfAccountRBTemplateData(fromAccountTD.getAccountId(), fromAccountTD.getAccountType(), fromAccountTD.getClientId(), fromAccountTD.getOfficeId());
        SelfAccountRBTemplateData toAccount = validToAccount; //new SelfAccountRBTemplateData(toAccountId, toAccountType, toClientId, toOfficeId);

        validateUserAccounts(fromAccount, toAccount, baseDataValidator, type);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        LocalDate ld = LocalDate.now();
        String date = ld.getDayOfMonth() + " " + ld.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ld.getYear();

        Map<String, Object> ret = new HashMap<>();
        ret.put("fromAccount", fromAccount);
        ret.put("toAccount", toAccount);
        ret.put("transactionDate", LocalDate.now());
        ret.put("transactionAmount", transactionAmount);


        ret.put("toOfficeId", toAccount.getOfficeId());
        ret.put("toClientId", toAccount.getClientId());
        ret.put("toAccountType", toAccount.getAccountType());
        ret.put("toAccountId", toAccount.getAccountId());
        ret.put("transferAmount", transactionAmount);
        ret.put("transferDate", date);
        ret.put("transferDescription", transactionDescription);
        ret.put("dateFormat", "dd MMMM yyyy");
        ret.put("fromAccountId", fromAccount.getAccountId());
        ret.put("fromAccountType", fromAccount.getAccountType());
        ret.put("fromClientId", fromAccount.getClientId());
        ret.put("fromOfficeId", fromAccount.getOfficeId());

        return ret;

    }

    private void validateUserAccounts(final SelfAccountRBTemplateData fromAccount, final SelfAccountRBTemplateData toAccount,
                                      final DataValidatorBuilder baseDataValidator, final String type) {
        AppUser user = this.context.authenticatedUser();
        Collection<SelfAccountRBTemplateData> validFromAccounts = this.selfAccountRBTransferReadService.retrieveSelfAccountTemplateData(user);

        Collection<SelfAccountRBTemplateData> validToAccounts = validFromAccounts;
        if (type.equals("tpt")) {
            validToAccounts = this.tptBeneficiaryReadPlatformService.retrieveTPTSelfAccountTemplateData(user);
        }

        boolean validFromAccount = false;
        for (SelfAccountRBTemplateData validAccount : validFromAccounts) {
            if (validAccount.equals(fromAccount)) {
                validFromAccount = true;
                break;
            }
        }

        boolean validToAccount = false;
        for (SelfAccountRBTemplateData validAccount : validToAccounts) {
            if (validAccount.equals(toAccount)) {
                validToAccount = true;
                break;
            }
        }

        if (!validFromAccount) {
            baseDataValidator.reset().failWithCode("invalid.from.account.details",
                    "Source account details doesn't match with valid user account details.");
        }

        if (!validToAccount) {
            baseDataValidator.reset().failWithCode("invalid.to.account.details",
                    "Destination account details doesn't match with valid user account details.");
        }

        if (fromAccount.equals(toAccount)) {
            baseDataValidator.reset().failWithCode("same.from.to.account.details", "Source and Destination account details are same.");
        }

    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
