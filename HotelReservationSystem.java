import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Represents a single hotel room. Implements Serializable for saving state to a file.
 */
class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    private int roomNumber;
    private String type; // e.g., "Single", "Double", "Suite"
    private double price;
    private boolean isAvailable;

    public Room(int roomNumber, String type, double price) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.price = price;
        this.isAvailable = true;
    }

    // Getters and Setters
    public int getRoomNumber() { return roomNumber; }
    public String getType() { return type; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    /**
     * Formats room details for display, including its category.
     */
    @Override
    public String toString() {
        return String.format("Room %d | Category: %s | Price: $%.2f/night | Status: %s",
                roomNumber,
                type,
                price,
                isAvailable ? "AVAILABLE" : "OCCUPIED");
    }
}

/**
 * Represents a customer reservation. Implements Serializable for saving state to a file.
 */
class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;
    private int reservationId;
    private Room room;
    private String guestName;
    private String checkInDate;
    private String checkOutDate;
    private double totalCost;

    public Reservation(int reservationId, Room room, String guestName, String checkInDate, String checkOutDate, double totalCost) {
        this.reservationId = reservationId;
        this.room = room;
        this.guestName = guestName;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalCost = totalCost;
    }

    // Getters
    public int getReservationId() { return reservationId; }
    public Room getRoom() { return room; }
    public String getGuestName() { return guestName; }
    public double getTotalCost() { return totalCost; }

    /**
     * Formats detailed reservation information for viewing.
     */
    @Override
    public String toString() {
        return String.format(
            "| ID: %d | Guest: %s | Room: %d (%s) | Dates: %s to %s | Total Paid: $%.2f",
            reservationId,
            guestName,
            room.getRoomNumber(),
            room.getType(),
            checkInDate,
            checkOutDate,
            totalCost
        );
    }
}

/**
 * Main class for managing the hotel reservation operations, including File I/O persistence.
 */
public class HotelReservationSystem {
    private static final String ROOMS_FILE = "rooms.ser";
    private static final String RESERVATIONS_FILE = "reservations.ser";
    private static final String ID_FILE = "next_id.ser";

    private List<Room> rooms;
    private Map<Integer, Reservation> reservations;
    private int nextReservationId = 1001;

    public HotelReservationSystem() {
        this.rooms = new ArrayList<>();
        this.reservations = new HashMap<>();
        // Attempt to load data on startup
        if (!loadData()) {
            System.out.println("No saved data found. Initializing default rooms...");
            initializeRooms();
        }
    }

    /**
     * Sets up the initial default list of rooms if no saved data is found.
     */
    private void initializeRooms() {
        rooms.add(new Room(101, "Single", 79.99));
        rooms.add(new Room(102, "Double", 99.99));
        rooms.add(new Room(103, "Double", 99.99));
        rooms.add(new Room(201, "Deluxe Double", 129.99));
        rooms.add(new Room(202, "Suite", 199.99));
        rooms.add(new Room(301, "Single", 85.00));
        System.out.println("Default hotel layout (6 rooms) created.");
    }

    /**
     * Attempts to load rooms, reservations, and the next ID from serialized files.
     * @return true if data was loaded successfully, false otherwise.
     */
    @SuppressWarnings("unchecked")
    private boolean loadData() {
        try (
            ObjectInputStream roomIn = new ObjectInputStream(new FileInputStream(ROOMS_FILE));
            ObjectInputStream resIn = new ObjectInputStream(new FileInputStream(RESERVATIONS_FILE));
            ObjectInputStream idIn = new ObjectInputStream(new FileInputStream(ID_FILE))
        ) {
            rooms = (List<Room>) roomIn.readObject();
            reservations = (Map<Integer, Reservation>) resIn.readObject();
            nextReservationId = idIn.readInt();
            System.out.println("\n*** System state loaded successfully from files. ***");
            return true;
        } catch (IOException | ClassNotFoundException e) {
            // File not found is common on first run, others are actual errors
            if (!(e instanceof java.io.FileNotFoundException)) {
                System.err.println("Error loading data: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Saves the current state of rooms, reservations, and the next ID to serialized files.
     */
    private void saveData() {
        try (
            ObjectOutputStream roomOut = new ObjectOutputStream(new FileOutputStream(ROOMS_FILE));
            ObjectOutputStream resOut = new ObjectOutputStream(new FileOutputStream(RESERVATIONS_FILE));
            ObjectOutputStream idOut = new ObjectOutputStream(new FileOutputStream(ID_FILE))
        ) {
            roomOut.writeObject(rooms);
            resOut.writeObject(reservations);
            idOut.writeInt(nextReservationId);
            System.out.println("Data saved to disk successfully.");
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    /**
     * Displays all available rooms, categorized and filtered by type.
     */
    public void checkAvailability(String roomType) {
        // 1. Filter and sort by type
        Map<String, List<Room>> availableRoomsByType = rooms.stream()
                .filter(Room::isAvailable)
                .filter(room -> roomType.isEmpty() || room.getType().equalsIgnoreCase(roomType))
                .collect(Collectors.groupingBy(Room::getType, Collectors.toList()));

        if (availableRoomsByType.isEmpty()) {
            System.out.println("\n--- No rooms are currently available matching your criteria. ---");
            return;
        }

        System.out.println("\n=== AVAILABLE ROOMS BY CATEGORY ===");
        availableRoomsByType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    System.out.println("\n[ " + entry.getKey().toUpperCase() + " ] (" + entry.getValue().size() + " available)");
                    entry.getValue().forEach(System.out::println);
                });
        System.out.println("===================================");
    }

    /**
     * Finds a specific room by its number.
     * @param roomNumber The number of the room to find.
     * @return The Room object or null if not found.
     */
    private Room findRoom(int roomNumber) {
        return rooms.stream()
                .filter(room -> room.getRoomNumber() == roomNumber)
                .findFirst()
                .orElse(null);
    }

    /**
     * Safely reads an integer input from the user.
     * @param scanner The Scanner object.
     * @return The integer input, or -1 if invalid.
     */
    private int readIntInput(Scanner scanner) {
        try {
            int input = Integer.parseInt(scanner.nextLine().trim());
            return input;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return -1;
        }
    }

    /**
     * Simulates a payment for the booking.
     * @return true if payment is successful, false otherwise.
     */
    private boolean processPayment(Scanner scanner, double amount) {
        System.out.println("\n--- PAYMENT SIMULATION ---");
        System.out.printf("Total Due: $%.2f\n", amount);
        System.out.println("1. Simulate Successful Payment");
        System.out.println("2. Simulate Payment Failure (Cancel Booking)");
        System.out.print("Choose action (1 or 2): ");
        
        String choice = scanner.nextLine().trim();

        if ("1".equals(choice)) {
            System.out.println("Payment processed successfully. Booking confirmed.");
            return true;
        } else if ("2".equals(choice)) {
            System.out.println("Payment failed. Reservation cancelled.");
            return false;
        } else {
            System.out.println("Invalid choice. Defaulting to payment failure.");
            return false;
        }
    }

    /**
     * Books an available room for a guest.
     */
    public void bookRoom(Scanner scanner) {
        System.out.print("\nEnter desired Room Number: ");
        int roomNumber = readIntInput(scanner);
        if (roomNumber == -1) return;

        Room room = findRoom(roomNumber);

        if (room == null || !room.isAvailable()) {
            System.out.println("Error: Room " + roomNumber + (room == null ? " not found." : " is currently occupied."));
            return;
        }

        System.out.print("Enter Guest Name: ");
        String guestName = scanner.nextLine();

        System.out.print("Enter Check-in Date (YYYY-MM-DD): ");
        String checkInDate = scanner.nextLine();

        System.out.print("Enter Check-out Date (YYYY-MM-DD): ");
        String checkOutDate = scanner.nextLine();

        // Calculate a simulated total cost (e.g., assuming 3 nights for simplicity)
        double totalCost = room.getPrice() * 3; 
        System.out.printf("\nSimulated Total Cost (for 3 nights): $%.2f\n", totalCost);


        // --- Payment Simulation Step ---
        if (!processPayment(scanner, totalCost)) {
            return; // Exit booking process if payment fails
        }

        // 1. Mark room as unavailable
        room.setAvailable(false);

        // 2. Create and store reservation object
        Reservation newReservation = new Reservation(nextReservationId, room, guestName, checkInDate, checkOutDate, totalCost);
        reservations.put(nextReservationId, newReservation);
        
        System.out.println("\n*** BOOKING SUCCESS! ***");
        System.out.println("Reservation ID: " + nextReservationId);
        System.out.println("Booking confirmed for " + guestName + ".");
        System.out.println(newReservation);

        // 3. Increment ID and save state
        nextReservationId++;
        saveData();
    }

    /**
     * Cancels an existing reservation.
     */
    public void cancelReservation(Scanner scanner) {
        System.out.print("\nEnter Reservation ID to cancel: ");
        int id = readIntInput(scanner);
        if (id == -1) return;

        Reservation reservation = reservations.get(id);

        if (reservation != null) {
            // Remove from map and update room availability
            reservations.remove(id); 
            reservation.getRoom().setAvailable(true);

            System.out.println("\n*** CANCELLATION SUCCESSFUL ***");
            System.out.println("Reservation " + id + " for " + reservation.getGuestName() + " has been cancelled.");
            System.out.println("Room " + reservation.getRoom().getRoomNumber() + " is now available.");
            saveData(); // Save state after successful cancellation
        } else {
            System.out.println("\nError: Reservation ID " + id + " not found.");
        }
    }

    /**
     * Displays all current reservations details, sorted by ID.
     */
    public void displayAllReservations() {
        if (reservations.isEmpty()) {
            System.out.println("\n--- No active reservations found. ---");
            return;
        }
        System.out.println("\n=== ALL ACTIVE BOOKING DETAILS ===");
        reservations.values().stream()
            .sorted(Comparator.comparing(Reservation::getReservationId))
            .forEach(System.out::println);
        System.out.println("===================================");
    }

    /**
     * Displays the main menu and handles user input loop.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=============================================");
            System.out.println("|      HOTEL MANAGEMENT SYSTEM (V2.0)       |");
            System.out.println("|  Data Persistence: ACTIVE (using File I/O)  |");
            System.out.println("=============================================");
            System.out.println("1. Search & Check Room Availability");
            System.out.println("2. Make a Reservation (Includes Payment)");
            System.out.println("3. Cancel a Reservation");
            System.out.println("4. View All Booking Details");
            System.out.println("5. Exit System");
            System.out.println("---------------------------------------------");
            System.out.print("Enter your choice (1-5): ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        System.out.print("Enter room type to filter (e.g., Single, Suite) or press Enter for all: ");
                        String type = scanner.nextLine().trim();
                        checkAvailability(type);
                        break;
                    case "2":
                        checkAvailability(""); // Show available rooms first
                        bookRoom(scanner);
                        break;
                    case "3":
                        displayAllReservations();
                        cancelReservation(scanner);
                        break;
                    case "4":
                        displayAllReservations();
                        break;
                    case "5":
                        running = false;
                        saveData(); // Final save before exiting
                        System.out.println("\nSystem shutting down. Data saved. Goodbye!");
                        break;
                    default:
                        System.out.println("\nInvalid choice. Please enter a number between 1 and 5.");
                }
            } catch (Exception e) {
                System.err.println("An unexpected error occurred during operation: " + e.getMessage());
            }
        }
        scanner.close();
    }

    /**
     * Main method to start the application.
     */
    public static void main(String[] args) {
        HotelReservationSystem system = new HotelReservationSystem();
        system.run();
    }
}
