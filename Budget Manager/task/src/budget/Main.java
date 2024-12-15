package budget;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static budget.Main.scanner;
import static budget.ProductTypes.*;

enum ProductTypes {

    FOOD("Food"),
    CLOTHES("Clothes"),
    ENTERTAINMENT("Entertainment"),
    OTHER("Other");

    private final String type;

    ProductTypes(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ProductTypes getByValue(String type) {
        for (ProductTypes productType : ProductTypes.values()) {
            if (productType.getType().equals(type)) {
                return productType;
            }
        }
        throw new IllegalArgumentException("No enum constant with value " + type);
    }
}


interface Action {
    void execute();
}

interface BalanceInterface {
    void add(double value);

    void subtract(double value);

    void show();
}

interface ProductInterface {

    double getPrice();

    void show(ProductTypes type);

    void showAll();
}

class Product implements ProductInterface {

    private final String name;
    private final Double price;
    private final ProductTypes type;

    public Product(String name, double price, ProductTypes productType) {
        this.name = name;
        this.price = price;
        this.type = productType;
    }

    @Override
    public double getPrice() {
        return price;
    }

    public ProductTypes getType() {
        return type;
    }

    @Override
    public void show(ProductTypes type) {
        if (type.equals(this.type)) {
            System.out.printf("%s $%.2f\n", name, price);
        }
    }

    @Override
    public void showAll() {
        System.out.printf("%s $%.2f\n", name, price);
    }

    @Override
    public String toString() {
        return type.getType() + ";" + name + ";" + price;
    }
}

class ProductGroup implements ProductInterface {

    public Balance balance;
    private final List<Product> items = new ArrayList<>();

    public ProductGroup() {
    }

    public ProductGroup(Balance balance) {
        this.balance = balance;
    }

    public void add(ProductTypes productType) {
        System.out.println("Enter purchase name:");
        scanner.nextLine();
        String name = scanner.nextLine().trim();
        System.out.println("Enter its price:");
        double price = scanner.nextDouble();
        balance.subtract(price);
        items.add(new Product(name, price, productType));
        System.out.println("Purchase was added!!");
    }

    public void add(ProductTypes productType, String name, double price) {
        items.add(new Product(name, price, productType));
    }

    @Override
    public double getPrice() {
        return items.stream().mapToDouble((Product::getPrice)).sum();
    }

    public double getPrice(ProductTypes productTypes) {
        return items.stream().filter(a -> a.getType().equals(productTypes)).mapToDouble(Product::getPrice).sum();
    }

    public double getCount(ProductTypes productTypes) {
        return items.stream().filter(a -> a.getType().equals(productTypes)).count();
    }

    @Override
    public void show(ProductTypes productTypes) {
        if (getCount(productTypes) == 0) {
            System.out.println("The purchase list is empty");
        } else {
            System.out.printf("%s:\n", productTypes.getType());
            items.forEach(a -> a.show(productTypes));
            System.out.println("Total sum: $" + printPrice(getPrice(productTypes)));
        }
    }

    @Override
    public void showAll() {
        if (items.isEmpty()) {
            System.out.println("The purchase list is empty!");
        } else {
            System.out.println("All:");
            items.forEach(Product::showAll);
            System.out.println("Total: $" + printPrice(getPrice()));
        }
    }

    public void showByType() {
        Map<String, Double> totalPriceByType = new HashMap<>();

        for (Product item : items) {
            totalPriceByType.merge(item.getType().getType(), item.getPrice(), Double::sum);
        }

        List<Map.Entry<String, Double>> sortedByTotalPrice = new ArrayList<>(totalPriceByType.entrySet());
        sortedByTotalPrice.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));


        for (ProductTypes value : ProductTypes.values()) {
            if (sortedByTotalPrice.stream().noneMatch(a -> a.getKey().equals(value.getType()))) {
                sortedByTotalPrice.add(new AbstractMap.SimpleEntry<>(value.getType(), 0.00));
            }
        }

        System.out.println("Types:");
        for (Map.Entry<String, Double> entry : sortedByTotalPrice) {
            System.out.println(entry.getKey() + " - $" + printPrice(entry.getValue()));
        }
        System.out.println("Total sum: $" + printPrice(getPrice()));
    }


    @Override
    public String toString() {
        return balance.getBalance() + "\n" + items.stream().map(Product::toString).collect(Collectors.joining("\n"));
    }

    public void sort() {
        items.sort(Comparator.comparing(Product::getPrice).reversed());
    }

    private String printPrice(double price) {
        if (price == 0) {
            return "0";
        } else {
            return String.valueOf(price);
        }
    }
}

class Balance implements BalanceInterface {
    private double balance = 0.0;

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public void add(double value) {
        balance += value;
    }

    @Override
    public void subtract(double value) {
        balance -= value;
    }

    @Override
    public void show() {
        System.out.printf("Balance: $%.2f\n", balance);
    }
}

class Menu {
    private final String name;
    private final LinkedHashMap<Integer, Action> actions = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, String> actionsList = new LinkedHashMap<>();

    public Menu(String name) {
        this.name = name;
    }

    public void addAction(int option, String description, Action action) {
        actions.put(option, action);
        actionsList.put(option, description);
    }

    public void show() {
        System.out.println(name);
        actionsList.forEach((option, description) -> System.out.printf("%d) %s\n", option, description));
    }

    public void execute(int option) {
        Action action = actions.get(option);
        if (action != null) {
            action.execute();
        } else {
            System.out.println("Invalid option.");
        }
    }

    public void run() {
        while (true) {
            show();
            int choice = new Scanner(System.in).nextInt();
            System.out.println();
            execute(choice);
            System.out.println();
        }
    }
}

class FileService {
//    private File file = new File("." + File.separator + "Budget Manager" + File.separator + "task" + File.separator + "purchases.txt");
    private final File file = new File("purchases.txt");

    public FileService() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void save(String data) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println(data);
            System.out.println("Purchases were saved!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> readAsList() {
        try {
            List<String> output = Files.readAllLines(Paths.get(file.getPath()));
            System.out.println("Purchases were loaded!");
            return output;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


class Main {
    public static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        Balance balance = new Balance();
        ProductGroup productGroup = new ProductGroup(balance);
        FileService fileService = new FileService();

        Menu mainMenu = new Menu("Choose your action:");

        mainMenu.addAction(1, "Add income", () -> {
            System.out.println("Enter income:");
            balance.add(scanner.nextDouble());
            System.out.println("Income was added!");
        });

        mainMenu.addAction(2, "Add purchase", () -> {
            Menu purchaseMenu = new Menu("Choose the type of purchase");
            purchaseMenu.addAction(1, FOOD.getType(), () -> productGroup.add(FOOD));
            purchaseMenu.addAction(2, CLOTHES.getType(), () -> productGroup.add(CLOTHES));
            purchaseMenu.addAction(3, ENTERTAINMENT.getType(), () -> productGroup.add(ENTERTAINMENT));
            purchaseMenu.addAction(4, OTHER.getType(), () -> productGroup.add(ProductTypes.OTHER));
            purchaseMenu.addAction(5, "Back", mainMenu::run);
            purchaseMenu.run();
        });

        mainMenu.addAction(3, "Show list of purchases", () -> {
            Menu purchaseMenu = new Menu("Choose the type of purchases");
            purchaseMenu.addAction(1, FOOD.getType(), () -> productGroup.show(FOOD));
            purchaseMenu.addAction(2, CLOTHES.getType(), () -> productGroup.show(CLOTHES));
            purchaseMenu.addAction(3, ENTERTAINMENT.getType(), () -> productGroup.show(ENTERTAINMENT));
            purchaseMenu.addAction(4, OTHER.getType(), () -> productGroup.show(OTHER));
            purchaseMenu.addAction(5, "All", productGroup::showAll);
            purchaseMenu.addAction(6, "Back", mainMenu::run);
            purchaseMenu.run();
        });

        mainMenu.addAction(4, "Balance", balance::show);

        mainMenu.addAction(5, "Save", () -> fileService.save(productGroup.toString()));

        mainMenu.addAction(6, "Load", () -> {
            List<String> output = fileService.readAsList();
            if (!output.isEmpty()) {
                balance.setBalance(Double.parseDouble(output.getFirst().trim()));
                output.removeFirst();

                output.stream()
                        .map(productsInString -> productsInString.split(";"))
                        .filter(products -> products.length == 3)
                        .forEach(product -> {
                            ProductTypes productType = getByValue(product[0]);
                            String productName = product[1];
                            double productPrice = Double.parseDouble(product[2]);
                            productGroup.add(productType, productName, productPrice);
                        });
            }
        });

        mainMenu.addAction(7, "Analyze (Sort)", () -> {
            productGroup.sort();
            Menu analyzerMenu = new Menu("How do you want to sort?");
            analyzerMenu.addAction(1, "Sort all purchases", productGroup::showAll);
            analyzerMenu.addAction(2, "Sort by type", productGroup::showByType);
            analyzerMenu.addAction(3, "Sort certain type", () -> {
                Menu analyzerMenuByTypeOfProduct = new Menu("Choose the type of purchase");
                analyzerMenuByTypeOfProduct.addAction(1, FOOD.getType(), () -> {
                    productGroup.show(FOOD);
                    System.out.println();
                    analyzerMenu.run();
                });
                analyzerMenuByTypeOfProduct.addAction(2, CLOTHES.getType(), () -> {
                    productGroup.show(CLOTHES);
                    System.out.println();
                    analyzerMenu.run();
                });
                analyzerMenuByTypeOfProduct.addAction(3, ENTERTAINMENT.getType(), () -> {
                    productGroup.show(ENTERTAINMENT);
                    System.out.println();
                    analyzerMenu.run();
                });
                analyzerMenuByTypeOfProduct.addAction(4, OTHER.getType(), () -> {
                    productGroup.show(OTHER);
                    System.out.println();
                    analyzerMenu.run();
                });
                analyzerMenuByTypeOfProduct.run();
            });
            analyzerMenu.addAction(4, "Back", mainMenu::run);
            analyzerMenu.run();
        });

        mainMenu.addAction(0, "Exit", () -> {
            System.out.println("Bye!");
            System.exit(0);
        });

        mainMenu.run();
    }
}