package no.javazone.integration;

import org.apache.camel.Exchange;
import org.apache.camel.test.junit4.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.File;


public class BatchApplicationTest extends CamelSpringTestSupport {
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(new String[] {"META-INF/spring/applicationContext.xml", "testContext.xml"});
    }

    public void setUp() throws Exception {
        deleteDirectory("data");
        deleteDirectory("logs");
        super.setUp();
    }

    public void tearDown() throws Exception {
        deleteDirectory("data");
        deleteDirectory("logs");
        super.tearDown();
    }

    @Test
    public void shouldMoveOkfilesToDone() throws Exception {
        template.sendBodyAndHeader("file://data/from", getOkFile(), Exchange.FILE_NAME, "okfile.txt");
        Thread.sleep(1000);
        File target = new File("data/from/done/okfile.txt");
        assertTrue("file not moved to done", target.exists());
    }

    @Test
    public void shouldInsertRecordsToDatabase() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(applicationContext.getBean("dataSource", DataSource.class));
        assertEquals(0, jdbcTemplate.queryForInt("select count(*) from appdata"));
        template.sendBodyAndHeader("file://data/from", getOkFile(), Exchange.FILE_NAME, "okfile.txt");
        Thread.sleep(2000);
        assertEquals(2, jdbcTemplate.queryForInt("select count(*) from appdata"));
    }

    @Test
     public void shouldRollbackInsertsAndCreateErrormessage() throws Exception {
         JdbcTemplate jdbcTemplate = new JdbcTemplate(applicationContext.getBean("dataSource", DataSource.class));
         assertEquals(0, jdbcTemplate.queryForInt("select count(*) from appdata"));
         assertEquals(0, jdbcTemplate.queryForInt("select count(*) from errormessages"));
         template.sendBodyAndHeader("file://data/from", getWrongFormatFile(), Exchange.FILE_NAME, "badfile.txt");
         Thread.sleep(2000);
         assertEquals(0, jdbcTemplate.queryForInt("select count(*) from appdata"));
         assertEquals(1, jdbcTemplate.queryForInt("select count(*) from errormessages"));
     }                            {}
    
    @Test
    public void shouldCatchSecondFileOnIdempotens() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(applicationContext.getBean("dataSource", DataSource.class));
        template.sendBodyAndHeader("file://data/from", getOkFile(), Exchange.FILE_NAME, "okfile.txt");
        Thread.sleep(2000);
        assertEquals(2, jdbcTemplate.queryForInt("select count(*) from appdata"));
        template.sendBodyAndHeader("file://data/from", getOkFile(), Exchange.FILE_NAME, "okfile.txt");
        Thread.sleep(2000);
        assertEquals(2, jdbcTemplate.queryForInt("select count(*) from appdata"));

    }

    @Test
    public void shouldTriggerScheduledJob() throws Exception {
        fail("No test for this yet!");
    }

    @Test
    public void shouldSendFiles() throws Exception {
        template.sendBodyAndHeader("file://data/from", getOkFile(), Exchange.FILE_NAME, "okfile.txt");
        Thread.sleep(2000);
        template.sendBody("seda:plukkUt", null);
        Thread.sleep(2000);
        File target = new File("data/to");
        assertTrue("Not created any files", target.list().length==3);
    }

    @Test
    public void shouldSendEventsForAppconsole() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(applicationContext.getBean("dataSource", DataSource.class));
        template.sendBodyAndHeader("file://data/from", getOkFile(), Exchange.FILE_NAME, "okfile.txt");
        template.sendBodyAndHeader("file://data/from", getWrongFormatFile(), Exchange.FILE_NAME, "errorfile.txt");
        Thread.sleep(2000);
        template.sendBody("seda:plukkUt", null);
        Thread.sleep(2000);
        assertEquals(1, jdbcTemplate.queryForInt("select count(*) from errormessages"));
        assertEquals(5, jdbcTemplate.queryForInt("select count(*) from events"));
    }

    String getOkFile() {
        return "STARTRECORD\n" +
                "12341234567891000004000010987654321\n" +
                "12351234567891000004000010987654321\n" +
                "SLUTTRECORD";
    }

    String getWrongFormatFile() {
        return "Nnsnsn sdsds jhdkshkdh %&/&%/&�%&";
    }
}
