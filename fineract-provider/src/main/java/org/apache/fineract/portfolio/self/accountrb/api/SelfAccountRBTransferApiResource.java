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
package org.apache.fineract.portfolio.self.accountrb.api;

import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.account.api.AccountTransfersApiResource;
import org.apache.fineract.portfolio.account.service.AccountTransfersReadPlatformService;
import org.apache.fineract.portfolio.self.accountrb.data.SelfAccountRBTemplateData;
import org.apache.fineract.portfolio.self.accountrb.data.SelfAccountRBTransferData;
import org.apache.fineract.portfolio.self.accountrb.data.SelfAccountRBTransferDataValidator;
import org.apache.fineract.portfolio.self.accountrb.exception.BeneficiaryRBTransferLimitExceededException;
import org.apache.fineract.portfolio.self.accountrb.exception.DailyRBTPTTransactionAmountLimitExceededException;
import org.apache.fineract.portfolio.self.accountrb.service.SelfAccountRBTransferReadService;
import org.apache.fineract.portfolio.self.accountrb.service.SelfBeneficiariesRBTPTReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

@Path("/v1/self/accounttransfersrb")
@Component
@Tag(name = "Self Account transfer", description = "")
@RequiredArgsConstructor
public class SelfAccountRBTransferApiResource {

    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<SelfAccountRBTransferData> toApiJsonSerializer;
    private final AccountTransfersApiResource accountTransfersApiResource;
    private final SelfAccountRBTransferReadService selfAccountRBTransferReadService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final SelfAccountRBTransferDataValidator dataValidator;
    private final SelfBeneficiariesRBTPTReadPlatformService tptBeneficiaryReadPlatformService;
    private final ConfigurationDomainService configurationDomainService;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Account Transfer Template", description = "Returns list of loan/savings accounts that can be used for account transfer\n"
            + "\n" + "\n" + "Example Requests:\n" + "\n" + "self/accounttransfers/template\n")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SelfAccountRBTransferApiResourceSwagger.GetAccountTransferTemplateResponse.class)))) })
    public String template(@DefaultValue("") @QueryParam("type") @Parameter(name = "type") final String type,
            @Context final UriInfo uriInfo) {

        AppUser user = this.context.authenticatedUser();
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        Collection<SelfAccountRBTemplateData> selfTemplateData = this.selfAccountRBTransferReadService.retrieveSelfAccountTemplateData(user);

        if (type.equals("tpt")) {
            Collection<SelfAccountRBTemplateData> tptTemplateData = this.tptBeneficiaryReadPlatformService
                    .retrieveTPTSelfAccountTemplateData(user);
            return this.toApiJsonSerializer.serialize(settings, new SelfAccountRBTransferData(selfTemplateData, tptTemplateData));
        }

        return this.toApiJsonSerializer.serialize(settings, new SelfAccountRBTransferData(selfTemplateData, selfTemplateData));
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create new Transfer", description = "Ability to create new transfer of monetary funds from one account to another.\n"
            + "\n" + "\n" + "Example Requests:\n" + "\n" + " self/accounttransfers/\n")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SelfAccountRBTransferApiResourceSwagger.PostNewTransferResponse.class)))) })
    public String create(@DefaultValue("") @QueryParam("type") @Parameter(name = "type") final String type,
            final String apiRequestBodyAsJson) {
        Map<String, Object> params = this.dataValidator.validateCreate(type, apiRequestBodyAsJson);
        if (type.equals("tpt")) {
            checkForLimits(params);
        }
        String jsonRequest = generateCompatibleJson(params);
        return this.accountTransfersApiResource.create(jsonRequest);
    }

    private String generateCompatibleJson(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"toOfficeId\": " + params.get("toOfficeId") + ", ");
        sb.append("\"toClientId\": " + params.get("toClientId") + ", ");
        sb.append("\"toAccountType\": " + params.get("toAccountType") + ", ");
        sb.append("\"toAccountId\": " + params.get("toAccountId") + ", ");
        sb.append("\"transferAmount\": " + params.get("transferAmount") + ", ");
        sb.append("\"transferDate\": " + "\"" + params.get("transferDate") + "\"" + ", ");
        sb.append("\"transferDescription\": " + "\"" + params.get("transferDescription") + "\"" + ", ");
        sb.append("\"dateFormat\": " + "\"" + params.get("dateFormat") + "\"" + ", ");
        sb.append("\"locale\": \"en\", ");
        sb.append("\"fromAccountId\": " + params.get("fromAccountId") + ", ");
        sb.append("\"fromAccountType\": " + params.get("fromAccountType") + ", ");
        sb.append("\"fromClientId\": " + params.get("fromClientId") + ", ");
        sb.append("\"fromOfficeId\": " + params.get("fromOfficeId") + " ");
        sb.append("}");

        return sb.toString();
    }

    private void checkForLimits(Map<String, Object> params) {
        SelfAccountRBTemplateData fromAccount = (SelfAccountRBTemplateData) params.get("fromAccount");
        SelfAccountRBTemplateData toAccount = (SelfAccountRBTemplateData) params.get("toAccount");
        LocalDate transactionDate = (LocalDate) params.get("transactionDate");
        BigDecimal transactionAmount = (BigDecimal) params.get("transactionAmount");

        AppUser user = this.context.authenticatedUser();
        Long transferLimit = this.tptBeneficiaryReadPlatformService.getTransferLimit(user.getId(), toAccount.getAccountId(),
                toAccount.getAccountType());
        if (transferLimit != null && transferLimit > 0) {
            if (transactionAmount.compareTo(new BigDecimal(transferLimit)) > 0) {
                throw new BeneficiaryRBTransferLimitExceededException();
            }
        }

        if (this.configurationDomainService.isDailyTPTLimitEnabled()) {
            Long dailyTPTLimit = this.configurationDomainService.getDailyTPTLimit();
            if (dailyTPTLimit != null && dailyTPTLimit > 0) {
                BigDecimal dailyTPTLimitBD = new BigDecimal(dailyTPTLimit);
                BigDecimal totTransactionAmount = this.accountTransfersReadPlatformService
                        .getTotalTransactionAmount(fromAccount.getAccountId(), fromAccount.getAccountType(), transactionDate);
                if (totTransactionAmount != null && totTransactionAmount.compareTo(BigDecimal.ZERO) > 0) {
                    if (dailyTPTLimitBD.compareTo(totTransactionAmount) <= 0
                            || dailyTPTLimitBD.compareTo(totTransactionAmount.add(transactionAmount)) < 0) {
                        throw new DailyRBTPTTransactionAmountLimitExceededException(fromAccount.getAccountId(), fromAccount.getAccountType());
                    }
                }
            }
        }
    }

}
