package no.javazone.domain;

import org.apache.camel.Body;
import org.apache.camel.Header;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MottaData {

    public void receiveNewFile(@Header("CamelFileName") String filename, @Body InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try{
            String record = reader.readLine();
            if (!record.startsWith("STARTRECORD")) {
                throw new RuntimeException("Wrong format on file: " + filename);
            }
            boolean reachedSlutt = false;
            int i = 0;
            while ((record=reader.readLine()) != null) {
                if (record.startsWith("SLUTTRECORD")) {
                    reachedSlutt = true;
                    break;
                }
            }
            if (! reachedSlutt) {
                throw new RuntimeException("No sluttrecord!");
            }
            reader.close();
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException("error reading file");
        }
    }
}
