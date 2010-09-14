package no.javazone.integration;

import no.javazone.domain.DoNotRetryException;
import no.javazone.domain.MottaData;
import org.apache.camel.LoggingLevel;
import org.apache.camel.spring.SpringRouteBuilder;


public class BatchRoute extends SpringRouteBuilder {

    @Override
    public void configure() throws Exception {

        errorHandler(transactionErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        onException(Throwable.class)
                .wireTap("direct:dbeventlog",
                        simple("insert into errormessages (key,message) values ('${exchangeId}','${exception.message} ')"));

        onException(DoNotRetryException.class)
                .wireTap("direct:dbeventlog",
                        simple("insert into errormessages (key,message) values ('${exchangeId}','${exception.message} ')"))
                .maximumRedeliveries(0);

        from("direct:dbeventlog")
                .to("jdbc:dataSource");




        from("file://data/from/?preMove=inprogress/&move=../done/&moveFailed=../error")
                .transacted()
                .wireTap("direct:dbeventlog",
                        simple("insert into events (key,message) values ('${exchangeId}','received file ${header.CamelFileName} ')"))
                .idempotentConsumer(header("CamelFileName"),getApplicationContext().getBean("mottaData", MottaData.class))
                .to("bean:mottaData");


        from("jetty:http://localhost:9090/plukkUt")
                .inOnly("seda:plukkUt") ;

        from("quartz://myGroup/myTimerName?cron=0+0+12+*+*+?+*+MON-FRI?stateful=true")
                .inOnly("seda:plukkUt");

        from("seda:plukkUt")
                .transacted()
                .wireTap("direct:dbeventlog",
                        simple("insert into events (key,message) values ('${exchangeId}','plukker ut data')"))
                .to("bean:plukkUtData");

        from("seda:writeFile")
                .wireTap("direct:dbeventlog",
                        simple("insert into events (key,message) values ('${exchangeId}','writing file ${header.CamelFileName} ')"))
                .to("file:data/to?tempPrefix=inprogress/");
    }

}
