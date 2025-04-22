package com.foodDelivery.userService;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(UserServiceApplication.class);
		app.setBanner(new Banner() {
			@Override
			public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
				out.println("=================================================");
				out.println("  _   _               ____                  _          ");
				out.println(" | | | |___  ___ _ __|  _ \\ ___  ___ _   _(_) ___ ___ ");
				out.println(" | | | / __|/ _ \\ '__| |_) / _ \\/ __| | | | |/ __/ _ \\");
				out.println(" | |_| \\__ \\  __/ |  |  _ <  __/\\__ \\ |_| | | (_|  __/");
				out.println("  \\___/|___/\\___|_|  |_| \\_\\___||___/\\__,_|_|\\___\\___|");
				out.println("                                                       ");
				out.println("  User Service - Food Delivery Platform");
				out.println("=================================================");
			}
		});
		app.run(args);
	}
}