package no.javazone.domain;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;


public class PlukkUtData {

    private JdbcTemplate jdbcTemplate;

    @EndpointInject()
    ProducerTemplate producer;

    public PlukkUtData(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void plukkUt(Exchange exchange) {
        List<Map<String, Object>> records = jdbcTemplate.queryForList("select * from appdata where status = 0");
        HashMap<String, List> map = new HashMap<String, List>();
        List<String> sqlupdates = new ArrayList<String>();
        groupRecordsOnOutFiles(records, map, sqlupdates);
        batchUpdate(sqlupdates);
        writeFiles(map);
    }

    private void groupRecordsOnOutFiles(List<Map<String, Object>> records, HashMap<String, List> map, List<String> sqlupdates) {
        for (Map<String,Object> record : records) {
            String key = (String) record.get("KEY");
            String recordLine = (String) record.get("RECORD");
            int version = 0;
            Object versionObj = record.get("VERSION");
            // uffda (fix, oracle, hsql diffs)
            if (versionObj instanceof Integer) {
                version = (Integer) versionObj;
            } else {
                version = ((BigDecimal) versionObj).intValue();
            }
            int nextversion = version + 1;
            List li = map.get(recordLine.substring(0,4));
            sqlupdates.add("update appdata set status = 1, version = "+ nextversion +"  where status = 0 and key = '"+ key +"' and version = " + version);
            if (li != null) {
                li.add(recordLine);
            } else {
                ArrayList list = new ArrayList();
                list.add(recordLine);
                map.put(recordLine.substring(0,4), list);
            }
        }
    }

    private void writeFiles(HashMap<String, List> map) {
        Set<String> set = map.keySet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            List recs = map.get(key);
            String s = "";
            for (Iterator iterator = recs.iterator(); iterator.hasNext();) {
                String s1 = (String) iterator.next();
                s += s1 +"\n";
            }
            producer.sendBodyAndHeader("seda:writeFile", s, Exchange.FILE_NAME, s.substring(0,4));
        }
    }


    private void batchUpdate(List<String> sqls) {
        int[] results = null;
        if (sqls != null && sqls.size() > 0) {
            String sqlStrings[] = new String[sqls.size()];
            sqls.toArray (sqlStrings);
            results = jdbcTemplate.batchUpdate(sqlStrings);
        }
        int sum = 0;
        for (int i=0; i < results.length; i++) {
            sum+=results[i];
        }
        if (sum != sqls.size()) {
            throw new DoNotRetryException("Failed on optimistic locking");
        }
    }
    
}
