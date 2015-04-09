package com.github.vase4kin;

import junit.framework.TestCase;
import net.thucydides.core.model.*;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.Logger;

import java.util.HashMap;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class to test implemented team city thucydides step listener
 */
public class TeamCityStepListenerTest {

    @Mock
    private Logger logger;

    @Mock
    private DataTable dataTable;

    @Mock
    private FailureCause failureCause;

    private TeamCityStepListener teamCityStepListener;

    private static final String STORY_PATH = "stories/sprint-1/us-1/story.story";
    private static final Story STORY = Story.withIdAndPath("storyId", "Test story", STORY_PATH);
    private static final Throwable THROWABLE = new Throwable("the test is failed!");
    private static final ExecutedStepDescription EXECUTED_STEP_DESCRIPTION = ExecutedStepDescription.withTitle("step");
    private static final StepFailure STEP_FAILURE = new StepFailure(EXECUTED_STEP_DESCRIPTION, THROWABLE);

    @Before
    public void before() {
        initMocks(this);
        teamCityStepListener = spy(new TeamCityStepListener(logger));
        doReturn("StackTrace").when(teamCityStepListener).getStackTrace(any(Throwable.class));
        when(failureCause.getMessage()).thenReturn("the test is failed!");
    }

    @After
    public void after() {
        System.clearProperty("teamcity.flowId");
    }

    @Test
    public void testScenarioResultIsSuccess() {

        TestOutcome testOutcome = new TestOutcome("passedScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getSuccessfulTestStep("Passed"));

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.passedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.passedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(2)).info(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioResultIsFailure() {

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getFailureTestStepWithAssertionError("Failed scenario step"));
        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='the test is failed!' details='Steps:|r|nFailed scenario step (0.1) -> FAILURE|r|nStackTrace|r|n' name='sprint-1.us-1.story.failedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioChildStepResultIsSuccess() {

        TestOutcome testOutcome = new TestOutcome("successScenario");
        testOutcome.setUserStory(STORY);
        TestStep testStep = TestStepFactory.getSuccessfulTestStep("Success scenario step");
        testStep.addChildStep(TestStepFactory.getSuccessfulTestStep("Success scenario child step"));
        testOutcome.recordStep(testStep);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.successScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.successScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(2)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioChildStepResultIsError() {

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        TestStep testStep = TestStepFactory.getErrorTestStep("Failed scenario step");
        testStep.addChildStep(TestStepFactory.getErrorTestStepWithThrowable("Failed scenario child step"));
        testOutcome.recordStep(testStep);
        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='the test is failed!' details='Steps:|r|nFailed scenario step (0.1) -> ERROR|r|nChildren Steps:|r|nFailed scenario child step (0.1) -> ERROR|r|nStackTrace|r|n|r|n' name='sprint-1.us-1.story.failedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioChildStepResultIsFailure() {

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        TestStep testStep = TestStepFactory.getFailureTestStep("Failed scenario step");
        testStep.addChildStep(TestStepFactory.getFailureTestStepWithAssertionError("Failed scenario child step"));
        testOutcome.recordStep(testStep);
        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='the test is failed!' details='Steps:|r|nFailed scenario step (0.1) -> FAILURE|r|nChildren Steps:|r|nFailed scenario child step (0.1) -> FAILURE|r|nStackTrace|r|n|r|n' name='sprint-1.us-1.story.failedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioResultIsError() {

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getErrorTestStepWithThrowable("Failed scenario step"));
        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='the test is failed!' details='Steps:|r|nFailed scenario step (0.1) -> ERROR|r|nStackTrace|r|n' name='sprint-1.us-1.story.failedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioResultIsSkipped() {

        TestOutcome testOutcome = new TestOutcome("skippedScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getSkippedTestStep("Skipped scenario step"));

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.skippedScenario']";
        String testIgnoredExpectedMessage = "##teamcity[testIgnored  name='sprint-1.us-1.story.skippedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.skippedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testIgnoredExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioResultIsPending() {

        TestOutcome testOutcome = new TestOutcome("pendingScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getPendingTestStep("Pending scenario step"));

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.pendingScenario']";
        String testIgnoredExpectedMessage = "##teamcity[testIgnored  name='sprint-1.us-1.story.pendingScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.pendingScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testIgnoredExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioResultIsIgnored() {

        TestOutcome testOutcome = new TestOutcome("ignoringScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getIgnoredTestStep("Ignored scenario step"));

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.ignoringScenario']";
        String testIgnoredExpectedMessage = "##teamcity[testIgnored  name='sprint-1.us-1.story.ignoringScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.ignoringScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testIgnoredExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testParametrisedSuccessfulScenarioWithGivenStoriesInStory() {

        teamCityStepListener.exampleStarted(new HashMap<String, String>() {{
            put("value", "exampleTableValue");
        }});

        TestOutcome testOutcome = new TestOutcome("parametrisedScenario");
        testOutcome.useExamplesFrom(dataTable);
        testOutcome.setUserStory(STORY);
        TestStep testStep = TestStepFactory.getSuccessfulTestStep("Successful scenario step");
        testStep.addChildStep(TestStepFactory.getSuccessfulTestStep("Successful scenario child step"));
        testOutcome.recordStep(testStep);
        TestStep testStep2 = TestStepFactory.getSuccessfulTestStep("[1] {value=exampleTableValue");
        testStep2.addChildStep(TestStepFactory.getSuccessfulTestStep("Successful scenario child step"));
        testOutcome.recordStep(testStep2);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(2)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFinishedExpectedMessage));
    }

    @Test
    public void testParametrisedFailedScenario() {

        teamCityStepListener.exampleStarted(new HashMap<String, String>() {{
            put("value", "exampleTableValue");
        }});

        TestOutcome testOutcome = new TestOutcome("parametrisedScenario");
        testOutcome.useExamplesFrom(dataTable);
        testOutcome.setUserStory(STORY);

        TestStep testStep1 = TestStepFactory.getFailureTestStep("[1] {value=exampleTableValue");
        testStep1.addChildStep(TestStepFactory.getFailureTestStepWithAssertionError("Failed scenario child step"));
        testOutcome.recordStep(testStep1);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testFailedExpectedMessage = "##teamcity[testFailed  details='Steps:|r|nFailed scenario child step (0.1) -> FAILURE|r|nStackTrace|r|n' name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testParametrisedErrorScenario() {

        teamCityStepListener.exampleStarted(new HashMap<String, String>() {{
            put("value", "exampleTableValue");
        }});

        TestOutcome testOutcome = new TestOutcome("parametrisedScenario");
        testOutcome.useExamplesFrom(dataTable);
        testOutcome.setUserStory(STORY);

        TestStep testStep = TestStepFactory.getErrorTestStep("[1] {value=exampleTableValue");
        testStep.addChildStep(TestStepFactory.getErrorTestStepWithThrowable("Failed scenario child step"));
        testOutcome.recordStep(testStep);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testErrorExpectedMessage = "##teamcity[testFailed  details='Steps:|r|nFailed scenario child step (0.1) -> ERROR|r|nStackTrace|r|n' name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testErrorExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testParametrisedSkippedScenario() {

        teamCityStepListener.exampleStarted(new HashMap<String, String>() {{
            put("value", "exampleTableValue");
        }});

        TestOutcome testOutcome = new TestOutcome("parametrisedScenario");
        testOutcome.useExamplesFrom(dataTable);
        testOutcome.setUserStory(STORY);

        TestStep testStep = TestStepFactory.getSkippedTestStep("[1] {value=exampleTableValue");
        testStep.addChildStep(TestStepFactory.getSkippedTestStep("Failed scenario child step"));
        testOutcome.recordStep(testStep);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testSkippedExpectedMessage = "##teamcity[testIgnored  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testSkippedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testParametrisedPendingScenario() {

        teamCityStepListener.exampleStarted(new HashMap<String, String>() {{
            put("value", "exampleTableValue");
        }});

        TestOutcome testOutcome = new TestOutcome("parametrisedScenario");
        testOutcome.useExamplesFrom(dataTable);
        testOutcome.setUserStory(STORY);

        TestStep testStep = TestStepFactory.getPendingTestStep("[1] {value=exampleTableValue");
        testStep.addChildStep(TestStepFactory.getPendingTestStep("Failed scenario child step"));
        testOutcome.recordStep(testStep);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testPendingExpectedMessage = "##teamcity[testIgnored  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testPendingExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testParametrisedIgnoredScenario() {

        teamCityStepListener.exampleStarted(new HashMap<String, String>() {{
            put("value", "exampleTableValue");
        }});

        TestOutcome testOutcome = new TestOutcome("parametrisedScenario");
        testOutcome.useExamplesFrom(dataTable);
        testOutcome.setUserStory(STORY);

        TestStep testStep = TestStepFactory.getIgnoredTestStep("[1] {value=exampleTableValue");
        testStep.addChildStep(TestStepFactory.getIgnoredTestStep("Failed scenario child step"));
        testOutcome.recordStep(testStep);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testPendingExpectedMessage = "##teamcity[testIgnored  name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.parametrisedScenario.{value=exampleTableValue}']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testPendingExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testEscapingSymbols() {

        TestOutcome testOutcome = new TestOutcome("\\|'\n\r\\[\\][]");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getFailureTestStepWithAssertionError("\\|'\n\r\\[\\][]"));

        FailureCause failureCause = mock(FailureCause.class);
        when(failureCause.getMessage()).thenReturn("\\|'\n\r\\[\\][]");

        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.|||'|n|r||[||]|[|]']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='|||'|n|r||[||]|[|]' details='Steps:|r|n|||'|n|r||[||]|[|] (0.1) -> FAILURE|r|nStackTrace|r|n' name='sprint-1.us-1.story.|||'|n|r||[||]|[|]']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.|||'|n|r||[||]|[|]']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testMessagePropertyCantBeNullIfResultHasTestFailureCauseInstanceOfNPE() {

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getErrorTestStepWithThrowable("Failed scenario step", new NullPointerException()));

        FailureCause failureCause = mock(FailureCause.class);

        NullPointerException nullPointerException = new NullPointerException();
        when(failureCause.getStackTrace()).thenReturn(null);
        when(failureCause.getMessage()).thenReturn(null);
        when(failureCause.getErrorType()).thenReturn(nullPointerException.getClass().getName());

        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='' details='Steps:|r|nFailed scenario step (0.1) -> ERROR|r|nStackTrace|r|n' name='sprint-1.us-1.story.failedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testSuiteIsAStoryClass() {

        teamCityStepListener.testSuiteStarted(TestCase.class);
        teamCityStepListener.testSuiteFinished();

        String testSuiteStartedExpectedMessage = "##teamcity[testSuiteStarted  name='junit.framework.TestCase']";
        String testSuiteFinishedExpectedMessage = "##teamcity[testSuiteFinished  name='junit.framework.TestCase']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(2)).info(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testSuiteStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testSuiteFinishedExpectedMessage));
    }

    @Test
    public void testSuiteIsAStory() {

        teamCityStepListener.testSuiteStarted(STORY);
        teamCityStepListener.testSuiteFinished();

        String testSuiteStartedExpectedMessage = "##teamcity[testSuiteStarted  name='Test story']";
        String testSuiteFinishedExpectedMessage = "##teamcity[testSuiteFinished  name='Test story']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(2)).info(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testSuiteStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testSuiteFinishedExpectedMessage));
    }

    @Test
    public void testResultTitleIfPathIsDifferent() {

        String storyPath = "jbehave/stories/consult_dictionary/LookupADefinition.story";
        Story story = Story.withIdAndPath("storyId", "Test story", storyPath);

        TestOutcome testOutcome = new TestOutcome("passedScenario");
        testOutcome.setUserStory(story);
        testOutcome.recordStep(TestStepFactory.getSuccessfulTestStep("Passed"));

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='consult_dictionary.LookupADefinition.passedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='consult_dictionary.LookupADefinition.passedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(2)).info(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFinishedExpectedMessage));
    }

    @Test
    public void duplicatedTestSuitePrintMessageCallsIfTestSuiteIsAClassAndTestSuiteContainsMoreThanOneTest() {

        TestOutcome testOutcome = new TestOutcome("passedScenario", TestCase.class);
        testOutcome.recordStep(TestStepFactory.getSuccessfulTestStep("Passed"));

        TestOutcome testOutcome2 = new TestOutcome("passedScenario2", TestCase.class);
        testOutcome2.recordStep(TestStepFactory.getSuccessfulTestStep("Passed"));

        String testSuiteStartedExpectedMessage = "##teamcity[testSuiteStarted  name='junit.framework.TestCase']";
        String testSuiteFinishedExpectedMessage = "##teamcity[testSuiteFinished  name='junit.framework.TestCase']";

        String testStartedExpectedMessage = "##teamcity[testStarted  name='junit_framework.passedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='junit_framework.passedScenario']";

        String testStartedExpectedMessage2 = "##teamcity[testStarted  name='junit_framework.passedScenario2']";
        String testFinishedExpectedMessage2 = "##teamcity[testFinished  duration='100' name='junit_framework.passedScenario2']";

        teamCityStepListener.testSuiteStarted(TestCase.class);
        teamCityStepListener.testFinished(testOutcome);
        teamCityStepListener.testSuiteStarted(TestCase.class);
        teamCityStepListener.testFinished(testOutcome2);
        teamCityStepListener.testSuiteFinished();

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(6)).info(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testSuiteStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(3), is(testStartedExpectedMessage2));
        assertThat(stringArgumentCaptor.getAllValues().get(4), is(testFinishedExpectedMessage2));
        assertThat(stringArgumentCaptor.getAllValues().get(5), is(testSuiteFinishedExpectedMessage));
    }

    @Test
    public void testGetStackTraceMethod() {

        assertThat(new TeamCityStepListener().getStackTrace(THROWABLE), containsString("java.lang.Throwable: the test is failed!"));
        assertThat(new TeamCityStepListener().getStackTrace(THROWABLE), containsString("com.github.vase4kin.TeamCityStepListenerTest.<clinit>(TeamCityStepListenerTest.java:"));
    }

    @Test
    public void testParametrisedScenarioIfTestStepIsNotGroupAndHasNotExampleTestDescription() {

        // this scenario can't be possible
        // the testOutCome can't be dataDriven(ExamplesTable)
        // if testOutCome step is not contain children steps (testStep.isAGroup() == false)
        // and each children step do not have test description started with '[', example: [1] {value=exampleTableValue}
        // for code coverage

        teamCityStepListener.exampleStarted(new HashMap<String, String>() {{
            put("value", "exampleTableValue");
        }});

        TestOutcome testOutcome = new TestOutcome("parametrisedScenario");
        testOutcome.useExamplesFrom(dataTable);
        testOutcome.setUserStory(STORY);
        TestStep testStep = TestStepFactory.getSuccessfulTestStep("{value=exampleTableValue");
        testOutcome.recordStep(testStep);

        teamCityStepListener.testFinished(testOutcome);

        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testScenarioIfResultTestFailureCauseIsNull() {

        // testFailureCause can't be null
        // for code coverage

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getFailureTestStepWithAssertionError("Failed scenario step"));
        testOutcome.setTestFailureCause(null);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='' details='Steps:|r|nFailed scenario step (0.1) -> FAILURE|r|nStackTrace|r|n' name='sprint-1.us-1.story.failedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioIfTestOutComeTestStepCanBeFailureAndErrorAtTheSameTime() {

        // testOutcome test step can't be failure and error at the same time
        // for code coverage

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        TestStep testStep = spy(TestStepFactory.getFailureTestStepWithAssertionError("Failed scenario step"));
        doReturn(false).when(testStep).isError();
        doReturn(false).when(testStep).isFailure();

        testOutcome.recordStep(testStep);
        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='the test is failed!' details='Steps:|r|nFailed scenario step (0.1) -> FAILURE|r|n' name='sprint-1.us-1.story.failedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testScenarioIfTestOutComeTestStepExceptionCanBeNull() {

        // testOutcome test step exception can't be null
        // for code coverage

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        TestStep testStep = spy(TestStepFactory.getFailureTestStepWithAssertionError("Failed scenario step"));
        doReturn(null).when(testStep).getException();

        testOutcome.recordStep(testStep);
        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='the test is failed!' details='Steps:|r|nFailed scenario step (0.1) -> FAILURE|r|n|r|n' name='sprint-1.us-1.story.failedScenario']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    @Test
    public void testTestStartedMethodNoLoggerMessage() {

        teamCityStepListener.testStarted("test");
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testTestRetriedMethodNoLoggerMessage() {

        teamCityStepListener.testRetried();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testTestIgnoredMethodNoLoggerMessage() {

        teamCityStepListener.testIgnored();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testTestFailedMethodNoLoggerMessage() {

        teamCityStepListener.testFailed(new TestOutcome("test outcome"), THROWABLE);
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testStepStartedMethodNoLoggerMessage() {

        teamCityStepListener.stepStarted(EXECUTED_STEP_DESCRIPTION);
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testSkippedStepStartedMethodNoLoggerMessage() {

        teamCityStepListener.skippedStepStarted(EXECUTED_STEP_DESCRIPTION);
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testStepFailedMethodNoLoggerMessage() {

        teamCityStepListener.stepFailed(STEP_FAILURE);
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testLastStepFailedMethodNoLoggerMessage() {

        teamCityStepListener.lastStepFailed(STEP_FAILURE);
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testStepIgnoredMethodNoLoggerMessage() {

        teamCityStepListener.stepIgnored();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testStepPendingMethodNoLoggerMessage() {

        teamCityStepListener.stepPending();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testStepPendingWithMessageMethodNoLoggerMessage() {

        teamCityStepListener.stepPending("pending");
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testStepFinishedMethodNoLoggerMessage() {

        teamCityStepListener.stepFinished();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testNotifyScreenChangeMethodNoLoggerMessage() {

        teamCityStepListener.notifyScreenChange();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testUseExamplesFromMethodNoLoggerMessage() {

        teamCityStepListener.useExamplesFrom(dataTable);
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testExampleFinishedMethodNoLoggerMessage() {

        teamCityStepListener.exampleFinished();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testAssumptionViolatedMethodNoLoggerMessage() {

        teamCityStepListener.assumptionViolated("message");
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testSuiteFinishedMethodInvokesTwoTimesAfterTestStoryFinished() {

        teamCityStepListener.testSuiteStarted(STORY);
        teamCityStepListener.testSuiteFinished();
        teamCityStepListener.testSuiteFinished();
    }

    @Test
    public void testSkippedMethodNoLoggerMessage() {

        teamCityStepListener.testSkipped();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testPendingMethodNoLoggerMessage() {

        teamCityStepListener.testPending();
        verifyArgumentCaptorCapturesNoLoggerMessages();
    }

    @Test
    public void testFlowIdProperty() {

        //setting team city flow id number
        System.setProperty("teamcity.flowId", "1");

        //init again teamcity step listener
        before();

        TestOutcome testOutcome = new TestOutcome("failedScenario");
        testOutcome.setUserStory(STORY);
        testOutcome.recordStep(TestStepFactory.getFailureTestStepWithAssertionError("Failed scenario step"));
        testOutcome.setTestFailureCause(failureCause);

        teamCityStepListener.testFinished(testOutcome);

        String testStartedExpectedMessage = "##teamcity[testStarted  name='sprint-1.us-1.story.failedScenario' flowId='1']";
        String testFailedExpectedMessage = "##teamcity[testFailed  message='the test is failed!' details='Steps:|r|nFailed scenario step (0.1) -> FAILURE|r|nStackTrace|r|n' name='sprint-1.us-1.story.failedScenario' flowId='1']";
        String testFinishedExpectedMessage = "##teamcity[testFinished  duration='100' name='sprint-1.us-1.story.failedScenario' flowId='1']";

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(3)).info(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getAllValues().get(0), is(testStartedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(1), is(testFailedExpectedMessage));
        assertThat(stringArgumentCaptor.getAllValues().get(2), is(testFinishedExpectedMessage));
    }

    private void verifyArgumentCaptorCapturesNoLoggerMessages() {
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, never()).info(stringArgumentCaptor.capture());
    }
}
