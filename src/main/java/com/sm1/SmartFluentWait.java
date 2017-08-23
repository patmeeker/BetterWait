package com.sm1;

import org.openqa.selenium.*;


import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.support.ui.*;


import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.mapdb.*;


public class SmartFluentWait<T> implements Wait<T> {

    public static final Duration FIVE_HUNDRED_MILLIS = new Duration(500, MILLISECONDS);

    private final T input;
    private final Clock clock;
    private final Sleeper sleeper;

    private Duration timeout = FIVE_HUNDRED_MILLIS;
    private Duration interval = FIVE_HUNDRED_MILLIS;
    private Supplier<String> messageSupplier = () -> null;

    private List<Class<? extends Throwable>> ignoredExceptions = Lists.newLinkedList();

    /**
     * @param input The input value to pass to the evaluated conditions.
     */
    public SmartFluentWait(T input) {
        this(input, new SystemClock(), Sleeper.SYSTEM_SLEEPER);
    }

    /**
     * @param input The input value to pass to the evaluated conditions.
     * @param clock The clock to use when measuring the timeout.
     * @param sleeper Used to put the thread to sleep between evaluation loops.
     */
    public SmartFluentWait(T input, Clock clock, Sleeper sleeper) {
        this.input = checkNotNull(input);
        this.clock = checkNotNull(clock);
        this.sleeper = checkNotNull(sleeper);
    }

    /**
     * Sets how long to wait for the evaluated condition to be true. The default timeout is
     * {@link #FIVE_HUNDRED_MILLIS}.
     *
     * @param duration The timeout duration.
     * @param unit The unit of time.
     * @return A self reference.
     */
    public SmartFluentWait<T> withTimeout(long duration, TimeUnit unit) {
        this.timeout = new Duration(duration, unit);
        return this;
    }

    /**
     * Sets the message to be displayed when time expires.
     *
     * @param message to be appended to default.
     * @return A self reference.
     */
    public SmartFluentWait<T> withMessage(final String message) {
        this.messageSupplier = () -> message;
        return this;
    }

    /**
     * Sets the message to be evaluated and displayed when time expires.
     *
     * @param messageSupplier to be evaluated on failure and appended to default.
     * @return A self reference.
     */
    public SmartFluentWait<T> withMessage(Supplier<String> messageSupplier) {
        this.messageSupplier = messageSupplier;
        return this;
    }

    /**
     * Sets how often the condition should be evaluated.
     *
     * <p>
     * In reality, the interval may be greater as the cost of actually evaluating a condition function
     * is not factored in. The default polling interval is {@link #FIVE_HUNDRED_MILLIS}.
     *
     * @param duration The timeout duration.
     * @param unit The unit of time.
     * @return A self reference.
     */
    public SmartFluentWait<T> pollingEvery(long duration, TimeUnit unit) {
        this.interval = new Duration(duration, unit);
        return this;
    }

    /**
     * Configures this instance to ignore specific types of exceptions while waiting for a condition.
     * Any exceptions not whitelisted will be allowed to propagate, terminating the wait.
     *
     * @param types The types of exceptions to ignore.
     * @param <K> an Exception that extends Throwable
     * @return A self reference.
     */
    public <K extends Throwable> SmartFluentWait<T> ignoreAll(Collection<Class<? extends K>> types) {
        ignoredExceptions.addAll(types);
        return this;
    }

    /**
     * @see #ignoreAll(Collection)
     * @param exceptionType exception to ignore
     * @return a self reference
     */
    public SmartFluentWait<T> ignoring(Class<? extends Throwable> exceptionType) {
        return this.ignoreAll(ImmutableList.<Class<? extends Throwable>>of(exceptionType));
    }

    /**
     * @see #ignoreAll(Collection)
     * @param firstType exception to ignore
     * @param secondType another exception to ignore
     * @return a self reference
     */
    public SmartFluentWait<T> ignoring(Class<? extends Throwable> firstType,
                                       Class<? extends Throwable> secondType) {

        return this.ignoreAll(ImmutableList.of(firstType, secondType));
    }


    /**
     * Repeatedly applies this instance's input value to the given function until one of the following
     * occurs:
     * <ol>
     * <li>the function returns neither null nor false,</li>
     * <li>the function throws an unignored exception,</li>
     * <li>the timeout expires,
     * <li>
     * <li>the current thread is interrupted</li>
     * </ol>
     *
     * @param isTrue the parameter to pass to the {@link ExpectedCondition}
     * @param <V> The function's expected return type.
     * @return The functions' return value if the function returned something different
     *         from null or false before the timeout expired.
     * @throws TimeoutException If the timeout expires.
     */
    @Override
    public <V> V until(Function<? super T, V> isTrue) {

        return smartUntil(isTrue, true);
    }



    private <V> V smartUntil(Function<? super T, V> isTrue, boolean doMap) {

        long end = clock.laterBy(timeout.in(MILLISECONDS));
        Throwable lastException;
        while (true) {

            try {
                V value = isTrue.apply(input);
                if (value != null && (Boolean.class != value.getClass() || Boolean.TRUE.equals(value))) {

                    if(doMap){
                        updateLocator(isTrue.toString());
                    }

                    return value;
                }

                // Clear the last exception; if another retry or timeout exception would
                // be caused by a false or null value, the last exception is not the
                // cause of the timeout.
                lastException = null;
            } catch (Throwable e) {

                if(doMap){

                    System.out.println("Desired locator failed: " + isTrue.toString());
                    System.out.println("Original message: " + e.getMessage());

                    return tryAltLocator(isTrue);

                }
                else {
                    lastException = propagateIfNotIgnored(e);
                }
            }

            // Check the timeout after evaluating the function to ensure conditions
            // with a zero timeout can succeed.
            if (!clock.isNowBefore(end)) {
                String message = messageSupplier != null ?
                        messageSupplier.get() : null;

                String timeoutMessage = String.format(
                        "Expected condition failed: %s (tried for %d second(s) with %s interval)",
                        message == null ? "waiting for " + isTrue : message,
                        timeout.in(SECONDS), interval);
                throw timeoutException(timeoutMessage, lastException);
            }

            try {
                sleeper.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WebDriverException(e);
            }
        }
    }

    private Throwable propagateIfNotIgnored(Throwable e) {
        for (Class<? extends Throwable> ignoredException : ignoredExceptions) {
            if (ignoredException.isInstance(e)) {
                return e;
            }
        }
        //Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
    }

    /**
     * Throws a timeout exception. This method may be overridden to throw an exception that is
     * idiomatic for a particular test infrastructure, such as an AssertionError in JUnit4.
     *
     * @param message The timeout message.
     * @param lastException The last exception to be thrown and subsequently suppressed while waiting
     *        on a function.
     * @return Nothing will ever be returned; this return type is only specified as a convenience.
     */
    protected RuntimeException timeoutException(String message, Throwable lastException) {
        throw new TimeoutException(message, lastException);
    }

    public void updateLocator(String locator){

        DB db = DBMaker.fileDB("SmartWait_Locators.db").make();
        ConcurrentMap<String,Long> map = db.hashMap(locator, Serializer.STRING, Serializer.LONG).createOrOpen();
        map.put("sample locator", 1234L);
        db.close();

        System.out.println("updated locators for: " + locator);


    }

    public <V> V tryAltLocator(Function<? super T, V> isTrue) {

        System.out.println("Attempting to do SmartWait");

        System.out.println("Getting stored locators for " + isTrue.toString());


        // get the locators
        DB db = DBMaker.fileDB("SmartWait_Locators.db").make();

        ConcurrentMap<String, Long> map = db.hashMap(isTrue.toString(), Serializer.STRING, Serializer.LONG).createOrOpen();



        // order by recent

        // print
        for (Iterator<ConcurrentMap.Entry<String, Long>> it = map.entrySet().iterator(); it.hasNext(); ) {
            ConcurrentMap.Entry<String, Long> entry = it.next();

            System.out.println("key: " + entry.getKey());
            System.out.println("val: " + entry.getValue());


//            if(entry.getKey().equals("test")) {
//                it.remove();
//            }
        }

        db.close();

    return null;
    }









//public void until(ExpectedCondition<WebElement> isTrue) {
//            System.out.println("hey, it compiles!");
//
//                WebDriver driver = new ChromeDriver();
//
//            WebDriverWait wait = new WebDriverWait(driver, 10);
//
//
//
//
//
//            WebElement webElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("id")));
//
//    }



















    // full path using tag names only
    // id ids if you got'm
    // completely different identifiers
    // text contained
    // relation to siblings

//    public WebElement findElement(By by, WebDriverWait wait){
//
//
//
//
//        System.out.println("getting element...");
//
//        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
//
//
//        // getPath(element, driver);
//
//        return element;
//    }


//    public WebElement findElement(By by, WebDriver driver){
//
//
//
//        WebDriverWait wait = new WebDriverWait(driver, 10);
//
//        System.out.println("getting element...");
//
//        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
//
//
//        getPath(element, driver);
//
//        return element;
//    }

    private void getPath(WebElement element, WebDriver driver){

        //System.out.println(element.getTagName());
        //System.out.println(element.getText());

        //((JavascriptExecutor) driver).executeScript("alert('alert')");

        String strings = JS_GET_NTH_OF_TYPE_PATH + JS_GET_CONTAINS_TEXT_PATH + GENERATE_ALTERNATE_SELECTORS;
        //System.out.println(strings);



        String retVal = (String) ((JavascriptExecutor) driver).executeScript(JS_GET_NTH_OF_TYPE_PATH + JS_GET_CONTAINS_TEXT_PATH + GENERATE_ALTERNATE_SELECTORS, element);

        System.out.println(retVal);



        //System.out.println(element.findElement(By.xpath("..")).getTagName());



        //WebElement parent = element.findElement(By.xpath(".."));

        //System.out.println(parent);

    }

    public String JS_GET_NTH_OF_TYPE_PATH =

            "function getTagNameOnlyPath(element) {" +

                    "  try {" +

                    "    if (element.tagName == 'BODY'){" +
                    "      return 'BODY';" +
                    "    }" +

                    "    var path = getTagNameOnlyPath(element.parentNode);" +

                    "    return path + ' ' + element.tagName + getPreviousSiblings(element);" +
                    "  }" +

                    "  catch (error) {" +

                    "  }" +
                    "}" +

                    "function getPreviousSiblings(element) {" +

                    "  var siblings = [];" +
                    "  var type = element.tagName;" +
                    "  while (element = element.previousSibling) {" +
                    "    if (element.tagName == type){" +
                    "      siblings.push(element);" +
                    "    }" +
                    "  }" +

                    "  var cssString = '';" +
                    "    cssString += ':nth-of-type(' + (siblings.length+1) + ')';" +
                    "  return cssString;" +
                    "}";


    public String JS_GET_CONTAINS_TEXT_PATH =

            "var elementOfInterest;" +

                    "function getTextContainedPath(element) {" +


                    "  try {" +

                    "    if (!elementOfInterest) {" +
                    "      elementOfInterest = element;" +
                    "    }" +

                    "    if (element.tagName == 'BODY') {" +
                    "      return '/';" +
                    "    }" +

                    "    var root = '/';" +
                    "    var xpathPart = getXpathPart(element);" +

                    "    var found = document.evaluate(\"count(\" + root + xpathPart + \")\", document, null, XPathResult.ANY_TYPE, null);" +

                    "    path = root;" +
                    "    if (found.numberValue > 2) {" +
                    "      path = getTextContainedPath(element.parentNode);" +
                    "    }" +

                    "    return path + xpathPart; "+

                    "  } catch (error) {" +

                    "  }" +
                    "}" +

                    "function getXpathPart(element) {" +

                    "  var textContained = getTextContained(element);" +

                    "  if (textContained.length > 0) {" +

                    "    return \"/*[text()[contains(.,'\" + textContained + \"')]]\";" +
                    "  } else {" +
                    "    return '/*';" +
                    "  }" +
                    "}" +

                    "function getTextContained(element) {" +

                    "  if (elementOfInterest === element) {" +

                    "    return chooseBestSubstring(element.innerText);" +

                    "  } else {" +

                    "    child = element.firstChild," +
                    "      texts = [];" +

                    "    while (child) {" +
                    "      if (child.nodeType == 3) {" +
                    "        newText = child.data;" +
                    "        texts.push(newText.trim());" +
                    "      }" +
                    "      child = child.nextSibling;" +
                    "    }" +

                    "    var textString = texts.join(\"\");" +

                    "    return textString" +

                    "  }" +

                    "}" +

                    "function chooseBestSubstring(parentString){\n" +

                    "var subs = parentString.split('\\n');" +
                    "    var longSub = '';" +
                    "    for (var i=0; i<subs.length; i++){" +
                    "    if(subs[i].length > longSub.length){" +
                    "      longSub = subs[i];" +
                    "      }" +
                    "    }" +
                    "return longSub.substr(0, 47);" +
                    "}";

    //public String GENERATE_ALTERNATE_SELECTORS = "return getTagNameOnlyPath(arguments[0])";
    public String GENERATE_ALTERNATE_SELECTORS = "return JSON.stringify([{'css': getTagNameOnlyPath(arguments[0])}, {'xpath': getTextContainedPath(arguments[0])}])";

}
