import com.arangodb.*;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.model.EdgeCreateOptions;
import com.arangodb.model.EdgeDeleteOptions;
import com.arangodb.model.VertexCreateOptions;
import com.arangodb.model.VertexDeleteOptions;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArangoMain {

    public static void main(String[] args) {
        java.util.logging.Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);
        Scanner scanner = new Scanner(System.in);
        ArangoDB arangoDB = new ArangoDB.Builder().user("piotr").password("piotr").build();
        ArangoDatabase arangoDatabase = arangoDB.db("serwisSamochodowy");
        ArangoGraph graph = arangoDatabase.graph("serwisSamochodowyGraph");
        do {
            System.out.println("|=================> System zarządzania serwisem samochodowym <=================|");
            System.out.println("===> 1.Dodaj zlecenie");
            System.out.println("===> 2.Wyświetl wszystkie zlecenia");
            System.out.println("===> 3.Usuń zlecenie");
            System.out.println("===> 4.Edytuj zlecenie");
            System.out.println("===> 5.Szukaj zlecenia po id");
            System.out.println("===> 6.Wyświetlanie zleceń od wybranej ceny");
            System.out.println("===> 7.Wyjście");
            System.out.println("Wybór:");
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    createVertexes(scanner, graph);
                    break;
                case 2:
                    printAll(arangoDB, graph);
                    break;
                case 3:
                    delete(scanner, arangoDB, graph);
                    break;
                case 4:
                    edit(scanner, arangoDB);
                    break;
                case 5:
                    findByKey(arangoDB, graph, scanner);
                    break;
                case 6:
                    findOrderByPrice(arangoDB, graph, scanner);
                    break;
                case 7:
                    System.exit(0);
                    break;
            }
        } while (true);



    }

    private static void createVertexes(Scanner scanner, ArangoGraph graph){
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
        System.out.println("Numer rejestracyjny");
        String carNumberPlate = scanner.nextLine();
        System.out.println("Status zlecenia");
        String status = scanner.nextLine();
        System.out.println("Wymienione części w samochodzie");
        String changedItems = scanner.nextLine();
        System.out.println("Cena naprawy");
        double price = scanner.nextDouble();
        scanner.nextLine();
        String keyZlecenie = String.valueOf(UUID.randomUUID());
        String keySamochod = String.valueOf(UUID.randomUUID());

        ArangoVertexCollection collectionZlecenie = graph.vertexCollection("Zlecenie");
        BaseDocument documentZlecenie = new BaseDocument();
        documentZlecenie.setKey(keyZlecenie);
        documentZlecenie.addAttribute("nazwa", orderName);
        documentZlecenie.addAttribute("imieKlienta", clientFirstName);
        documentZlecenie.addAttribute("nazwiskoKlienta", clientLastName);
        documentZlecenie.addAttribute("statusZlecenia", status);
        documentZlecenie.addAttribute("listaCzesci", changedItems);
        documentZlecenie.addAttribute("cenaZlecenia", price);

        collectionZlecenie.insertVertex(documentZlecenie, new VertexCreateOptions());

        ArangoVertexCollection collectionSamochod = graph.vertexCollection("Samochod");
        BaseDocument documentSamochod = new BaseDocument();
        documentSamochod.setKey(keySamochod);
        documentSamochod.addAttribute("nazwa", carBrandName);
        documentSamochod.addAttribute("modelSamochodu", carModelName);
        documentSamochod.addAttribute("numerRejestracyjny", carNumberPlate);
        collectionSamochod.insertVertex(documentSamochod, new VertexCreateOptions());

        ArangoEdgeCollection collection = graph.edgeCollection("edges");
        BaseEdgeDocument document = new BaseEdgeDocument("Zlecenie/"+keyZlecenie, "Samochod/"+keySamochod);
        collection.insertEdge(document, new EdgeCreateOptions());
    }

    private static void printAll(ArangoDB arangoDB, ArangoGraph graph){
        ArangoVertexCollection arangoVertexCollectionZlecenia = graph.vertexCollection("Zlecenie");
        ArangoVertexCollection arangoVertexCollectionSamochod = graph.vertexCollection("Samochod");
        Map<String, String> map = new HashMap<>();
        String queryReadZlecenie = "FOR t IN edges RETURN t";
        ArangoCursor<BaseDocument> cursor = arangoDB.db("serwisSamochodowy").query(queryReadZlecenie, null, null, BaseDocument.class);
        printFields(arangoVertexCollectionZlecenia, arangoVertexCollectionSamochod, map, cursor);
    }

    private static void printFields(ArangoVertexCollection arangoVertexCollectionZlecenia, ArangoVertexCollection arangoVertexCollectionSamochod, Map<String, String> map, ArangoCursor<BaseDocument> cursor) {
        cursor.forEachRemaining(edges -> map.put(String.valueOf(edges.getProperties().get("_from")).replace("Zlecenie/", ""), String.valueOf(edges.getProperties().get("_to")).replace("Samochod/", "")));
        for (Map.Entry<String, String > entry : map.entrySet()) {
            BaseDocument baseDocumentZlecenie = arangoVertexCollectionZlecenia.getVertex(entry.getKey(), BaseDocument.class);
            BaseDocument baseDocumentSamochod = arangoVertexCollectionSamochod.getVertex(entry.getValue(), BaseDocument.class);
            System.out.println("Klucz zlecania:   "+entry.getKey());
            System.out.println("Nazwa zlecenia:   "+baseDocumentZlecenie.getProperties().get("nazwa"));
            System.out.println("Imię klienta:   "+baseDocumentZlecenie.getProperties().get("imieKlienta"));
            System.out.println("Nazwisko klienta:   "+baseDocumentZlecenie.getProperties().get("nazwiskoKlienta"));
            System.out.println("Status:   "+baseDocumentZlecenie.getProperties().get("statusZlecenia"));
            System.out.println("Lista części:   "+baseDocumentZlecenie.getProperties().get("listaCzesci"));
            System.out.println("Cena:   "+baseDocumentZlecenie.getProperties().get("cenaZlecenia"));
            System.out.println("Klucz samochodu:   "+entry.getValue());
            System.out.println("Model pojazdu:   "+baseDocumentSamochod.getProperties().get("nazwa"));
            System.out.println("Model samochodu:   "+baseDocumentSamochod.getProperties().get("modelSamochodu"));
            System.out.println("Numer rejestracyjny:   "+baseDocumentSamochod.getProperties().get("numerRejestracyjny"));
            System.out.println();
        }
    }

    private static void delete(Scanner scanner, ArangoDB arangoDB, ArangoGraph graph){
        System.out.println("Podaj klucz zlecenia:");
        String keyZlecenie = scanner.nextLine();
        Map<String, String> mapEdge= new HashMap<>();
        String queryZlecenie = "FOR t IN edges FILTER t._from == 'Zlecenie/"+ keyZlecenie +"' RETURN t";
        ArangoCursor<BaseDocument> cursor = arangoDB.db("serwisSamochodowy").query(queryZlecenie, null, null, BaseDocument.class);
        cursor.forEachRemaining(edges -> mapEdge.put(String.valueOf(edges.getKey()), String.valueOf(edges.getProperties().get("_to")).replace("Samochod/", "")));
        ArangoEdgeCollection collectionEdge = graph.edgeCollection("edges");
        collectionEdge.deleteEdge(mapEdge.keySet().toString().replace("[", "").replace("]", ""), new EdgeDeleteOptions());
        ArangoVertexCollection collectionSamochod = graph.vertexCollection("Samochod");
        collectionSamochod.deleteVertex(mapEdge.values().toString().replace("[", "").replace("]", ""));
        ArangoVertexCollection collectionOrder = graph.vertexCollection("Zlecenie");
        collectionOrder.deleteVertex(keyZlecenie, new VertexDeleteOptions());
    }

    private static void edit(Scanner scanner, ArangoDB arangoDB){
        String key;
        BaseDocument document = new BaseDocument();
        System.out.println("Które pole chcesz edytować?");
        System.out.println("===> 1.Nazwe zlecenia");
        System.out.println("===> 2.Imię klienta");
        System.out.println("===> 3.Nazwisko klienta");
        System.out.println("===> 4.Status zlecenia");
        System.out.println("===> 5.Wymienione części w samochodzie");
        System.out.println("===> 6.Cena naprawy");
        System.out.println("===> 7.Marka samochodu");
        System.out.println("===> 8.Model samochodu");
        System.out.println("===> 9.Numer rejestracyjny");
        System.out.println("Wybór");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch (choice) {
            case 1:
                System.out.println("Podaj klucz zlecenia które chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nową nazwę zlecenia:");
                String orderName = scanner.nextLine();
                document.addAttribute("nazwa", orderName);
                arangoDB.db("serwisSamochodowy").collection("Zlecenie").updateDocument(key, document);
                break;
            case 2:
                System.out.println("Podaj klucz zlecenia które chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nowe imię klienta:");
                String clientFirstName = scanner.nextLine();
                document.addAttribute("imieKlienta", clientFirstName);
                arangoDB.db("serwisSamochodowy").collection("Zlecenie").updateDocument(key, document);
                break;
            case 3:
                System.out.println("Podaj klucz zlecenia które chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nowe nazwisko klienta:");
                String clientLastName = scanner.nextLine();
                document.addAttribute("nazwiskoKlienta", clientLastName);
                arangoDB.db("serwisSamochodowy").collection("Zlecenie").updateDocument(key, document);
                break;
            case 4:
                System.out.println("Podaj klucz zlecenia które chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nowy status zlecenia:");
                String status = scanner.nextLine();
                document.addAttribute("statusZlecenia", status);
                arangoDB.db("serwisSamochodowy").collection("Zlecenie").updateDocument(key, document);
                break;
            case 5:
                System.out.println("Podaj klucz zlecenia które chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nowe wymienione części:");
                String changedItems = scanner.nextLine();
                document.addAttribute("listaCzesci", changedItems);
                arangoDB.db("serwisSamochodowy").collection("Zlecenie").updateDocument(key, document);
                break;
            case 6:
                System.out.println("Podaj klucz zlecenia które chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nową cenę:");
                double price = scanner.nextDouble();
                scanner.nextLine();
                document.addAttribute("cenaZlecenia", price);
                arangoDB.db("serwisSamochodowy").collection("Zlecenie").updateDocument(key, document);
                break;
            case 7:
                System.out.println("Podaj klucz samochodu który chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nową markę samochodu:");
                String carBrandName = scanner.nextLine();
                document.addAttribute("nazwa", carBrandName);
                arangoDB.db("serwisSamochodowy").collection("Samochod").updateDocument(key, document);
                break;
            case 8:
                System.out.println("Podaj klucz samochodu który chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nowy model samochodu:");
                String carModelName = scanner.nextLine();
                document.addAttribute("modelSamochodu", carModelName);
                arangoDB.db("serwisSamochodowy").collection("Samochod").updateDocument(key, document);
                break;
            case 9:
                System.out.println("Podaj klucz samochodu który chcesz edytować: ");
                key = scanner.nextLine();
                System.out.println("Wpisz nowe numery rejestracyjne:");
                String carNumberPlate = scanner.nextLine();
                document.addAttribute("nazwa", carNumberPlate);
                arangoDB.db("serwisSamochodowy").collection("Samochod").updateDocument(key, document);
                break;
        }
    }


    private static void findByKey(ArangoDB arangoDB, ArangoGraph graph, Scanner scanner){
        System.out.println("Podaj klucz zlecenia:");
        String keyZlecenie = scanner.nextLine();
        ArangoVertexCollection arangoVertexCollectionZlecenia = graph.vertexCollection("Zlecenie");
        ArangoVertexCollection arangoVertexCollectionSamochod = graph.vertexCollection("Samochod");
        Map<String, String> map = new HashMap<>();
        String queryReadZlecenie = "FOR t IN edges FILTER t._from == 'Zlecenie/"+ keyZlecenie +"' RETURN t";
        ArangoCursor<BaseDocument> cursor = arangoDB.db("serwisSamochodowy").query(queryReadZlecenie, null, null, BaseDocument.class);
        printFields(arangoVertexCollectionZlecenia, arangoVertexCollectionSamochod, map, cursor);
    }

    private static void findOrderByPrice(ArangoDB arangoDB, ArangoGraph graph, Scanner scanner) {
        System.out.println("Podaj cenę");
        double cena = scanner.nextDouble();
        scanner.nextLine();
        ArangoVertexCollection arangoVertexCollectionZlecenia = graph.vertexCollection("Zlecenie");
        List<String> list = new ArrayList<>();
        String queryReadZlecenie = "FOR z IN Zlecenie FILTER z.cenaZlecenia > "+ cena+" RETURN z";
        ArangoCursor<BaseDocument> cursor = arangoDB.db("serwisSamochodowy").query(queryReadZlecenie, null, null, BaseDocument.class);
        cursor.forEachRemaining(edges ->
                list.add(edges.getKey()));

        for(String tmp : list) {
            BaseDocument baseDocumentZlecenie = arangoVertexCollectionZlecenia.getVertex(tmp, BaseDocument.class);
            System.out.println("Klucz zlecania:   "+ baseDocumentZlecenie.getKey());
            System.out.println("Nazwa zlecenia:   "+baseDocumentZlecenie.getProperties().get("nazwa"));
            System.out.println("Imię klienta:   "+baseDocumentZlecenie.getProperties().get("imieKlienta"));
            System.out.println("Nazwisko klienta:   "+baseDocumentZlecenie.getProperties().get("nazwiskoKlienta"));
            System.out.println("Status:   "+baseDocumentZlecenie.getProperties().get("statusZlecenia"));
            System.out.println("Lista części:   "+baseDocumentZlecenie.getProperties().get("listaCzesci"));
            System.out.println("Cena:   "+baseDocumentZlecenie.getProperties().get("cenaZlecenia"));
            System.out.println();

        }


    }
}
