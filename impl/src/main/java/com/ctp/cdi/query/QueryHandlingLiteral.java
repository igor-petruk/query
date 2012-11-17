package com.ctp.cdi.query;

import javax.enterprise.util.AnnotationLiteral;

public class QueryHandlingLiteral extends AnnotationLiteral<QueryHandling> implements QueryHandling {

    public static final QueryHandling INSTANCE = new QueryHandlingLiteral();

}
