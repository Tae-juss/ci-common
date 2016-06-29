package com.browserstack.automate.ci.common;

import com.browserstack.automate.AutomateClient;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

public class TestCaseTracker {
    private static final String PROPERTY_DEBUG = "browserstack.automate.debug";
    private static String TAG = "[BrowserStackAutomate]";

    private final AutomateClient automateClient;

    public TestCaseTracker(String username, String accessKey) {
        this.automateClient = new AutomateClient(username, accessKey);
    }

    public AutomateClient getAutomateClient() {
        return automateClient;
    }

    public static String getTestCaseHash(String testCaseName) {
        return Utils.generateHash(testCaseName);
    }

    public static String[] findTestCaseSession(List<AutomateTestCase> testCaseList, String testCaseName, String testCaseHash, long testIndex) {
        Iterator<AutomateTestCase> testCaseIterator = testCaseList.iterator();
        while (testCaseIterator.hasNext()) {
            AutomateTestCase atc = testCaseIterator.next();

            logDebug(System.out, ">>  cr => " + testCaseName + " | " + testIndex);
            logDebug(System.out, ">> atc => " + (atc.hasTestHash() ? atc.testHash : atc.testFullPath) + " | " + atc.testIndex);

            if (testIndex != atc.testIndex) {
                logDebug(System.out, ">> => Mismatch: " + testIndex + " != " + atc.testIndex);
                continue;
            }

            boolean matchedHash = (atc.hasTestHash() && testCaseHash.equals(atc.testHash));
            if (matchedHash || testCaseName.equals(atc.testFullPath)) {
                testCaseIterator.remove();
                logDebug(System.out, ">> CaseResult: " + testCaseName + " {" + testIndex + "} <=> {"
                        + atc.testIndex + "} matched: " + testCaseList.size());
                logDebug(System.out, "findTestCaseSession: end with session");

                return new String[]{atc.sessionId, matchedHash ? testCaseHash : testCaseName};
            }
        }

        return null;
    }

    public static boolean isDebugEnabled() {
        return System.getProperty(PROPERTY_DEBUG, "false").equals("true");
    }

    public static void log(PrintStream printStream, String message) {
        printStream.println(TAG + ": " + message);
    }

    public static void logDebug(PrintStream printStream, String message) {
        if (isDebugEnabled()) printStream.println(TAG + ": " + message);
    }

    public static void setTag(String tag) {
        TAG = tag;
    }
}
