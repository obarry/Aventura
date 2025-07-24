package com.aventura.tools.tracing;

import java.io.*;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2025 Olivier BARRY
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------ 
 * 
 * New Tracer implementation.
 * Static methods
 * Allow to test activation at tracing stage
 * Less heavy (no additional methods in each class)
 * 
 * @author Olivier BARRY
 * @since November 2016
 * 
 */

public class Tracer {
    
    //static boolean isActivated = true;
    public static boolean isdate = false;
    
    public static boolean error     = true;
    public static boolean exception = true;
    public static boolean warning   = true;
    public static boolean info      = false;
    public static boolean function  = false;
    public static boolean state     = true;
	public static boolean object    = false;
	public static boolean stats     = true;
	public static boolean debug     = false;
    
    // For output to file
    public static boolean isfile = false;
    public static String file = null;
    private static FileOutputStream fout = null;
    
    private static String ERROR     = "ERROR";
    private static String EXCEPTION = "EXCEPTION";
    private static String WARNING   = "WARNING";
    private static String INFO      = "INFO";
    private static String FUNCTION  = "FUNCTION";
    private static String STATE     = "STATE";
    private static String OBJECT    = "OBJECT";
    private static String STATS     = "STATS";
    private static String DEBUG     = "DEBUG";
    
    /*
    public static void activateTracingLevels() {
        
        // Get config manager reference for config parameters
        ConfigManager cfg = ConfigManager.getConfigManager();
        
        isdate               = cfg.getIsTracingDate();
        
        info      = cfg.getIsTracingInfo();
        warning   = cfg.getIsTracingWarning();
        error     = cfg.getIsTracingError();
        exception = cfg.getIsTracingException();
        function  = cfg.getIsTracingFunction();
        state     = cfg.getIsTracingState();
        object    = cfg.getIsTracingObject();
        stats     = cfg.getIsTracingStats();
        debug     = cfg.getIsTracingDebug();
        
        isfile = cfg.getIsTracingToFile();
        if (isfile) {
            file = cfg.getTracingFile();
            // Open stream in append mode
            try {
                fout = new FileOutputStream(file,true);
                trace("Tracing to file: "+file);
            } catch (Exception e) {
                //traceException(Class.forName("Tracer"), e);
                try {
                    traceError(Class.forName("Tracer"),"Error trying to open tracing file. Going to trace to output instead.");
                } catch (Exception ex) {
                    trace("Error trying to open tracing file. Going to trace to output instead.");
                }
                // No longer use a file
                isfile = false;
            }
        }
    }
    */
    
    //
    // For test methods (e.g. main() methods in classes)
    // Will activate all levels of tracing
    //
    public static void activateAllLevels() {
                
        info      = true;
        warning   = true;
        error     = true;
        exception = true;
        function  = true;
        state     = true;
        object    = true;
        stats     = true;
        debug     = true;
        
    }

 
    public static void traceError(Class cls, String str) {
        trace(ERROR,cls,str);
    }
    
    public static void traceException(Class cls, String str) {
        trace(EXCEPTION,cls,str);
    }
    
    public static void traceWarning(Class cls, String str) {
        trace(WARNING,cls,str);
    }
    
    public static void traceInfo(Class cls, String str) {
        trace(INFO,cls,str);
    }
    
    public static void traceFunction(Class cls, String str) {
        trace(FUNCTION,cls,str);
    }
    
    public static void traceState(Class cls, String str) {
        trace(STATE,cls,str);
    }
    
	public static void traceObject(Class cls, String str) {
		trace(OBJECT,cls,str);
	}
    
	public static void traceStats(Class cls, String str) {
		trace(STATS,cls,str);
	}
	
	public static void traceDebug(Class cls, String str) {
		trace(DEBUG,cls,str);
	}
    
    private static void trace(String level, Class cls, String str) {
        if (isdate) {
            output("** " + date()+" * "+cls.toString()+" * "+level+" * "+str + " **");
        } else {
            output("** " + cls.getName() + " * " + level + " * " + str + " **");
        }      
    }
    public static void trace(String str) {
        //Date date = new Date();
        if (isdate) {
            System.out.println("** " + date()+" * "+str + " **");
        } else {
            System.out.println("** " + str + " **");
        }      
    }
    
    private static void output(String out) {
        if (!isfile) {
            System.out.println(out);
        } else {
            // Print to file
            try {
                // \n is needed for notepad, DOS text files.
                fout.write((out+"\r\n").getBytes());
            } catch (Exception e) {
                System.out.println(out);
            }
        }
    }
    
    private static String date() {
        return Long.toString(System.currentTimeMillis());
    }
    
}