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
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.self.accountrb.api.SelfBeneficiariesRBTPTApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.*;

import static org.apache.fineract.portfolio.self.accountrb.api.SelfBeneficiariesRBTPTApiConstants.*;

@Component
public class SelfBeneficiariesRBTPTDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private static final Set<String> CREATE_REQUEST_DATA_PARAMETERS =
            new HashSet<>(
                    Arrays.asList(SelfBeneficiariesRBTPTApiConstants.LOCALE,
                    NAME_PARAM_NAME,
                    ACCOUNT_NUMBER_PARAM_NAME,
                    ACCOUNT_TYPE_PARAM_NAME,
                    TRANSFER_LIMIT_PARAM_NAME,
                    INSTITUTION_NAME_PARAM_NAME,
                    ACCOUNT_NAME_PARAM_NAME,
                    ACCOUNT_ID_PARAM_NAME
                    ));


    private static final Set<String> UPDATE_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(NAME_PARAM_NAME, TRANSFER_LIMIT_PARAM_NAME));

    @Autowired
    public SelfBeneficiariesRBTPTDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public HashMap<String, Object> validateForCreate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CREATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(RESOURCE_NAME);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper.extractStringNamed(NAME_PARAM_NAME, element);
        baseDataValidator.reset().parameter(NAME_PARAM_NAME).value(name).notBlank().notExceedingLengthOf(50);

        final String accountName = this.fromApiJsonHelper.extractStringNamed(ACCOUNT_NAME_PARAM_NAME, element);
        baseDataValidator.reset().parameter(NAME_PARAM_NAME).value(name).notBlank().notExceedingLengthOf(50);

        final String institutionName = this.fromApiJsonHelper.extractStringNamed(INSTITUTION_NAME_PARAM_NAME, element);
        baseDataValidator.reset().parameter(INSTITUTION_NAME_PARAM_NAME).value(institutionName).notBlank().notExceedingLengthOf(50);

        final String accountNumber = this.fromApiJsonHelper.extractStringNamed(ACCOUNT_NUMBER_PARAM_NAME, element);
        baseDataValidator.reset().parameter(ACCOUNT_NUMBER_PARAM_NAME).value(accountNumber).notBlank().notExceedingLengthOf(20);

        final Integer accountType = this.fromApiJsonHelper.extractIntegerNamed(ACCOUNT_TYPE_PARAM_NAME, element,
                this.fromApiJsonHelper.extractLocaleParameter(element.getAsJsonObject()));
        baseDataValidator.reset().parameter(ACCOUNT_TYPE_PARAM_NAME).value(accountType).notNull()
                .isOneOfTheseValues(PortfolioAccountType.LOAN.getValue(), PortfolioAccountType.SAVINGS.getValue());

        final Long transferLimit = this.fromApiJsonHelper.extractLongNamed(TRANSFER_LIMIT_PARAM_NAME, element);
        baseDataValidator.reset().parameter(TRANSFER_LIMIT_PARAM_NAME).value(transferLimit).ignoreIfNull().longGreaterThanZero();

        final Long accountId = this.fromApiJsonHelper.extractLongNamed(ACCOUNT_ID_PARAM_NAME, element);
        baseDataValidator.reset().parameter(TRANSFER_LIMIT_PARAM_NAME).value(transferLimit).ignoreIfNull().longGreaterThanZero();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        HashMap<String, Object> ret = new HashMap<>();
        ret.put(NAME_PARAM_NAME, name);
        ret.put(INSTITUTION_NAME_PARAM_NAME, institutionName);
        ret.put(ACCOUNT_NUMBER_PARAM_NAME, accountNumber);
        ret.put(ACCOUNT_NAME_PARAM_NAME, accountName);
        ret.put(ACCOUNT_TYPE_PARAM_NAME, accountType);
        ret.put(TRANSFER_LIMIT_PARAM_NAME, transferLimit);
        ret.put(ACCOUNT_ID_PARAM_NAME, transferLimit);

        return ret;
    }

    public HashMap<String, Object> validateForUpdate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, UPDATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(RESOURCE_NAME);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        HashMap<String, Object> ret = new HashMap<>();

        if (this.fromApiJsonHelper.parameterExists(NAME_PARAM_NAME, element)) {
            final String name = this.fromApiJsonHelper.extractStringNamed(NAME_PARAM_NAME, element);
            baseDataValidator.reset().parameter(NAME_PARAM_NAME).value(name).notBlank().notExceedingLengthOf(50);
            ret.put(NAME_PARAM_NAME, name);
        }

        if (this.fromApiJsonHelper.parameterExists(TRANSFER_LIMIT_PARAM_NAME, element)) {
            final Long transferLimit = this.fromApiJsonHelper.extractLongNamed(TRANSFER_LIMIT_PARAM_NAME, element);
            baseDataValidator.reset().parameter(TRANSFER_LIMIT_PARAM_NAME).value(transferLimit).ignoreIfNull().longGreaterThanZero();
            ret.put(TRANSFER_LIMIT_PARAM_NAME, transferLimit);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return ret;
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

}
