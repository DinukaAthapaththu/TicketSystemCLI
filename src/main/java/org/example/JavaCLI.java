package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class JavaCLI {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Scanner scanner = new Scanner(System.in);

    public JavaCLI() {

    }

    public void start() {
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
            System.out.println("3. Stop System");
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
                case 3 -> stopSystem();
                default -> System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    public void stopSystem() {
        String response = sendPostRequest("http://localhost:8080/api/v1/system/stop", false);  // Assuming true is for authorization or special header
        System.out.println(response);
        backToMainMenu();
    }

    private void showSystemDashboard() {
        System.out.println("\n==== System Dashboard ====");

        while (true) {
            System.out.println("\n==== Commands ====");
            System.out.println("1. Current Queue Size");
            System.out.println("2. Get Vendors");
            System.out.println("3. Get Customers");
            System.out.println("4. Add Vendor");
            System.out.println("5. Add Customer");
            System.out.println("6. Remove Vendor");
            System.out.println("7. Remove Customer");
            System.out.println("8. Back to Main Menu");
            System.out.print("Enter your choice: ");
            int choice = getIntInput("");

            switch (choice) {
                case 1 -> getCurrentQueueSize();
                case 2 -> getVendors();
                case 3 -> getCustomers();
                case 4 -> addVendor();
                case 5 -> addCustomer();
                case 6 -> removeVendor();
                case 7 -> removeCustomer();
                case 8 -> backToMainMenu();
                default -> System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    private void backToMainMenu() {
        System.out.println("Returning to the main menu...");
        mainMenu();  // Go back to the main menu
    }

    private void removeVendor() {
        int vendorId = getIntInput("Enter ID: ");
        String response = sendPostRequest("http://localhost:8080/api/v1/vendor/remove?vendorId=" + vendorId, false);
        System.out.println(response);
    }

    private void removeCustomer() {
        int customerId = getIntInput("Enter ID: ");
        String response = sendPostRequest("http://localhost:8080/api/v1/customer/remove?customerId=" + customerId, false);
        System.out.println(response);
    }

    private void addCustomer() {
        int customerId = getIntInput("Enter ID: ");
        String response = sendPostRequest("http://localhost:8080/api/v1/customer/add?customerId=" + customerId, false);
        System.out.println(response);
    }

    private void addVendor() {
        int vendorId = getIntInput("Enter ID: ");
        String response = sendPostRequest("http://localhost:8080/api/v1/vendor/add?vendorId=" + vendorId, false);
        System.out.println(response);
    }

    private void getCurrentQueueSize() {
        String queueSize = sendGetRequest("http://localhost:8080/api/v1/system/getQueueSize", false);
        System.out.println("=====================");
        System.out.println("Current Queue Size: " + queueSize);
        System.out.println("=====================");
    }

    private void getVendors() {
        String vendorList = sendGetRequest("http://localhost:8080/api/v1/vendor/list", true);
        System.out.println("=====================");
        System.out.println("Vendor List: " + vendorList);
        System.out.println("=====================");
    }

    private void getCustomers() {
        String customerList = sendGetRequest("http://localhost:8080/api/v1/customer/list", true);
        System.out.println("=====================");
        System.out.println("Customer List: " + customerList);
        System.out.println("=====================");
    }

    private String sendPostRequest(String url, boolean isResponseTypeArray) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (isResponseTypeArray) {
                    return extractDataFromList(response.body());
                }
                return extractData(response.body());
            } else {
                System.out.println(extractErrors(response.body()));
            }
        } catch (Exception e) {
            System.out.println("Error fetching data: " + e.getMessage());
        }
        return "Unavailable";
    }

    private String sendGetRequest(String url, boolean isResponseTypeArray) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (isResponseTypeArray) {
                    return extractDataFromList(response.body());
                }
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

    private String extractDataFromList(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray dataArray = jsonResponse.getJSONArray("data");

            if (dataArray.isEmpty()) {
                return "No data found.";
            }

            StringBuilder result = new StringBuilder("Data: ");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject obj = dataArray.getJSONObject(i);
                result.append(obj.toString()).append("\n");
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Invalid response format.";
        }
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


