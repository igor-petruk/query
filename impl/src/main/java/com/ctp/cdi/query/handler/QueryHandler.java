package com.ctp.cdi.query.handler;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;

import org.apache.commons.proxy.Invoker;
import org.jboss.solder.logging.Logger;

import com.ctp.cdi.query.builder.QueryBuilder;
import com.ctp.cdi.query.builder.QueryBuilderFactory;
import com.ctp.cdi.query.meta.DaoComponent;
import com.ctp.cdi.query.meta.DaoComponents;
import com.ctp.cdi.query.meta.DaoMethod;
import com.ctp.cdi.query.meta.Initialized;

/**
 * Entry point for query processing.
 * 
 * @author thomashug
 */
public class QueryHandler implements Serializable, Invoker {

    private static final long serialVersionUID = 1L;

    private final Logger log = Logger.getLogger(getClass());
    
    @Inject @Any
    private Instance<EntityManager> entityManager;
    
    @Inject
    private QueryBuilderFactory queryBuilder;
    
    @Inject @Initialized
    private DaoComponents components;
    
    @Inject
    private Event<CdiQueryInvocationContext> contextCreated;
    
    private Class<?> originalTarget;
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
        InvocationContext context = new QueryInvocationContext(proxy, method, parameters);
        return handle(context);
    }
    
    @AroundInvoke
    public Object handle(InvocationContext context) {
        CdiQueryInvocationContext queryContext = null;
        try {
            Class<?> daoClass = originalTarget;
            DaoComponent dao = components.lookupComponent(daoClass);
            DaoMethod method = components.lookupMethod(daoClass, context.getMethod());
            queryContext = createContext(context, dao, method);
            QueryBuilder builder = queryBuilder.build(method);
            return builder.execute(queryContext);
        } catch (Exception e) {
            log.error("Query execution error", e);
            if (queryContext != null) {
                throw new QueryInvocationException(e, queryContext);
            }
            throw new QueryInvocationException(e, context);
        }
    }

    private CdiQueryInvocationContext createContext(InvocationContext context, DaoComponent dao, DaoMethod method) {
        CdiQueryInvocationContext queryContext = new CdiQueryInvocationContext(context, method, resolveEntityManager(dao));
        contextCreated.fire(queryContext);
        return queryContext;
    }
    
    private EntityManager resolveEntityManager(DaoComponent dao) {
        Annotation[] qualifiers = extractFromTarget(dao.getDaoClass());
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = dao.getEntityManagerQualifiers(); 
        }
        if (qualifiers == null || qualifiers.length == 0) {
            return entityManager.get();
        }
        return entityManager.select(qualifiers).get();
    }

    private Annotation[] extractFromTarget(Class<?> target) {
        try {
            Method method = originalTarget.getDeclaredMethod("getEntityManager");
            return method.getAnnotations();
        } catch (Exception e) {
            return null;
        }
    }

    public Class<?> getOriginalTarget() {
        return originalTarget;
    }

    public void setOriginalTarget(Class<?> originalTarget) {
        this.originalTarget = originalTarget;
    }

}
