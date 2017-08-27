package com.sm1;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.IOUtils;

/**
 * Created by pat on 8/24/17.
 */
public class Locators {

    private static String JS_GENERATE_LOCATORS;




    public static void updateLocator(WebElement element, String locator, WebDriver driver){

        //String[] parts = locator.split(": ");

        if (initJS()) {


            JavascriptExecutor js = (JavascriptExecutor) driver;
            String retVal = (String) js.executeScript(JS_GENERATE_LOCATORS, element);
            System.out.println("retVal: " + retVal);

            DB db = DBMaker.fileDB("SmartWait_Locators.db").make();
            ConcurrentMap<String, Long> map = db.hashMap(locator, Serializer.STRING, Serializer.LONG).createOrOpen();
            map.put("sample locator", 1234L);
            db.close();

            System.out.println("1 updated locators for: " + locator);

        }


    }

    public static boolean initJS(){
        boolean success = false;

        try{
            JS_GENERATE_LOCATORS = IOUtils.toString(Locators.class.getClass().getResourceAsStream("/locators.js"), "UTF-8");


            success = true;
        }
        catch (Exception e){

            e.printStackTrace();
        }

        return success;

    }





}
