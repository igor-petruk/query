package com.ctp.cdi.query;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.commons.proxy.factory.javassist.JavassistProxyFactory;
import org.apache.deltaspike.core.util.metadata.builder.ContextualLifecycle;
import org.jboss.solder.reflection.annotated.AnnotatedTypeBuilder;

import com.ctp.cdi.query.handler.QueryHandler;

public class QueryHandlerBeanLifecycle<X> implements ContextualLifecycle<X> {

    private Class javaClass;
    private BeanManager beanManager;

    public QueryHandlerBeanLifecycle(Class<X> javaClass, BeanManager beanManager) {
        this.javaClass = javaClass;
        this.beanManager = beanManager;
    }

    @Override
    public X create(Bean<X> bean, CreationalContext<X> creationalContext) {
        AnnotatedTypeBuilder<X> typeBuilder = new AnnotatedTypeBuilder<X>()
                .readFromType((Class) QueryHandler.class);
        InjectionTarget<X> injectionTarget = beanManager.createInjectionTarget(typeBuilder.create());
        X handler = injectionTarget.produce(creationalContext);
        injectionTarget.inject(handler, creationalContext);
        injectionTarget.postConstruct(handler);
        QueryHandler qHandler = (QueryHandler) handler;
        qHandler.setOriginalTarget(javaClass);
        Object result = new JavassistProxyFactory()
                .createInvokerProxy(qHandler, new Class<?>[] { javaClass });
        
        return (X) result;
    }

    @Override
    public void destroy(Bean<X> bean, X instance, CreationalContext<X> creationalContext) {
        
    }

}
