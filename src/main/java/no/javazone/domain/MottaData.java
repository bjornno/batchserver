package no.javazone.domain;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;


public class MottaData implements IdempotentRepository {

    private JdbcTemplate jdbcTemplate;
    private int batchSize = 1000;

    public MottaData(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void receiveNewFile(@Header("CamelFileName") String filename, @Body InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try{
            String record = reader.readLine();
            if (!record.startsWith("STARTRECORD")) {
                throw new RuntimeException("Wrong format on file: " + filename);
            }
            List<String> sqls = new ArrayList<String>();
            boolean reachedSlutt = false;
            int i = 0;
            while ((record=reader.readLine()) != null) {
                if (record.startsWith("SLUTTRECORD")) {
                    reachedSlutt = true;
                    break;
                }
                sqls.add("insert into appdata (key, record) values ('"+ UUID.randomUUID().toString()+"','"+record+"')");
                if (++i % batchSize == 0)  {
                    persist(sqls);
                    sqls = new ArrayList<String>();
                }
            }
            if (! reachedSlutt) {
                throw new RuntimeException("No sluttrecord!");
            }
            persist(sqls);
            reader.close();
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException("error reading file");
        }
    }

    private void persist(List<String> sqls) {
        if (sqls != null && sqls.size() > 0) {
            String sqlStrings[] = new String[sqls.size()];
            sqls.toArray (sqlStrings);
            jdbcTemplate.batchUpdate(sqlStrings);
        }
    }

    public boolean add(Object o) {
        if (contains(o)) {
            Logger.getLogger(MottaData.class.getName()).warning("Received duplicate file, not processed");
            return false;
        } else {
            jdbcTemplate.execute("insert into idempotent values ('"+ o +"')");
            return true;
        }
    }

    public boolean contains(Object o) {
        Integer i = jdbcTemplate.queryForObject("select count(*) from idempotent where checksum = '"+ o +"'", Integer.class);
        return (i.intValue() > 0);
    }

    public boolean remove(Object o) {
        // no need, rollback will handle;
        return true;
    }

    public boolean confirm(Object o) {
        return true;
    }
}
