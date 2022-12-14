package com.example.demo.config;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.listener.TaskletStepListener;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

	/** StepBuilderのFactoryクラス */
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	/** JobBuilderのFactoryクラス */
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	@Qualifier("FirstTasklet")
	private Tasklet firstTasklet;
	
	@Autowired
	@Qualifier("SuccessTasklet")
	private Tasklet successTasklet;
	
	@Autowired
	@Qualifier("FailTasklet")
	private Tasklet failTasklet;
	
	@Autowired
	@Qualifier("TaskletStepListener")
	private TaskletStepListener taskletStepListener;
	
	@Autowired
	@Qualifier("RandomTasklet")
	private Tasklet randomTasklet;
	
	@Autowired
	private JobExecutionDecider sampleDecider;
	
	/** FirstStepを生成 */
	@Bean
	public Step firstStep() {
		
		return stepBuilderFactory.get("FirstStep")
				.tasklet(firstTasklet)
				.listener(taskletStepListener)
				.build();
	}
	
	/** SuccessStepを生成 */
	@Bean
	public Step successStep() {
		
		return stepBuilderFactory.get("SuccessStep")
				.tasklet(successTasklet)
				.build();
	}
	
	/** FailStepの生成 */
	@Bean
	public Step failStep() {
		
		return stepBuilderFactory.get("FailStep")
				.tasklet(failTasklet)
				.build();
	}
	
	/** RandomStepを生成 */
	@Bean
	public Step randomStep() {
		
		return stepBuilderFactory.get("RandomStep") // Builderの取得
				.tasklet(randomTasklet) // Taskletのセット
				.listener(taskletStepListener) // listener
				.build(); // Stepの生成
	}
	
	/** Taskletの分岐Jobを生成 */
	@Bean
	public Job taskletBranchJob() throws Exception {
		
		return jobBuilderFactory.get("TaskletBranchJob")
				.incrementer(new RunIdIncrementer())
				.start(firstStep()) // 最初のStepをセット
				.on(ExitStatus.COMPLETED.getExitCode()) // COMPLETEDの場合
				.to(successStep()) // Step2へ
				.from(firstStep()) // Step1へ戻る
				.on(ExitStatus.FAILED.getExitCode()) // FAILEDの場合
				.to(failStep()) // Step3へ
				.end() // 分岐終了
				.build(); // Jobの生成
	}
	
	/** RandomTaskletの分岐のJobを生成 */
	@Bean
	public Job randomTaskletBranchJob() throws Exception {
		
		return jobBuilderFactory.get("RandomTaskletBranchJob")
				.incrementer(new RunIdIncrementer())
				.start(randomStep()) // 最初のStep
				.next(sampleDecider) // Deciderへ
				.from(sampleDecider) // Deciderに戻る
				.on(FlowExecutionStatus.COMPLETED.getName())
				.to(successStep())
				.from(sampleDecider) // Deciderに戻る
				.on(FlowExecutionStatus.FAILED.getName())
				.to(failStep())
				.end() // 分岐終了
				.build(); // Jobの生成
	}
}
