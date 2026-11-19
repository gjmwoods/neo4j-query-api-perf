package org.example;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
	public static void main(String[] args) throws InterruptedException {

		var driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.none(), Config.builder()
						//.withMaxConnectionPoolSize(1000)
						.withLogging(Logging.none()).build());

		driver.verifyConnectivity();

		var noOfThreads = 100;
		var noToExecPerThread = 2000;
		var totalReqs = noToExecPerThread * noOfThreads;

		var threadPool = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger errorCount = new AtomicInteger();


		var start = System.currentTimeMillis();
		for (int i = 0; i<noOfThreads; i++){
			threadPool.submit(() -> {
				AtomicInteger count = new AtomicInteger();


				while (count.get() !=noToExecPerThread) {
                    try (var session = driver.session()){
                        var result = session.run("CALL queryAPI.nightnight(500);");
                            result.consume();
                            count.getAndIncrement();
                    } catch (Exception e) {
                        errorCount.getAndIncrement();
                        System.out.println(e.getMessage());
                    }
                }
			});
		}

		threadPool.shutdown();
		threadPool.awaitTermination(1000, TimeUnit.SECONDS);
		var end = System.currentTimeMillis();
		var totalTime = end - start;
		System.out.println(String.format("%s requests executed in %s milis", totalReqs, totalTime));
        System.out.println(String.format("%s errors", errorCount));
		System.out.println(String.format("%s reqs per mili", totalReqs/totalTime));
	}
}