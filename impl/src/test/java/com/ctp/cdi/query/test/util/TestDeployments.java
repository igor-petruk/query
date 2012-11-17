package com.ctp.cdi.query.test.util;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.beans10.BeansDescriptor;
import org.jboss.shrinkwrap.impl.base.filter.ExcludeRegExpPaths;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import com.ctp.cdi.query.AbstractEntityDao;
import com.ctp.cdi.query.Dao;
import com.ctp.cdi.query.EntityDao;
import com.ctp.cdi.query.FirstResult;
import com.ctp.cdi.query.MaxResults;
import com.ctp.cdi.query.Modifying;
import com.ctp.cdi.query.NonEntity;
import com.ctp.cdi.query.Query;
import com.ctp.cdi.query.QueryExtension;
import com.ctp.cdi.query.QueryHandlerBeanLifecycle;
import com.ctp.cdi.query.QueryHandling;
import com.ctp.cdi.query.QueryHandlingLiteral;
import com.ctp.cdi.query.QueryParam;
import com.ctp.cdi.query.QueryResult;
import com.ctp.cdi.query.WithEntityManager;
import com.ctp.cdi.query.audit.AuditEntityListener;
import com.ctp.cdi.query.builder.QueryBuilder;
import com.ctp.cdi.query.criteria.Criteria;
import com.ctp.cdi.query.criteria.CriteriaSupport;
import com.ctp.cdi.query.criteria.QueryCriteria;
import com.ctp.cdi.query.criteria.QueryDslSupport;
import com.ctp.cdi.query.criteria.QuerySelection;
import com.ctp.cdi.query.handler.QueryHandler;
import com.ctp.cdi.query.home.DefaultNavigationProvider;
import com.ctp.cdi.query.home.EntityHome;
import com.ctp.cdi.query.meta.DaoComponents;
import com.ctp.cdi.query.param.Parameters;
import com.ctp.cdi.query.spi.DelegateQueryHandler;
import com.ctp.cdi.query.spi.QueryInvocationContext;
import com.ctp.cdi.query.test.TransactionalTestCase;
import com.ctp.cdi.query.test.domain.AuditedEntity;
import com.ctp.cdi.query.util.EntityUtils;

public abstract class TestDeployments {

    public static Filter<ArchivePath> TEST_FILTER = new ExcludeRegExpPaths(".*Test.*class");
 
    public static MavenDependencyResolver resolver() {
        return DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
    }
    
    public static WebArchive initDeployment() {
        return initDeployment(".*test.*");
    }

    /**
     * Create a basic deployment containing API classes, the Extension class and
     * test persistence / beans descriptor.
     * 
     * @return Basic web archive.
     */
    public static WebArchive initDeployment(String testFilter) {
        Logging.reconfigure();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibrary(createApiArchive())
                .addClasses(QueryExtension.class, QueryHandlerBeanLifecycle.class, QueryHandling.class, QueryHandlingLiteral.class)
                .addClasses(TransactionalTestCase.class)
                .addPackages(true, TEST_FILTER, createImplPackages())
                .addPackages(true, AuditedEntity.class.getPackage())
                .addPackages(true, new ExcludeRegExpPaths(testFilter), TransactionalTestCase.class.getPackage())
                .addAsResource("test-settings.properties", ArchivePaths.create("test-settings.properties"))
                .addAsWebInfResource(classpathResource("test-persistence.xml", "META-INF/persistence.xml"), ArchivePaths.create("classes/META-INF/persistence.xml"))
                .addAsWebInfResource("META-INF/services/javax.enterprise.inject.spi.Extension",
                        ArchivePaths.create("classes/META-INF/services/javax.enterprise.inject.spi.Extension"))
                .addAsWebInfResource(
                        new StringAsset(Descriptors.importAs(BeansDescriptor.class)
                            .fromFile("src/main/resources/META-INF/beans.xml")
                            .exportAsString()), 
                        ArchivePaths.create("beans.xml"));
        return addDependencies(archive);
    }

    public static Package[] createImplPackages() {
        return Arrays.asList(
                AuditEntityListener.class.getPackage(),
                QueryBuilder.class.getPackage(),
                QueryCriteria.class.getPackage(),
                QueryHandler.class.getPackage(),
                DaoComponents.class.getPackage(),
                Parameters.class.getPackage(),
                EntityUtils.class.getPackage(),
                DefaultNavigationProvider.class.getPackage()
            ).toArray(new Package[8]);
    }

    public static Archive<?> createApiArchive() {
        return ShrinkWrap.create(JavaArchive.class, "archive.jar")
                .addClasses(AbstractEntityDao.class, Dao.class, EntityDao.class,
                        FirstResult.class, MaxResults.class, Modifying.class,
                        NonEntity.class, Query.class, QueryParam.class, QueryResult.class, WithEntityManager.class)
                .addClasses(Criteria.class, QuerySelection.class, CriteriaSupport.class,
                        QueryDslSupport.class)
                .addClasses(DelegateQueryHandler.class, QueryInvocationContext.class)
                .addPackage(EntityHome.class.getPackage());
    }
    
    public static WebArchive addDependencies(WebArchive archive) {
        archive.addAsLibraries(resolver()
                .artifact("com.mysema.querydsl:querydsl-jpa")
                .artifact("org.apache.commons:commons-proxy")
                .artifact("org.apache.deltaspike.core:deltaspike-core-api")
                .artifact("org.apache.deltaspike.core:deltaspike-core-impl")
                .resolveAsFiles());
        if (includeLibs()) {
            archive.addAsLibraries(resolver()
                    .artifact("org.jboss.solder:solder-api")
                    .artifact("org.jboss.solder:solder-impl").resolveAsFiles());
        }
        return archive;
    }
    
    public static boolean config(String property) {
        ResourceBundle bundle = ResourceBundle.getBundle("test-settings");
        return Boolean.valueOf(bundle.getString(property));
    }
    
    public static String classpathResource(String resource, String fallback) {
        URL url = TestDeployments.class.getClassLoader().getResource(resource);
        return url != null ? resource : fallback;
    }
    
    private static boolean includeLibs() {
        return config("arquillian.deploy.libs");
    }

}
