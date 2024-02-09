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

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

import java.util.Collection;

public class SelfBeneficiariesRBTPTData {

    @SuppressWarnings("unused")
    private final Long id;
    @SuppressWarnings("unused")
    private final String name;
    @SuppressWarnings("unused")
    private final String accountName;
    @SuppressWarnings("unused")
    private final EnumOptionData accountType;
    @SuppressWarnings("unused")
    private final String accountNumber;
    @SuppressWarnings("unused")
    private final Long accountId;
    @SuppressWarnings("unused")
    private final Long transferLimit;

    @SuppressWarnings("unused")
    private final String institutionName;
    @SuppressWarnings("unused")
    private final Collection<EnumOptionData> accountTypeOptions;

    public SelfBeneficiariesRBTPTData(final Collection<EnumOptionData> accountTypeOptions) {
        this.accountTypeOptions = accountTypeOptions;
        this.id = null;
        this.name = null;
        this.institutionName = null;
        this.accountId = null;
        this.accountName = null;
        this.accountType = null;
        this.accountNumber = null;
        this.transferLimit = null;
    }

    public SelfBeneficiariesRBTPTData(final Long id,
                                      final String name,
                                      final String accountName,
                                      final EnumOptionData accountType,
                                      final String accountNumber,
                                      final Long accountId,
                                      final Long transferLimit,
                                      final String institutionName) {
        this.accountTypeOptions = null;
        this.id = id;
        this.name = name;
        this.institutionName = institutionName;
        this.accountName = accountName;
        this.accountType = accountType;
        this.accountNumber = accountNumber;
        this.transferLimit = transferLimit;
        this.accountId = accountId;
    }
}
