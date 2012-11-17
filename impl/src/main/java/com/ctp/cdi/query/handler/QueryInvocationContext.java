package com.ctp.cdi.query.handler;

import java.lang.reflect.Method;
import java.util.Map;

import javax.interceptor.InvocationContext;

public class QueryInvocationContext implements InvocationContext {

    private static final Object[] NO_OBJECTS = new Object[0];

    private final Object target;
    private final Method method;
    private Object[] parameters;

    public QueryInvocationContext(Object target, Method method,  Object[] parameters) {
        this.target = target;
        this.method = method;
        this.parameters = parameters;
    }

    public Object getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(final Object[] parameters) {
        this.parameters = parameters == null ? NO_OBJECTS : parameters;
    }

    public Map<String, Object> getContextData() {
        return null;
    }

    public Object getTimer() {
        return null;
    }

    public Object proceed() throws Exception {
        return method.invoke(target, parameters);
    }
}
