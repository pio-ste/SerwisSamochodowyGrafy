import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.neo4j.driver.internal.types.InternalTypeSystem.TYPE_SYSTEM;

public class Main {

    public static void main(String[] args) throws Exception {
        java.util.logging.Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);
        Scanner scanner = new Scanner(System.in);
        try (Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "admin"));
            Session session = driver.session()) {
            do {
                System.out.println("|=================> System zarządzania serwisem samochodowym <=================|");
                System.out.println("===> 1.Dodaj zlecenie");
                System.out.println("===> 2.Wyświetl wszystkie zlecenia");
                System.out.println("===> 3.Usuń zlecenie");
                System.out.println("===> 4.Edytuj zlecenie");
                System.out.println("===> 5.Szukaj zlecenia po id");
                System.out.println("===> 6.Wyszukiwanie zlecenia po marce samochodu i minimalnym roczniku");
                System.out.println("Wybór:");
                int choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        System.out.println("Nazwa zlecenia");
                        String orderName = scanner.nextLine();
                        System.out.println("Imię klienta");
                        String clientFirstName = scanner.nextLine();
                        System.out.println("Nazwisko klienta");
                        String clientLastName = scanner.nextLine();
                        System.out.println("Marka samochodu");
                        String carBrandName = scanner.nextLine();
                        System.out.println("Model samochodu");
                        String carModelName = scanner.nextLine();
                        System.out.println("Rocznik samochodu");
                        int carYear = scanner.nextInt();
                        scanner.nextLine();
                        System.out.println("Numer rejestracyjny");
                        String carNumberPlate = scanner.nextLine();
                        System.out.println("Status zlecenia");
                        String status = scanner.nextLine();
                        System.out.println("Wymienione części w samochodzie");
                        String changedItems = scanner.nextLine();
                        System.out.println("Cena naprawy");
                        double price = scanner.nextDouble();
                        scanner.nextLine();
                        session.writeTransaction(tx -> createOrder(tx, orderName, clientFirstName, clientLastName, status, changedItems, price));
                        session.writeTransaction(tx -> createCar(tx, carBrandName, carModelName, carYear, carNumberPlate));
                        session.writeTransaction(tx -> createRelationship(tx, orderName, carNumberPlate));
                        break;
                    case 2:
                        session.writeTransaction(Main::readAllNodes);
                        break;
                    case 3:
                        session.writeTransaction(Main::readAllNodes);
                        System.out.println("Wpisz id zlecenia które ma być usunięte");
                        int id = scanner.nextInt();
                        scanner.nextLine();
                        session.writeTransaction(tx -> deleteById(tx, id));
                        break;
                    case 4:
                        session.writeTransaction(Main::readAllNodes);
                        session.writeTransaction(tx -> updateOrder(tx, scanner));
                        break;
                    case 5:
                        session.writeTransaction(Main::readAllNodes);
                        session.writeTransaction(tx -> selectOrderById(tx, scanner));
                        break;
                    case 6:
                        session.writeTransaction(tx -> selectOrderByCarBrandAndYear(tx, scanner));
                        break;
                    case 7:

                        break;
                }
            } while (true);
        }
    }


    public static Result createOrder(Transaction transaction, String orderName, String clientFirstName, String clientLastName, String status, String changedItems, double price) {
        String command = "CREATE (:Zlecenie {nazwaZlecenia:$orderName, imieKlienta:$clientFirstName, nazwiskoKlienta:$clientLastName, status:$status, wymienioneCzesci:$changedItems, cena:$price})";
        System.out.println("Executing: " + command);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("orderName", orderName);
        parameters.put("clientFirstName", clientFirstName);
        parameters.put("clientLastName", clientLastName);
        parameters.put("status", status);
        parameters.put("changedItems", changedItems);
        parameters.put("price", price);
        return transaction.run(command, parameters);
    }

    public static Result createCar(Transaction transaction, String carBrandName, String carModelName, int carYear, String carNumberPlate) {
        String command = "CREATE (:Samochod {numerRejestracyjny:$carNumberPlate, marka:$carBrandName, model:$carModelName, rocznik:$carYear})";
        System.out.println("Executing: " + command);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("carNumberPlate", carNumberPlate);
        parameters.put("carBrandName", carBrandName);
        parameters.put("carModelName", carModelName);
        parameters.put("carYear", carYear);
        return transaction.run(command, parameters);
    }

    public static Result createRelationship(Transaction transaction, String orderName, String carNumberPlate) {
        String command =
                "MATCH (s:Samochod),(z:Zlecenie) " +
                        "WHERE s.numerRejestracyjny = $carNumberPlate AND z.nazwaZlecenia = $orderName "
                        + "CREATE (s)−[r:JEST_W]−>(z)" +
                        "RETURN type(r)";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("carNumberPlate", carNumberPlate);
        parameters.put("orderName", orderName);
        System.out.println("Executing: " + command);
        return transaction.run(command, parameters);
    }

    public static void printNode(Node node) {
        System.out.println("labels = " + " : " + node.labels());
        System.out.println("id = " + node.id());
        System.out.println("Dane = " + node.asMap());
    }

    public static void printRelationship(Relationship relationship) {
        System.out.println("id = " + relationship.id());
        System.out.println("type = " + relationship.type());
        System.out.println("asMap = " + relationship.asMap());
    }

    public static void printField(Pair<String, Value> field) {
        Value value = field.value();
        if (TYPE_SYSTEM.NODE().isTypeOf(value))
            printNode(field.value().asNode());
        else if (TYPE_SYSTEM.RELATIONSHIP().isTypeOf(value))
            printRelationship(field.value().asRelationship());
        else
            throw new RuntimeException();
    }

    public static Result readAllNodes(Transaction transaction) {
        String command =
                "MATCH (n)" +
                        "RETURN n";
        return getResult(transaction, command);
    }

    private static Result getResult(Transaction transaction, String command) {
        Result result = transaction.run(command);
        while (result.hasNext()) {
            Record record = result.next();
            List<Pair<String, Value>> fields = record.fields();
            for (Pair<String, Value> field : fields)
                printField(field);
        }
        return result;
    }

    public static Result selectOrderById(Transaction transaction, Scanner scanner){
        System.out.println("Wpisz id zlecenia");
        int id = scanner.nextInt();
        scanner.nextLine();
        String command = "MATCH ((z:Zlecenie)-[r]-(s:Samochod)) where ID(z)="+id+" return s, r, z";
        System.out.println("Executing: " + command);
        return getResult(transaction, command);
    }

    public static Result selectOrderByCarBrandAndYear(Transaction transaction, Scanner scanner){
        System.out.println("Wpisz markę samochodu");
        String carBrand = scanner.nextLine();
        System.out.println("Wisz rocznik samochodu");
        int carYear = scanner.nextInt();
        scanner.nextLine();
        String command = "MATCH (z:Zlecenie)-[r]-(s:Samochod) where s.marka= '"+carBrand+"' AND s.rocznik>= "+carYear+" return s, r, z";
        System.out.println("Executing: " + command);
        return getResult(transaction, command);
    }

    public static Result deleteById(Transaction transaction, int id) {
        String command = "MATCH (z:Zlecenie) WHERE ID(z)=" + id +" OPTIONAL MATCH (z)-[r]-(s)DELETE r,z,s ";
        System.out.println("Executing: " + command);
        return transaction.run(command);
    }

    public static Result updateOrder(Transaction transaction, Scanner scanner) {
        String command;
        System.out.println("Wpisz id zlecenia które ma być edytowane");
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.println("===> 1.Nazwe zlecenia");
        System.out.println("===> 2.Imię klienta");
        System.out.println("===> 3.Nazwisko klienta");
        System.out.println("===> 4.Status zlecenia");
        System.out.println("===> 5.Wymienione części w samochodzie");
        System.out.println("===> 6.Cena naprawy");
        System.out.println("===> 7.Marka samochodu");
        System.out.println("===> 8.Model samochodu");
        System.out.println("===> 9.Rocznik samochodu");
        System.out.println("===> 10.Numer rejestracyjny");
        System.out.println("Wybór");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch (choice){
            case 1:
                System.out.println("Wpisz nową nazwę zlecenia:");
                String orderName = scanner.nextLine();
                command = "MATCH (z:Zlecenie) WHERE ID(z)=" + id +" SET z.nazwaZlecenia='"+ orderName +"' return z";
                System.out.println("Executing: " + command);

                break;
            case 2:
                System.out.println("Wpisz nowe imię klienta:");
                String clientFirstName = scanner.nextLine();
                command = "MATCH (z:Zlecenie) WHERE ID(z)=" + id +" SET z.imieKlienta='"+ clientFirstName +"' return z";
                System.out.println("Executing: " + command);
                break;
            case 3:
                System.out.println("Wpisz nowe nazwisko klienta:");
                String clientLastName = scanner.nextLine();
                command = "MATCH (z:Zlecenie) WHERE ID(z)=" + id +" SET z.nazwiskoKlienta='"+ clientLastName +"' return z";
                System.out.println("Executing: " + command);
                break;
            case 4:
                System.out.println("Wpisz nowy status zlecenia:");
                String status = scanner.nextLine();
                command = "MATCH (z:Zlecenie) WHERE ID(z)=" + id +" SET z.status='"+ status +"' return z";
                System.out.println("Executing: " + command);
                break;
            case 5:
                System.out.println("Wpisz nowe wymienione części:");
                String changedItems = scanner.nextLine();
                command = "MATCH (z:Zlecenie) WHERE ID(z)=" + id +" SET z.wymienioneCzesci='"+ changedItems +"' return z";
                System.out.println("Executing: " + command);
                break;
            case 6:
                System.out.println("Wpisz nową cenę:");
                double price = scanner.nextDouble();
                scanner.nextLine();
                command = "MATCH (z:Zlecenie) WHERE ID(z)=" + id +" SET z.cena='"+ price +"' return z";
                System.out.println("Executing: " + command);
                break;
            case 7:
                System.out.println("Wpisz nową markę samochodu:");
                String carBrandName = scanner.nextLine();
                command = "MATCH (z:Zlecenie)-[r]-(s:Samochod) WHERE ID(z) =" + id +" SET s.marka ="+carBrandName+" return r";
                break;
            case 8:
                System.out.println("Wpisz nowy model samochodu:");
                String carModelName = scanner.nextLine();
                command = "MATCH (z:Zlecenie)-[r]-(s:Samochod) WHERE ID(z)="+ id +" SET s.model ="+carModelName+" return r";
                break;
            case 9:
                System.out.println("Wpisz nowy rocznik samochodu:");
                int carYear = scanner.nextInt();
                command = "MATCH (z:Zlecenie)-[r]-(s:Samochod) WHERE ID(z) ="+ id +" SET s.rocznik ="+carYear+" return r";
                scanner.nextLine();
                break;
            case 10:
                System.out.println("Wpisz nowe numery rejestracyjne:");
                String carNumberPlate = scanner.nextLine();
                command = "MATCH (z:Zlecenie)-[r]-(s:Samochod) WHERE ID(z) = "+ id +" SET s.numerRejestracyjny ="+carNumberPlate+" return r";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + choice);
        }
        return transaction.run(command);
    }
}