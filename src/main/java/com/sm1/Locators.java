package com.sm1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;

/**
 * Created by pat on 8/24/17.
 */
public class Locators {

    private static String JS_GENERATE_LOCATORS;
    private static String JS_TRY_LOCATORS;





    public static void updateLocator(WebElement element, String OriginalLocator, WebDriver driver){


        try {

            if (initJS()) {

                JavascriptExecutor js = (JavascriptExecutor) driver;
                String retVal = (String) js.executeScript(JS_GENERATE_LOCATORS, element);
                System.out.println("retVal: " + retVal);

                DB db = DBMaker.fileDB("BetterWait_Locators_" + getFileSafeString(OriginalLocator) + getFileSafeString(driver.getCurrentUrl()) + ".db").make();
                ConcurrentMap<String, String> map = db.hashMap(OriginalLocator, Serializer.STRING, Serializer.STRING).createOrOpen();
                map.put(OriginalLocator, retVal);
                db.close();

                System.out.println("updated locators for: " + OriginalLocator);

            }

        }
        catch (Exception e){
            System.out.println("BetterWait Error: ");
            e.printStackTrace();
        }


    }

    public static String getFileSafeString(String toConvert){

        String fileSafe = toConvert.replace(" ","_").replace(".","_dot_").replace(":","_colon_").replace("-","_hyphen_").replace("/","_slash_");

        System.out.println("file safe string: " + fileSafe);

        return fileSafe;

    }

    public static boolean initJS(){
        boolean success = false;

        try{
            JS_GENERATE_LOCATORS = IOUtils.toString(Locators.class.getClass().getResourceAsStream("/locators.js"), "UTF-8");
            JS_TRY_LOCATORS = IOUtils.toString(Locators.class.getClass().getResourceAsStream("/TryLocators.js"), "UTF-8");
            success = true;
        }
        catch (Exception e){

            e.printStackTrace();
        }

        return success;

    }



    public static WebElement tryAltLocators(String locators, WebDriver driver) {

        WebElement element = null;



        try {

            if (initJS()) {

                JavascriptExecutor js = (JavascriptExecutor) driver;


                System.out.println("JS_TRY_LOCATORS: ");
                System.out.println(JS_TRY_LOCATORS);



                element = (WebElement) js.executeScript(JS_TRY_LOCATORS, locators);

                System.out.println("JS_TRY_LOCATORS retval: ");
                System.out.println(element);

            }

        }
        catch (Exception e){
            System.out.println("BetterWait Error: ");
            e.printStackTrace();
        }

        return element;


    }






}
