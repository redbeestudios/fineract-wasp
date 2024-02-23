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

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.service.AccountTransferEnumerations;
import org.apache.fineract.portfolio.self.accountrb.data.SelfAccountRBTemplateData;
import org.apache.fineract.portfolio.self.accountrb.data.SelfBeneficiariesRBTPTData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class SelfBeneficiariesRBTPTReadPlatformServiceImpl implements SelfBeneficiariesRBTPTReadPlatformService {

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final BeneficiaryMapper mapper;
    private final AccountTemplateMapper accountTemplateMapper;

    public SelfBeneficiariesRBTPTReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = new BeneficiaryMapper();
        this.accountTemplateMapper = new AccountTemplateMapper();
    }

    @Override
    public Collection<SelfBeneficiariesRBTPTData> retrieveAll() {
        AppUser user = this.context.authenticatedUser();
        return this.jdbcTemplate.query(this.mapper.schema(), this.mapper, new Object[] { user.getId() });
    }

    @Override
    public Collection<SelfAccountRBTemplateData> retrieveTPTSelfAccountTemplateData(AppUser user) {
        return this.jdbcTemplate.query(this.accountTemplateMapper.schema(), this.accountTemplateMapper,
                new Object[] { user.getId() });
    }

    private static final class BeneficiaryMapper implements RowMapper<SelfBeneficiariesRBTPTData> {

        private final String schemaSql;

        BeneficiaryMapper() {
            final StringBuilder sqlBuilder = new StringBuilder("select b.id as id, ");
            sqlBuilder.append(" b.name as name, ");
            sqlBuilder.append(" b.account_name as accountName, ");
            sqlBuilder.append(" b.account_type as accountType, ");
            sqlBuilder.append(" b.account_id as accountId, ");
            sqlBuilder.append(" b.account_number as accountNumber, ");
            sqlBuilder.append(" b.institution_name as institutionName, ");
            sqlBuilder.append(" b.transfer_limit as transferLimit, ");
            sqlBuilder.append(" b.institution_code as institutionCode, ");
            sqlBuilder.append(" b.currency_code as currencyCode ");
            sqlBuilder.append(" from m_selfservice_beneficiariesrb_tpt as b ");
            sqlBuilder.append(" where b.is_active = true ");
            sqlBuilder.append(" and b.account_type = 1 ");
            sqlBuilder.append(" and b.app_user_id = ?");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SelfBeneficiariesRBTPTData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Long accountId = rs.getLong("accountId");
            final String accountNumber = rs.getString("accountNumber");
            final String name = rs.getString("name");
            final String accountName = rs.getString("accountName");
            final String institutionName = rs.getString("institutionName");
            final Integer accountTypeId = rs.getInt("accountType");
            final EnumOptionData accountType = AccountTransferEnumerations.accountType(PortfolioAccountType.fromInt(accountTypeId));
            final Long transferLimit = rs.getLong("transferLimit");
            final String institutionCode = rs.getString("institutionCode");
            final String currencyCode = rs.getString("currencyCode");

            return new SelfBeneficiariesRBTPTData(id,
                    name,
                    accountName,
                    accountType,
                    accountNumber,
                    accountId,
                    transferLimit,
                    institutionName,
                    institutionCode,
                    currencyCode);
        }
    }

    private static final class AccountTemplateMapper implements RowMapper<SelfAccountRBTemplateData> {

        private final String schemaSql;

        AccountTemplateMapper() {
            final StringBuilder sqlBuilder = new StringBuilder("b.account_type as accountType, ");
            sqlBuilder.append(" from m_selfservice_beneficiariesrb_tpt as b ");
            sqlBuilder.append(" where b.is_active = true ");
            sqlBuilder.append(" and b.account_type = 2 ");
            sqlBuilder.append(" and b.app_user_id = ?");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SelfAccountRBTemplateData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final String officeName = "dummy"; // rs.getString("officeName");
            final Long officeId = 0l; //rs.getLong("officeId");
            final String clientName = "dummy"; //rs.getString("clientName");
            final Long clientId = 0l; //rs.getLong("clientId");
            final Integer accountTypeId = rs.getInt("accountType");
            final String accountNumber = "0123"; // rs.getString("accountNumber");
            final Long accountId = 0l; //rs.getLong("accountId");

            return new SelfAccountRBTemplateData(accountId, accountNumber, accountTypeId, clientId, clientName, officeId, officeName);
        }
    }

    @Override
    public Long getTransferLimit(Long appUserId, Long accountId, Integer accountType) {
        final StringBuilder sqlBuilder = new StringBuilder("select b.transfer_limit ");
        sqlBuilder.append(" from m_selfservice_beneficiariesrb_tpt as b ");
        sqlBuilder.append(" where b.app_user_id = ? ");
        sqlBuilder.append(" and b.account_id = ? ");
        sqlBuilder.append(" and b.account_type = ? ");
        sqlBuilder.append(" and b.is_active = true; ");

        return this.jdbcTemplate.queryForObject(sqlBuilder.toString(), Long.class, appUserId, accountId, accountType);
    }
}
