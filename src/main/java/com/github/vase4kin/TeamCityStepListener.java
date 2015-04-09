package com.github.vase4kin;

import net.thucydides.core.model.*;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sum;

public class TeamCityStepListener implements StepListener {

    private final String FLOW_ID = System.getProperty("teamcity.flowId");

    private static final String MESSAGE_TEMPLATE = "##teamcity[%s %s]";
    private static final String PROPERTY_TEMPLATE = " %s='%s'";

    private static final String EMPTY_STRING = "";

    private static final HashMap<String, String> ESCAPE_CHARS = new LinkedHashMap<String, String>() {
        {
            put("\\|", "||");
            put("\'", "|\'");
            put("\n", "|n");
            put("\r", "|r");
            put("\\[", "|[");
            put("\\]", "|]");
            put("[", "|[");
            put("]", "|]");
        }
    };

    private Logger logger;

    private Stack<String> suiteStack = new Stack<>();

    private Integer examplesTestCount = 0;
    private HashMap<Integer, String> exampleTestNames = new HashMap<>();

    public TeamCityStepListener(Logger logger) {
        this.logger = logger;
    }

    private String currentTestSuiteName = EMPTY_STRING;

    public TeamCityStepListener() {
        this(LoggerFactory.getLogger(TeamCityStepListener.class));
    }

    private String escapeProperty(String value) {
        for (Map.Entry<String, String> escapeChar : ESCAPE_CHARS.entrySet()) {
            value = value.replace(escapeChar.getKey(), escapeChar.getValue());
        }
        return value;
    }

    private void printMessage(String messageName, Map<String, String> properties) {
        StringBuilder propertiesBuilder = new StringBuilder();
        if (FLOW_ID != null) {
            properties.put("flowId", FLOW_ID);
        }
        for (Map.Entry<String, String> property : properties.entrySet()) {
            propertiesBuilder.append(
                    String.format(
                            PROPERTY_TEMPLATE,
                            property.getKey(),
                            escapeProperty(property.getValue())
                    )
            );
        }
        String message = String.format(MESSAGE_TEMPLATE, messageName, propertiesBuilder.toString());
        logger.info(message);
    }

    private void printMessage(String messageName, String description, Long duration) {
        Map<String, String> properties = new HashMap<>();
        properties.put("name", description);
        properties.put("duration", duration.toString());
        printMessage(messageName, properties);
    }

    private void printMessage(String messageName, String description) {
        Map<String, String> properties = new HashMap<>();
        properties.put("name", description);
        printMessage(messageName, properties);
    }

    @Override
    public void testSuiteStarted(Class<?> storyClass) {
        String storyClassName = storyClass.getName();
        if (!currentTestSuiteName.equals(storyClassName)) {
            suiteStack.push(storyClassName);
            printTestSuiteStarted(storyClassName);
            currentTestSuiteName = storyClassName;
        }
    }

    @Override
    public void testSuiteStarted(Story story) {
        String storyName = story.getName();
        suiteStack.push(storyName);
        printTestSuiteStarted(storyName);
    }

    @Override
    public void testSuiteFinished() {
        if (!suiteStack.empty()) {
            String suiteName = suiteStack.pop();
            printTestSuiteFinished(suiteName);
        }
    }

    @Override
    public void testStarted(String description) {
    }

    @Override
    public void testFinished(TestOutcome result) {
        if (result.isDataDriven()) {
            printExampleResults(result);
        } else {
            printTestStarted(result);
            if (result.isFailure() || result.isError()) {
                printFailure(result);
            } else if (result.isSkipped() || result.isPending()) {
                printTestIgnored(result);
            }
            printTestFinished(result);
        }
    }

    @Override
    public void testRetried() {
    }

    private void printFailure(TestOutcome result) {
        HashMap<String, String> properties = new HashMap<>();
        properties.put("name", getResultTitle(result));
        properties.put("message", getTestOutComeTestFailureCauseMessage(result.getTestFailureCause()));
        properties.put("details", getStepsInfo(result.getTestSteps()));
        printMessage("testFailed", properties);
    }

    private String getTestOutComeTestFailureCauseMessage(FailureCause failureCause) {
        if (failureCause != null && failureCause.getMessage() != null) {
            return failureCause.getMessage();
        } else {
            return EMPTY_STRING;
        }
    }

    private void printExampleResults(TestOutcome result) {
        List<TestStep> testSteps = result.getTestSteps();
        int number = 0;
        for (int i = 0; i < testSteps.size(); i++) {
            if (isExample(testSteps.get(i))) {
                List<TestStep> childrenTestSteps = result.getTestSteps().get(i).getChildren();
                String testName = getResultTitle(result, exampleTestNames.get(number));
                Long duration = sum(childrenTestSteps, on(TestStep.class).getDuration());
                printTestStarted(testName);
                if (hasFailureStep(childrenTestSteps)) {
                    String getStepsInfo = getStepsInfo(childrenTestSteps);
                    HashMap<String, String> properties = new HashMap<>();
                    properties.put("name", testName);
                    properties.put("details", getStepsInfo);
                    printMessage("testFailed", properties);
                } else if (hasPendingStep(childrenTestSteps)) {
                    printTestIgnored(testName);
                }
                printTestFinished(testName, duration);
                number++;
            }
        }
        examplesTestCount = 0;
        exampleTestNames.clear();
    }

    private boolean isExample(TestStep testStep) {
        return testStep.isAGroup() && testStep.getDescription().startsWith("[");
    }

    private String getStepsInfo(List<TestStep> testSteps) {
        StringBuilder builder = new StringBuilder("Steps:\r\n");
        for (TestStep testStep : testSteps) {
            String stepMessage = String.format("%s (%s) -> %s\r\n", testStep.getDescription(), testStep.getDurationInSeconds(), getResultMessage(testStep));
            builder.append(stepMessage);
        }
        return builder.toString();
    }

    private Boolean hasFailureStep(List<TestStep> testSteps) {
        for (TestStep testStep : testSteps) {
            if (testStep.isError() || testStep.isFailure()) {
                return true;
            }
        }
        return false;
    }

    private Boolean hasPendingStep(List<TestStep> testSteps) {
        for (TestStep testStep : testSteps) {
            if (testStep.isSkipped() || testStep.isPending() || testStep.isIgnored()) {
                return true;
            }
        }
        return false;
    }

    private String getResultMessage(TestStep testStep) {
        StringBuilder builder = new StringBuilder();
        builder.append(testStep.getResult().toString());
        if (testStep.isFailure() || testStep.isError()) {
            String exceptionCauseStackTrace;
            if (testStep.isAGroup()) {
                exceptionCauseStackTrace = "Children " + getStepsInfo(testStep.getChildren());
            } else {
                exceptionCauseStackTrace = testStep.getException() != null
                        ? getStackTrace(testStep.getException().toException())
                        : EMPTY_STRING;
            }
            builder.append(
                    String.format("\r\n%s", exceptionCauseStackTrace)
            );
        }
        return builder.toString();
    }

    protected String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString().trim();
    }

    private String getResultTitle(TestOutcome result) {
        String path = result.getPath();
        if(path.contains("stories/")) {
            path = path.split("stories/")[1];
        }
        if (path.endsWith(".story")) {
            path = path.substring(0, path.length() - 6);
        }
        String title = path.replace(".", "_").replace("/", ".");
        return title + "." + result.getMethodName().replace(".", "_");
    }

    private String getResultTitle(TestOutcome result, String name) {
        String title = getResultTitle(result);
        title = title + "." + name.replace(".", "_");
        return title;
    }

    private void printTestStarted(String name) {
        printMessage("testStarted", name);
    }

    private void printTestStarted(TestOutcome result) {
        printMessage("testStarted", getResultTitle(result));
    }

    private void printTestIgnored(TestOutcome result) {
        printMessage("testIgnored", getResultTitle(result));
    }

    private void printTestIgnored(String name) {
        printMessage("testIgnored", name);
    }

    private void printTestFinished(TestOutcome result) {
        printMessage("testFinished", getResultTitle(result), result.getDuration());
    }

    private void printTestFinished(String name, Long duration) {
        printMessage("testFinished", name, duration);
    }

    private void printTestSuiteFinished(String name) {
        printMessage("testSuiteFinished", name);
    }

    private void printTestSuiteStarted(String name) {
        printMessage("testSuiteStarted", name);
    }

    @Override
    public void testFailed(TestOutcome testOutcome, Throwable cause) {
    }

    @Override
    public void testIgnored() {
    }

    @Override
    public void testSkipped() {
    }

    @Override
    public void testPending() {
    }

    @Override
    public void stepStarted(ExecutedStepDescription description) {
    }

    @Override
    public void skippedStepStarted(ExecutedStepDescription description) {
    }

    @Override
    public void stepFailed(StepFailure failure) {
    }

    @Override
    public void lastStepFailed(StepFailure failure) {
    }

    @Override
    public void stepIgnored() {
    }

    @Override
    public void stepPending() {
    }

    @Override
    public void stepPending(String message) {
    }

    @Override
    public void stepFinished() {
    }

    @Override
    public void notifyScreenChange() {
    }

    @Override
    public void useExamplesFrom(DataTable table) {
    }

    @Override
    public void exampleStarted(Map<String, String> data) {
        exampleTestNames.put(examplesTestCount, data.toString());
        examplesTestCount++;
    }

    @Override
    public void exampleFinished() {
    }

    @Override
    public void assumptionViolated(String message) {
    }
}
