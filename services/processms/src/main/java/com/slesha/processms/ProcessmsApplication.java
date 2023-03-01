package com.slesha.processms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;

import com.google.gson.Gson;
import com.slesha.processms.config.EnrollRequest;

@SpringBootApplication
public class ProcessmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcessmsApplication.class, args);
	}
	@KafkaListener(topics = "enroll", groupId = "group-id")
	public void enrollListen(String message) {
   		System.out.println("Received Messasge in enroll - group-id: " + message);
		Gson g = new Gson();
		System.out.println(g.fromJson(message, EnrollRequest.class));
	}

	@KafkaListener(topics = "login", groupId = "group-id")
	public void loginListen(String message) {
   		System.out.println("Received Messasge in login - group-id: " + message);
	}

	@KafkaListener(topics = "userplans", groupId = "group-id")
	public void getUserPlans(String message){
		System.out.println("Received Messasge in user-plans - group-id: " + message);

	}

}
