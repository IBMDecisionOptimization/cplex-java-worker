package com.ibm.optim.oaas.processor.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.optim.oaas.processor.ProcessorException;

public class TestWorkerContext implements IWorkerContext {
  private Logger logger = Logger.getLogger(TestWorkerContext.class.getName());
  private Path tempDir;
  private int workerCoresLimit = Runtime.getRuntime().availableProcessors();
  private int effectiveWorkerCoresLimit = -1;
  private final Map<String, File> inputAttachments = new HashMap<String, File>();
  private final Map<String, File> outputAttachments = new HashMap<String, File>();
  private final Map<String, String> solveDetails = new HashMap<String, String>();

  public void setUp() {

    try {
      tempDir = Files.createTempDirectory("WorkContextTest");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  public void tearDown() {

    if (tempDir != null) {
      try {
        logger.log(Level.INFO, "Cleaning test directory: " + tempDir);
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

  }

  public void setLogger(Logger logger) {

    this.logger = logger;

  }

  @Override
  public void logEngine(Level level, String message) throws ProcessorException {

    logger.log(level, message);

  }

  @Override
  public List<String> getInputAttachments() {

    return new ArrayList<>(inputAttachments.keySet());

  }

  @Override
  public InputStream getInputAttachment(String attachment) throws ProcessorException {

    try {
      return new BufferedInputStream(new FileInputStream(getInputAttachmentFile(attachment)));
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }

  }

  @Override
  public File getInputAttachmentFile(String attachment) throws ProcessorException {

    return inputAttachments.get(attachment);

  }

  public void setInputAttachment(String attachment, InputStream is) throws ProcessorException {

    try {
      Path tmpFile = Files.createTempFile(tempDir, attachment, ".in");
      Files.copy(is, tmpFile);
      inputAttachments.put(attachment, tmpFile.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  public List<String> getOutputAttachments() {

    return new ArrayList<>(outputAttachments.keySet());

  }

  public File getOutputAttachmentFile(String attachment) {

    return outputAttachments.get(attachment);

  }

  @Override
  public void setOutputAttachment(String attachment, InputStream is) throws ProcessorException {

    try {
      Path tmpFile = Files.createTempFile(tempDir, attachment, ".out");
      Files.copy(is, tmpFile);
      outputAttachments.put(attachment, tmpFile.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  @Override
  public void setOutputAttachmentFile(String attachment, File file) throws ProcessorException {

    outputAttachments.put(attachment, file);

  }

  @Override
  public void setOutputAttachmentFiles(Map<String, File> attachments) throws ProcessorException {

    outputAttachments.putAll(attachments);

  }

  @Override
  public File getTempDir() {

    return tempDir.toFile();

  }

  @Override
  public int getWorkerCoresLimit() {

    return workerCoresLimit;

  }

  public void setWorkerCoresLimit(int workerCoresLimit) {

    this.workerCoresLimit = workerCoresLimit;

  }

  public int getEffectiveWorkerCoresLimit() {

    return effectiveWorkerCoresLimit;

  }

  @Override
  public void setEffectiveWorkerCoresLimit(int count) throws ProcessorException {

    logger.log(Level.INFO, "Effective thread used: " + count);
    effectiveWorkerCoresLimit = count;

  }

  public Map<String, String> getSolveDetails() {

    return solveDetails;

  }

  @Override
  public void addSolveDetails(Map<String, String> details) throws ProcessorException {

    solveDetails.putAll(details);

  }
}
