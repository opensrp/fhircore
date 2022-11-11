package org.smartregister.fhircore.engine.rulesengine;

// Online Java Compiler
// Use this editor to write, compile and run your Java code online

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeHelper {

    public static String makeItReadable(String dateString) {
        SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = from.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new SimpleDateFormat("E, MMM dd yyyy").format(date);
    }
}
