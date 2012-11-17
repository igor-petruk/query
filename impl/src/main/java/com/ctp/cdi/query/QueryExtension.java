package com.ctp.cdi.query;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.persistence.EntityManager;

import org.apache.deltaspike.core.util.bean.BeanBuilder;
import org.jboss.solder.logging.Logger;

import com.ctp.cdi.query.handler.QueryHandler;
import com.ctp.cdi.query.meta.DaoComponentsFactory;
import com.ctp.cdi.query.meta.unit.PersistenceUnits;

/**
 * The main extension class for CDI queries, based on Seam Solder service handlers.
 * Overrides the behavior for looking up handler classes.
 *
 * @author thomashug
 */
public class QueryExtension implements Extension {

    private final Logger log = Logger.getLogger(QueryExtension.class);
    
    protected final Set<Bean<?>> beans = new HashSet<Bean<?>>();
    
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery before) {
        PersistenceUnits.instance().init();
    }
    
    <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> event, BeanManager beanManager) {
        final Class<?> handlerClass = getHandlerClass(event);

        if (handlerClass != null) {
            buildBean(event.getAnnotatedType(), beanManager);
        }
    }
    
    void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        for (Bean<?> bean : beans) {
            event.addBean(bean);
        }
        beans.clear();
    }

    <X> Class<?> getHandlerClass(ProcessAnnotatedType<X> event) {
        if (event.getAnnotatedType().isAnnotationPresent(Dao.class) || event.getAnnotatedType().getJavaClass().isAnnotationPresent(Dao.class)) {
            log.debugv("getHandlerClass: Dao annotation detected on {0}", event.getAnnotatedType());
            boolean added = DaoComponentsFactory.instance().add(event.getAnnotatedType().getJavaClass());
            if (!added) {
                log.infov("getHandlerClass: Type {0} ignored as it's not related to an entity",
                        event.getAnnotatedType());
            }
            return added ? QueryHandler.class : null;
        }
        return null;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    <X> void buildBean(AnnotatedType<X> annotatedType, BeanManager beanManager) {
        try {
            final BeanBuilder<X> builder = new BeanBuilder<X>(beanManager);
            builder.readFromType(annotatedType);
            builder.types(extracted(annotatedType));
            builder.beanLifecycle(new QueryHandlerBeanLifecycle(
                    annotatedType.getJavaClass(), beanManager));
            beans.add(builder.create());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    private <X> Set<Type> extracted(AnnotatedType<X> annotatedType) {
        Set<Type> result = new HashSet<Type>();
        for (Type type : annotatedType.getTypeClosure()) {
            if (!type.equals(EntityManager.class)) {
                result.add(type);
            }
        }
        return result;
    }

}
