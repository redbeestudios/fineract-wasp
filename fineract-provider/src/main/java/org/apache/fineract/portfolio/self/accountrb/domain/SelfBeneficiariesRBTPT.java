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
package org.apache.fineract.portfolio.self.accountrb.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import java.util.HashMap;
import java.util.Map;

import static org.apache.fineract.portfolio.self.account.api.SelfBeneficiariesTPTApiConstants.NAME_PARAM_NAME;
import static org.apache.fineract.portfolio.self.account.api.SelfBeneficiariesTPTApiConstants.TRANSFER_LIMIT_PARAM_NAME;

@Entity
@Table(name = "m_selfservice_beneficiariesrb_tpt", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "app_user_id", "is_active" }, name = "name") })
public class SelfBeneficiariesRBTPT extends AbstractPersistableCustom {

    @Column(name = "app_user_id", nullable = false)
    private Long appUserId;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "account_name", length = 50, nullable = false)
    private String accountName;

    @Column(name = "institution_name", length = 50, nullable = false)
    private String institutionName;

    @Column(name = "institution_code", length = 3, nullable = true)
    private String institutionCode;

    @Column(name = "currency_code", length = 3, nullable = true)
    private String currencyCode;

    @Column(name = "account_number", length = 50, nullable = false)
    private String accountNumber;

    //@Column(name = "office_id", nullable = true)
    //private Long officeId;

    //@Column(name = "client_id", nullable = true)
    //private Long clientId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "account_type", nullable = false)
    private Integer accountType;

    @Column(name = "transfer_limit", nullable = true)
    private Long transferLimit;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected SelfBeneficiariesRBTPT() {
        //
    }

    public SelfBeneficiariesRBTPT(Long appUserId,
                                  String name,
                                  String accountName,
                                  String accountNumber,
                                  Long accountId,
                                  Integer accountType,
                                  String institutionName,
                                  Long transferLimit,
                                  String institutionCode,
                                  String currencyCode) {
        this.appUserId = appUserId;
        this.name = name;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.accountId = accountId;
        this.accountType = accountType;
        this.institutionName = institutionName;
        this.transferLimit = transferLimit;
        this.institutionCode = institutionCode;
        this.currencyCode = currencyCode;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTransferLimit() {
        return this.transferLimit;
    }

    public void setTransferLimit(Long transferLimit) {
        this.transferLimit = transferLimit;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Long getAppUserId() {
        return this.appUserId;
    }

    public String getInstitutionName() { return this.institutionName; }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    //public Long getOfficeId() {
    //    return this.officeId;
    //}

    //public Long getClientId() {
    //    return this.clientId;
    //}

    public String getAccountNumber() {
        return this.accountNumber;
    }

    public String getAccountName() {
        return this.accountName;
    }

    public Long getAccountId() {
        return this.accountId;
    }

    public Integer getAccountType() {
        return this.accountType;
    }

    public Map<String, Object> update(String newName, Long newTransferLimit) {
        Map<String, Object> changes = new HashMap<>();
        if (!this.name.equals(newName)) {
            this.name = newName;
            changes.put(NAME_PARAM_NAME, newName);
        }
        if ((this.transferLimit != null && !this.transferLimit.equals(newTransferLimit))
                || (this.transferLimit == null && newTransferLimit != null)) {
            this.transferLimit = newTransferLimit;
            changes.put(TRANSFER_LIMIT_PARAM_NAME, newTransferLimit);
        }
        return changes;
    }

}
