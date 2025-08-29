package com.aman.bfhjavaqualifier;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@SpringBootApplication
public class BfhJavaQualifierApplication implements CommandLineRunner {

    private final RestTemplate restTemplate;

    public BfhJavaQualifierApplication(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(BfhJavaQualifierApplication.class, args);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void run(String... args) {
        try {
            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            Map<String, String> genBody = Map.of(
                    "name", "Aman Kumar",
                    "regNo", "22BCI0017",
                    "email", "kumaraman181003@gmail.com"
            );

            ResponseEntity<GenerateResponse> genResp = restTemplate.postForEntity(
                    generateUrl, genBody, GenerateResponse.class
            );

            if (!genResp.getStatusCode().is2xxSuccessful() || genResp.getBody() == null) {
                System.out.println("Failed to generate webhook/token. HTTP: " + genResp.getStatusCode());
                return;
            }

            String webhook = genResp.getBody().getWebhook();
            String accessToken = genResp.getBody().getAccessToken();
            System.out.println("Webhook: " + webhook);
            System.out.println("AccessToken: " + accessToken);

            String finalQuery = """
                    SELECT
                        p.amount AS SALARY,
                        CONCAT(e.first_name, ' ', e.last_name) AS NAME,
                        DATE_PART('year', AGE(CURRENT_DATE, e.dob)) AS AGE,
                        d.department_name AS DEPARTMENT_NAME
                    FROM payments p
                    JOIN employee e ON p.emp_id = e.emp_id
                    JOIN department d ON e.department = d.department_id
                    WHERE EXTRACT(DAY FROM p.payment_time) <> 1
                    ORDER BY p.amount DESC
                    LIMIT 1;
                    """;

            String submitUrl = (webhook != null && !webhook.isBlank())
                    ? webhook
                    : "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            Map<String, String> submitBody = Map.of("finalQuery", finalQuery);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(submitBody, headers);

            ResponseEntity<String> submitResp = restTemplate.postForEntity(submitUrl, request, String.class);
            System.out.println("Submission HTTP: " + submitResp.getStatusCode());
            System.out.println("Submission Body: " + submitResp.getBody());

        } catch (Exception e) {
            System.out.println("Error running qualifier flow: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class GenerateResponse {
        private String webhook;
        private String accessToken;

        public String getWebhook() { return webhook; }
        public void setWebhook(String webhook) { this.webhook = webhook; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    }
}
