/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.catalog.glue;

import java.util.Objects;

import org.killbill.billing.catalog.DefaultCatalogService;
import org.killbill.billing.catalog.api.CatalogInternalApi;
import org.killbill.billing.catalog.api.CatalogService;
import org.killbill.billing.catalog.api.CatalogUserApi;
import org.killbill.billing.catalog.api.DefaultCatalogInternalApi;
import org.killbill.billing.catalog.api.user.DefaultCatalogUserApi;
import org.killbill.billing.catalog.caching.CatalogCache;
import org.killbill.billing.catalog.caching.CatalogCacheInvalidationCallback;
import org.killbill.billing.catalog.caching.DefaultCatalogCache;
import org.killbill.billing.catalog.caching.DefaultOverriddenPlanCache;
import org.killbill.billing.catalog.caching.OverriddenPlanCache;
import org.killbill.billing.catalog.caching.PriceOverridePattern;
import org.killbill.billing.catalog.dao.CatalogOverrideDao;
import org.killbill.billing.catalog.dao.DefaultCatalogOverrideDao;
import org.killbill.billing.catalog.io.CatalogLoader;
import org.killbill.billing.catalog.io.VersionedCatalogLoader;
import org.killbill.billing.catalog.override.DefaultPriceOverrideSvc;
import org.killbill.billing.catalog.override.PriceOverrideSvc;
import org.killbill.billing.catalog.plugin.VersionedCatalogMapper;
import org.killbill.billing.catalog.plugin.api.CatalogPluginApi;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.tenant.api.TenantInternalApi.CacheInvalidationCallback;
import org.killbill.billing.util.config.definition.CatalogConfig;
import org.killbill.billing.util.glue.KillBillModule;
import org.skife.config.AugmentedConfigurationObjectFactory;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class CatalogModule extends KillBillModule {

    public static final String CATALOG_INVALIDATION_CALLBACK = "CatalogInvalidationCallback";

    private final PriceOverridePattern priceOverridePattern;

    public CatalogModule(final KillbillConfigSource configSource) {
        super(configSource);
        // Unless explicitly specified we keep current "-" delimiter for backward compatibility
        final boolean useRECXMLNamesCompliant = Boolean.valueOf(Objects.requireNonNullElse(configSource.getString("org.killbill.catalog.price.override.delimiter.REC-xml-names-19990114.compliant"), "false"));
        this.priceOverridePattern = new PriceOverridePattern(useRECXMLNamesCompliant);
    }

    protected void installConfig() {
        final CatalogConfig config = new AugmentedConfigurationObjectFactory(skifeConfigSource).build(CatalogConfig.class);
        bind(CatalogConfig.class).toInstance(config);
    }

    protected void installCatalog() {
        bind(PriceOverridePattern.class).toInstance(priceOverridePattern);
        bind(CatalogService.class).to(DefaultCatalogService.class).asEagerSingleton();
        bind(CatalogLoader.class).to(VersionedCatalogLoader.class).asEagerSingleton();
        bind(PriceOverrideSvc.class).to(DefaultPriceOverrideSvc.class).asEagerSingleton();
    }

    protected void installCatalogDao() {
        bind(CatalogOverrideDao.class).to(DefaultCatalogOverrideDao.class).asEagerSingleton();
    }

    protected void installCatalogUserApi() {
        bind(CatalogUserApi.class).to(DefaultCatalogUserApi.class).asEagerSingleton();
    }

    protected void installCatalogInternalApi() {
        bind(CatalogInternalApi.class).to(DefaultCatalogInternalApi.class).asEagerSingleton();
    }

    public void installCatalogConfigCache() {
        bind(CatalogCache.class).to(DefaultCatalogCache.class).asEagerSingleton();
        bind(CacheInvalidationCallback.class).annotatedWith(Names.named(CATALOG_INVALIDATION_CALLBACK)).to(CatalogCacheInvalidationCallback.class).asEagerSingleton();

        bind(OverriddenPlanCache.class).to(DefaultOverriddenPlanCache.class).asEagerSingleton();
    }

    protected void installCatalogPluginApi() {
        bind(new TypeLiteral<OSGIServiceRegistration<CatalogPluginApi>>() {}).toProvider(DefaultCatalogProviderPluginRegistryProvider.class).asEagerSingleton();
        bind(VersionedCatalogMapper.class).asEagerSingleton();
    }


    @Override
    protected void configure() {
        installConfig();
        installCatalogDao();
        installCatalog();
        installCatalogUserApi();
        installCatalogInternalApi();
        installCatalogConfigCache();
        installCatalogPluginApi();
    }
}
