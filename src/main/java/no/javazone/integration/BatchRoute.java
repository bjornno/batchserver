package no.javazone.integration;

import org.apache.camel.spring.SpringRouteBuilder;


public class BatchRoute extends SpringRouteBuilder {

    @Override
    public void configure() throws Exception {

        from("file://data/from/?preMove=inprogress/&move=../done/&moveFailed=../error")
                .to("bean:mottaData");

    }

}
