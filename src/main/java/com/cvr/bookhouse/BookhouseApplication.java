package com.cvr.bookhouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BookhouseApplication {

	public static void main(String[] args) {
        for (String arg : args) {
            if ("--mcp".equalsIgnoreCase(arg)) {
                System.out.println("✨ MCP/LLM features coming soon... stay tuned! ✨");
                break;
            }
        }
		SpringApplication.run(BookhouseApplication.class, args);
	}

}
