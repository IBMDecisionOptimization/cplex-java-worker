package com.ibm.optim.oaas.samples.cplex.worker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.ibm.optim.oaas.processor.ProcessorException;
import com.ibm.optim.oaas.processor.job.JobSolveStatus;
import com.ibm.optim.oaas.processor.worker.IWorker;
import com.ibm.optim.oaas.processor.worker.IWorkerContext;
import com.ibm.optim.oaas.processor.worker.LogEngineOutputStream;
import com.ibm.optim.oaas.processor.worker.WorkerProcessingException;

/* --------------------------------------------------------------------------
 * File: MixBlend.java
 * Version 22.1.0  
 * --------------------------------------------------------------------------
 * Licensed Materials - Property of IBM
 * 5725-A06 5725-A29 5724-Y48 5724-Y49 5724-Y54 5724-Y55 5655-Y21
 * Copyright IBM Corporation 2001, 2022. All Rights Reserved.
 *
 * US Government Users Restricted Rights - Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 * --------------------------------------------------------------------------
 * 
 * Problem Description
 * -------------------
 * 
 * Goal is to blend four sources to produce an alloy: pure metal, raw
 * materials, scrap, and ingots.
 * 
 * Each source has a cost.
 * Each source is made up of elements in different proportions.
 * Ingots are discrete, so they are modeled as integers.
 * Alloy has minimum and maximum proportion of each element.
 * 
 * Minimize cost of producing a requested quantity of alloy.
 */
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;

public class MixBlendWorker implements IWorker {
  static int _nbElements = 3;
  static int _nbRaw = 2;
  static int _nbScrap = 2;
  static int _nbIngot = 1;
  static double _alloy = 71.0;

  static double[] _cm = { 22.0, 10.0, 13.0 };
  static double[] _cr = { 6.0, 5.0 };
  static double[] _cs = { 7.0, 8.0 };
  static double[] _ci = { 9.0 };
  static double[] _p = { 0.05, 0.30, 0.60 };
  static double[] _P = { 0.10, 0.40, 0.80 };

  static double[][] _PRaw = { { 0.20, 0.01 }, { 0.05, 0.00 }, { 0.05, 0.30 } };
  static double[][] _PScrap = { { 0.00, 0.01 }, { 0.60, 0.00 }, { 0.40, 0.70 } };
  static double[][] _PIngot = { { 0.10 }, { 0.45 }, { 0.45 } };

  private IWorkerContext workerContext;

  @Override
  public void setWorkerContext(IWorkerContext workerContext) {
    this.workerContext = workerContext;
  }

  @Override
  public JobSolveStatus process(String jobId, Map<String, String> jobParameters)
      throws ProcessorException {

    JobSolveStatus result = JobSolveStatus.UNKNOWN;

    try (IloCplex cplex = new IloCplex()) {
      cplex.setOut(new LogEngineOutputStream(workerContext));
      // Set number of threads to align with pod configuration
      cplex.setParam(IloCplex.Param.Threads, workerContext.getWorkerCoresLimit());
      workerContext.setEffectiveWorkerCoresLimit(workerContext.getWorkerCoresLimit());

      IloNumVar[] m = cplex.numVarArray(_nbElements, 0.0, Double.MAX_VALUE);
      IloNumVar[] r = cplex.numVarArray(_nbRaw, 0.0, Double.MAX_VALUE);
      IloNumVar[] s = cplex.numVarArray(_nbScrap, 0.0, Double.MAX_VALUE);
      IloNumVar[] i = cplex.intVarArray(_nbIngot, 0, Integer.MAX_VALUE);
      IloNumVar[] e = new IloNumVar[_nbElements];
      // Objective Function: Minimize Cost
      IloNumExpr nbElements = cplex.scalProd(_cm, m);
      IloNumExpr nbRaw = cplex.scalProd(_cr, r);
      IloNumExpr nbScrap = cplex.scalProd(_cs, s);
      IloNumExpr nbIngot = cplex.scalProd(_ci, i);
      cplex.addMinimize(cplex.sum(nbElements, nbRaw, nbScrap, nbIngot));
      // Min and max quantity of each element in alloy
      for (int j = 0; j < _nbElements; j++) {
        e[j] = cplex.numVar(_p[j] * _alloy, _P[j] * _alloy);
      }
      // Constraint: produce requested quantity of alloy
      cplex.addEq(cplex.sum(e), _alloy);
      // Constraints: Satisfy element quantity requirements for alloy
      for (int j = 0; j < _nbElements; j++) {
        cplex.addEq(e[j], cplex.sum(m[j], cplex.scalProd(_PRaw[j], r),
            cplex.scalProd(_PScrap[j], s), cplex.scalProd(_PIngot[j], i)));
      }
      if (cplex.solve()) {
        result = getCplexSolveStatus(cplex.getStatus());
        if (result == JobSolveStatus.OPTIMAL_SOLUTION
            || result == JobSolveStatus.FEASIBLE_SOLUTION) {
          workerContext.logEngine(Level.INFO, "Cplex status: " + cplex.getStatus());
          workerContext.logEngine(Level.INFO, "Cost:" + cplex.getObjValue());
          // Report model metrics
          Map<String, String> details = new HashMap<>();
          details.put(getDetailKpiName("model", "BEST_BOUND"),
              Double.toString(cplex.getObjValue()));
          details.put(getDetailKpiName("model", "MIP_GAP"),
              Double.toString(cplex.getMIPRelativeGap()));
          details.put(getDetailKpiName("model", "STATUS"), cplex.getStatus().toString());
          details.put(getDetailKpiName("model", "Elements"),
              Double.toString(cplex.getValue(nbElements)));
          details.put(getDetailKpiName("model", "Raw"), Double.toString(cplex.getValue(nbRaw)));
          details.put(getDetailKpiName("model", "Scrap"), Double.toString(cplex.getValue(nbScrap)));
          details.put(getDetailKpiName("model", "Ingot"), Double.toString(cplex.getValue(nbIngot)));

          details.put(getDetailStatName("model", "int_vars"),
              Integer.toString(cplex.getNintVars()));
          details.put(getDetailStatName("model", "continuous_vars"),
              Integer.toString(cplex.getNcols()));
          details.put(getDetailStatName("model", "linear_constraints"),
              Integer.toString(cplex.getNLCs()));
          details.put(getDetailStatName("model", "bin_vars"),
              Integer.toString(cplex.getNbinVars()));
          details.put(getDetailStatName("model", "quadratic_constraints"),
              Integer.toString(cplex.getNQCs()));
          details.put(getDetailStatName("model", "total_constraints"),
              Integer.toString(cplex.getNrows()));
          details.put(getDetailStatName("model", "total_variables"),
              Integer.toString(cplex.getNcols()));
          workerContext.addSolveDetails(details);

          publishCSV("pure_metal.csv", new String[] { "metal" },
              Arrays.stream(cplex.getValues(m))
                  .boxed()
                  .map(v -> new Object[] { v })
                  .toArray(Object[][]::new));
          publishCSV("raw_material.csv", new String[] { "material" },
              Arrays.stream(cplex.getValues(r))
                  .boxed()
                  .map(v -> new Object[] { v })
                  .toArray(Object[][]::new));
          publishCSV("scrap.csv", new String[] { "scrap" },
              Arrays.stream(cplex.getValues(s))
                  .boxed()
                  .map(v -> new Object[] { v })
                  .toArray(Object[][]::new));
          publishCSV("ingots.csv", new String[] { "ingots" },
              Arrays.stream(cplex.getValues(i))
                  .boxed()
                  .map(v -> new Object[] { v })
                  .toArray(Object[][]::new));
          publishCSV("elements.csv", new String[] { "elements" },
              Arrays.stream(cplex.getValues(e))
                  .boxed()
                  .map(v -> new Object[] { v })
                  .toArray(Object[][]::new));
        }
      }
    } catch (IloException e) {
      throw new WorkerProcessingException("Concert exception", e);
    }
    return result;

  }

  @Override
  public JobSolveStatus stop() throws ProcessorException {
    return JobSolveStatus.UNKNOWN;
  }

  private void publishCSV(String name, String[] headers, Object[][] rows)
      throws ProcessorException {

    workerContext.logEngine(Level.INFO, "Publishing " + name + "...");
    File out = new File(workerContext.getTempDir(), name);
    try (CSVPrinter printer = new CSVPrinter(new FileWriter(out), CSVFormat.DEFAULT.builder()
        .setHeader(headers)
        .build())) {
      for (Object[] row : rows) {
        printer.printRecord(row);
      }
    } catch (IOException e) {
      throw new WorkerProcessingException("Error saving file " + name, e);
    }
    workerContext.setOutputAttachmentFile(name, out);

  }

  private JobSolveStatus getCplexSolveStatus(Status status) {

    if (status.equals(IloCplex.Status.Feasible)) {
      return JobSolveStatus.FEASIBLE_SOLUTION;
    } else if (status.equals(IloCplex.Status.Infeasible)) {
      return JobSolveStatus.INFEASIBLE_SOLUTION;
    } else if (status.equals(IloCplex.Status.InfeasibleOrUnbounded)) {
      return JobSolveStatus.INFEASIBLE_OR_UNBOUNDED_SOLUTION;
    } else if (status.equals(IloCplex.Status.Optimal)) {
      return JobSolveStatus.OPTIMAL_SOLUTION;
    } else if (status.equals(IloCplex.Status.Unbounded)) {
      return JobSolveStatus.UNBOUNDED_SOLUTION;
    }
    return JobSolveStatus.UNKNOWN;

  }
}