/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.execution.step.tasklet;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInterruptedException;
import org.springframework.batch.core.domain.StepSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * A {@link Step} that executes a {@link Tasklet} directly. This step does not manage transactions or any looping
 * functionality. The tasklet should do this on its own.
 * 
 * @author Ben Hale
 */
public class TaskletStep extends StepSupport {

	private static final Log logger = LogFactory.getLog(TaskletStep.class);

	private final Tasklet tasklet;

	private final JobRepository jobRepository;

	/**
	 * Creates a new <code>Step</code> for executing a <code>Tasklet</code>
	 * 
	 * @param tasklet The <code>Tasklet</code> to execute
	 * @param jobRepository The <code>JobRepository</code> to use for persistence of incremental state
	 */
	public TaskletStep(Tasklet tasklet, JobRepository jobRepository) {
		this.tasklet = tasklet;
		this.jobRepository = jobRepository;
	}

	public void execute(StepExecution stepExecution) throws StepInterruptedException, BatchCriticalException {
		stepExecution.setStartTime(new Date());
		updateStatus(stepExecution, BatchStatus.STARTED);

		ExitStatus exitStatus = ExitStatus.FAILED;
		try {
			exitStatus = tasklet.execute();
			updateStatus(stepExecution, BatchStatus.COMPLETED);
		} catch (Exception e) {
			logger.error("Encountered an error running the tasklet");
			updateStatus(stepExecution, BatchStatus.FAILED);
			throw new BatchCriticalException(e);
		} finally {
			stepExecution.setExitStatus(exitStatus);
			stepExecution.setEndTime(new Date());
			jobRepository.saveOrUpdate(stepExecution);
		}
	}

	private void updateStatus(StepExecution stepExecution, BatchStatus status) {
		stepExecution.setStatus(status);
		jobRepository.saveOrUpdate(stepExecution);
	}

}
