package com.test.feeextract;

import com.test.feeextract.domain.JobStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FeeextractApplication {

	public static void main(String[] args) {

		JobStatus com = JobStatus.COMPLETED;
		System.out.println("1.com: " + com.getDescription());
		System.out.println("2.com: " + com.name());
		System.out.println("3.com: " + com.isCompleted());

		com = JobStatus.FAILED;

		System.out.println("1.f: " + com.getDescription());
		System.out.println("2.f: " + com.name());
		System.out.println("3.f: " + com.isCompleted());


		System.out.println("Math.random : " +Math.random());

		System.out.println("(int)Math.random() : " +(int)Math.random() * 3 + 1);
		//System.out.println("(int)(Math.random() * 3) + 1 : " + (int)(Math.random() * 3) + 1;
		int randomSeconds = (int)(Math.random() * 3) + 1;


		SpringApplication.run(FeeextractApplication.class, args);
	}

}
