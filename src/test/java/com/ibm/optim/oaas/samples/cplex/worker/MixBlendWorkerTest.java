package com.ibm.optim.oaas.samples.cplex.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.BeforeParam;

import com.ibm.optim.oaas.processor.ProcessorException;
import com.ibm.optim.oaas.processor.job.JobSolveStatus;
import com.ibm.optim.oaas.processor.worker.IWorker;
import com.ibm.optim.oaas.processor.worker.TestWorkerContext;

public class MixBlendWorkerTest {
  private TestWorkerContext workerContext;

  @Before
  public void setUp() {
    workerContext = new TestWorkerContext();
    workerContext.setUp();
  }

  @After
  public void tearDown() {
    if (workerContext != null) {
      workerContext.tearDown();
    }
  }

  @Test
  @BeforeParam
  public void testProcess() throws ProcessorException {

    IWorker worker = new MixBlendWorker();
    worker.setWorkerContext(workerContext);
    Map<String, String> parameters = new HashMap<>();
    assertEquals(JobSolveStatus.OPTIMAL_SOLUTION, worker.process("testProcess", parameters));
    assertFalse(workerContext.getSolveDetails().isEmpty());
    assertFalse(workerContext.getOutputAttachments().isEmpty());

  }

}
