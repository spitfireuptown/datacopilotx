package com.datacopilotx.ai.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {

    public static String getFullStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        Throwable cause = e.getCause();
        int depth = 1;
        while (cause != null) {
            sb.append("\nCaused by: ").append(cause.getClass().getName())
              .append(": ").append(cause.getMessage()).append("\n");
            
            StackTraceElement[] causeStackTrace = cause.getStackTrace();
            for (StackTraceElement element : causeStackTrace) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
            
            cause = cause.getCause();
            depth++;
            if (depth > 10) {
                sb.append("\n[... truncated at 10 levels ...]");
                break;
            }
        }
        
        return sb.toString();
    }

    public static String getStackTraceString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}