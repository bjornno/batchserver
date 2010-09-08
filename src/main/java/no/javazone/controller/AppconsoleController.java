package no.javazone.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
@Transactional
public class AppconsoleController {
    private JdbcTemplate jdbcTemplate;

    @Required
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @RequestMapping(value = "/properties", method = RequestMethod.GET)
    @ResponseBody
    public String getAllProperties() {
        String output = "";
        List<Map<String, Object>> allRecords = jdbcTemplate.queryForList("select key, value from application_properties");
        for (Map<String, Object> map : allRecords) {
            output += map.get("key") + " " + map.get("value") + "\n";
        }
        return output;
    }


    @RequestMapping(value = "/data", method = RequestMethod.GET)
    @ResponseBody
    public String getAllAppdata() {
        String output = "";
        int i = jdbcTemplate.queryForInt("select count(*) from appdata where status = 0");
        if (i > 100) {
            output = i + " rows with status ready, too many to show each line";
        } else if (i == 0) {
            output = "no data";
        } else {
            output += i + " rows with status ready.\n";
            List<String> allRecords = jdbcTemplate.queryForList("select record from appdata where status = 0", String.class);
            for (String record : allRecords) {
                output += record + "\n";
            }
        }
        return output;
    }

    @RequestMapping(value = "/events", method = RequestMethod.GET)
    @ResponseBody
    public String getAllEvents() {
        String output = "";
        int i = jdbcTemplate.queryForInt("select count(*) from events");
        if (i == 0) {
            output = "no events";
        }
        List<String> events = jdbcTemplate.queryForList("select message from events", String.class);
        for (String event : events) {
            output += "INFO: " + event + "\n";
        }
        return output;
    }


    @RequestMapping(value = "/errors", method = RequestMethod.GET)
    @ResponseBody
    public String getErrors() {
        return "database: \n" + getDbErrors() + "\n\n" +
                "filesystem: \n" +
        getFileErrors() + "\n" + getFileToLongInProgress();
    }

    public String getDbErrors() {
        List<String> errormessages = jdbcTemplate.queryForList("select message from errormessages", String.class);
        String result = "";
        for (String errormessage : errormessages) {
            result += "ERROR: "+errormessage +"\n";
        }
        return result;
    }

    public String getFileErrors() {
        String errors = "";
        File target = new File("data/from/error/");

        File[] files = target.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                errors += "ERROR: Failed file:"+file.getAbsolutePath() + " " + new Date(file.lastModified()).toString() + "\n";
            }
        }
        return errors;
    }

    public String getFileToLongInProgress() {
        String errors = "";
        File target = new File("data/from/inprogress/");

        File[] files = target.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.lastModified() < (System.currentTimeMillis() - 30000)) {
                    errors += "WARNING: In process status for more than 30 secs: "+file.getAbsolutePath() + " " + new Date(file.lastModified()).toString() + "\n" ;
                }
            }
        }
        return errors;
    }


}
