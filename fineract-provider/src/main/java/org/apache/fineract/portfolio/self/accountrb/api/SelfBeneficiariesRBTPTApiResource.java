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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.service.AccountTransferEnumerations;
import org.apache.fineract.portfolio.self.accountrb.data.SelfBeneficiariesRBTPTData;
import org.apache.fineract.portfolio.self.accountrb.service.SelfBeneficiariesRBTPTReadPlatformService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Path("/v1/self/beneficiariesrb/tpt")
@Component
@Tag(name = "Self Third Party Transfer", description = "")
@RequiredArgsConstructor
public class SelfBeneficiariesRBTPTApiResource {

    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<SelfBeneficiariesRBTPTData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final SelfBeneficiariesRBTPTReadPlatformService readPlatformService;
    private static final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
            SelfBeneficiariesRBTPTApiConstants.NAME_PARAM_NAME, SelfBeneficiariesRBTPTApiConstants.INSTITUTION_NAME_PARAM_NAME,
            SelfBeneficiariesRBTPTApiConstants.ACCOUNT_NUMBER_PARAM_NAME, SelfBeneficiariesRBTPTApiConstants.ACCOUNT_TYPE_PARAM_NAME,
            SelfBeneficiariesRBTPTApiConstants.TRANSFER_LIMIT_PARAM_NAME, SelfBeneficiariesRBTPTApiConstants.ID_PARAM_NAME,
            SelfBeneficiariesRBTPTApiConstants.CLIENT_NAME_PARAM_NAME, SelfBeneficiariesRBTPTApiConstants.ACCOUNT_TYPE_OPTIONS_PARAM_NAME));

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Beneficiary Third Party Transfer Template", description = "Returns Account Type enumerations. Self User is expected to know office name and account number to be able to add beneficiary.\n"
            + "\n" + "Example Requests:\n" + "\n" + "/self/beneficiariesrb/tpt/template")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SelfBeneficiariesRBTPTApiResourceSwagger.GetSelfBeneficiariesTPTTemplateResponse.class))) })
    public String template(@Context final UriInfo uriInfo) {

        final EnumOptionData loanAccountType = AccountTransferEnumerations.accountType(PortfolioAccountType.LOAN);
        final EnumOptionData savingsAccountType = AccountTransferEnumerations.accountType(PortfolioAccountType.SAVINGS);

        final Collection<EnumOptionData> accountTypeOptions = Arrays.asList(savingsAccountType, loanAccountType);

        SelfBeneficiariesRBTPTData templateData = new SelfBeneficiariesRBTPTData(accountTypeOptions);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, templateData, RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Add TPT Beneficiary", description = "Api to add third party beneficiary linked to current user.\n" + "\n"
            + "Parameter Definitions\n" + "\n" + "name : Nick name for beneficiary, should be unique for an self service user\n" + "\n"
            + "institutionName : Institution account belongs to (MercadoPago)\n" + "\n" + "accountNumber : Account Number of beneficiary(not id)\n" + "\n"
            + "transferLimit : Each transfer initiated to this account will not exceed this amount\n" + "\n" + "Example Requests:\n" + "\n"
            + "/self/beneficiariesrb/tpt\n\n" + "Mandatory Fields: name, officeName, accountNumber, accountType\n\n"
            + "Optional Fields: transferLimit")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SelfBeneficiariesRBTPTApiResourceSwagger.PostSelfBeneficiariesTPTRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SelfBeneficiariesRBTPTApiResourceSwagger.PostSelfBeneficiariesTPTResponse.class))) })
    public String add(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().addSelfServiceBeneficiaryTPT().withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{beneficiaryId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update TPT Beneficiary", description = "Api to update third party beneficiary linked to current user.\n" + "\n"
            + "Example Requests:\n" + "\n" + "/self/beneficiariesrb/tpt/{beneficiaryId}\n\n" + "Optional Fields: name, transferLimit")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SelfBeneficiariesRBTPTApiResourceSwagger.PutSelfBeneficiariesTPTBeneficiaryIdRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SelfBeneficiariesRBTPTApiResourceSwagger.PutSelfBeneficiariesTPTBeneficiaryIdResponse.class))) })
    public String update(@PathParam("beneficiaryId") @Parameter(description = "beneficiaryId") final Long beneficiaryId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateSelfServiceBeneficiaryTPT(beneficiaryId)
                .withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{beneficiaryId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete TPT Beneficiary", description = "Api to delete third party beneficiary linked to current user.\n" + "\n"
            + "Example Requests:\n" + "\n" + "/self/beneficiariesrb/tpt/{beneficiaryId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SelfBeneficiariesRBTPTApiResourceSwagger.DeleteSelfBeneficiariesTPTBeneficiaryIdResponse.class))) })
    public String delete(@PathParam("beneficiaryId") final Long beneficiaryId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteSelfServiceBeneficiaryTPT(beneficiaryId)
                .withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Get All TPT Beneficiary", description = "Api to get all third party beneficiary linked to current user.\n" + "\n"
            + "Example Requests:\n" + "\n" + "/self/beneficiariesrb/tpt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SelfBeneficiariesRBTPTApiResourceSwagger.GetSelfBeneficiariesTPTResponse.class)))) })
    public String retrieveAll(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(SelfBeneficiariesRBTPTApiConstants.BENEFICIARY_ENTITY_NAME);

        final Collection<SelfBeneficiariesRBTPTData> beneficiaries = this.readPlatformService.retrieveAll();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, beneficiaries, RESPONSE_DATA_PARAMETERS);
    }

}
