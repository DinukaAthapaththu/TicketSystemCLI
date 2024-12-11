package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class JavaCLI {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Scanner scanner = new Scanner(System.in);
    private Timer pollingTimer;

    public JavaCLI(){

    }

    public void start(){
        while (true) {
            System.out.println("\n==== Initial Configuration ====");
            int totalTickets = getIntInput("Enter Total Tickets: ");
            int ticketReleaseRate = getIntInput("Enter Ticket Release Rate: ");
            int customerRetrievalRate = getIntInput("Enter Customer Retrieval Rate: ");
            int maxQueueSize = getIntInput("Enter Maximum Queue Size: ");

            if (sendConfigRequest(totalTickets, ticketReleaseRate, customerRetrievalRate, maxQueueSize)) {
                mainMenu();
            } else {
                System.out.println("Configuration failed. Please try again.");
            }
        }
    }

    private void mainMenu() {
        while (true) {
            System.out.println("\n==== Main Menu ====");
            System.out.println("1. Change Config");
            System.out.println("2. Start System");
            System.out.print("Enter your choice: ");
            int choice = getIntInput("");

            switch (choice) {
                case 1 -> start();  // Reconfigure
                case 2 -> {
                    if (startSystem()) {
                        showSystemDashboard();
                        return;
                    }
                }
                default -> System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    private void showSystemDashboard() {
        System.out.println("\n==== System Dashboard ====");

        while (true) {
            System.out.println("\n==== Commands ====");
            System.out.println("1. Current Queue Size");
            System.out.println("2. Get Vendors");
            System.out.println("3. Get Customers");
            System.out.println("4. Stop System");
            System.out.println("5. Add Customer");
            System.out.println("6. Add Vendor");
            System.out.println("7. Remove Customer");
            System.out.println("8. Remove Vendor");
            System.out.print("Enter your choice: ");
            int choice = getIntInput("");

//            switch (choice) {
//                case 1 -> stopSystem();
//                case 2 -> addEntity("customer");
//                case 3 -> addEntity("vendor");
//                case 4 -> removeEntity("customer");
//                case 5 -> removeEntity("vendor");
//                default -> System.out.println("Invalid choice! Please try again.");
//            }
        }
    }

    private void startPollingQueueSize() {
        pollingTimer = new Timer(true);
        pollingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String queueSize = sendGetRequest("http://localhost:8080/api/v1/system/getQueueSize");
                System.out.print("\rCurrent Queue Size: " + queueSize);
            }
        }, 0, 1000);
    }

    private String sendGetRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return extractData(response.body());
            } else {
                System.out.println(extractErrors(response.body()));
            }
        } catch (Exception e) {
            System.out.println("Error fetching data: " + e.getMessage());
        }
        return "Unavailable";
    }

    private String extractData(String responseBody) {
        int start = responseBody.indexOf("\"data\":\"") + 8;
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }

    private int getIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input. Please enter a number: ");
            scanner.next();
        }
        return scanner.nextInt();
    }

    private boolean sendConfigRequest(int totalTickets, int ticketReleaseRate, int customerRetrievalRate, int maxQueueSize) {
        String requestBody = String.format("""
                {
                    "totalTickets": %d,
                    "ticketReleaseRate": %d,
                    "customerRetrivalRate": %d,
                    "maxTicketCapacity": %d
                }
                """, totalTickets, ticketReleaseRate, customerRetrievalRate, maxQueueSize);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/v1/config/save"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Configuration saved successfully.");
                return true;
            } else {
                System.out.println("Failed to save configuration: " + response.body());
            }
        } catch (Exception e) {
            System.out.println("Error sending configuration request: " + e.getMessage());
        }
        return false;
    }

    private boolean startSystem() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/v1/system/start"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody.contains("\"hasError\":true")) {
                    System.out.println("System start failed: " + extractErrors(responseBody));
                    return false;
                } else {
                    System.out.println("System started successfully!");
                    return true;
                }
            } else {
                System.out.println("Failed to start system: " + response.body());
            }
        } catch (Exception e) {
            System.out.println("Error sending start system request: " + e.getMessage());
        }
        return false;
    }

    private String extractErrors(String responseBody) {
        int start = responseBody.indexOf("\"errors\":[") + 10;
        int end = responseBody.indexOf("]", start);
        return responseBody.substring(start, end).replace("\"", "");
    }
}


