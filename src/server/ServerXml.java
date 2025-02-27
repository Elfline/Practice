package server;

import org.w3c.dom.Node;
import utilities.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class ServerXml {
    private static final String BOOKS_FILE = "res/server/books.xml";
    private static final String TRANSACTIONS_FILE = "res/server/transactions.xml";
    private static final String FAVORITE_FILE = "res/server/favorites.xml";
    private static final String SALES_FILE = "res/server/sales.xml";
    private static final String ACCOUNTS_FILE = "res/server/accounts.xml";
    private static List<User> users = new ArrayList<>();
    private static AtomicInteger transactionCounter = new AtomicInteger(1);

    /** Helper methods in parsing xml */
    public static void appendChildElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(textContent));
        parent.appendChild(element);
    }

    /** Helper methods in parsing xml */
    public static Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    /** Helper methods in parsing xml */
    public static void saveXmlDocument(Document doc, File file) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }

    /** Load users from XML file */
    public static List<User> loadUsersFromXML() {
        File xmlFile = new File(ACCOUNTS_FILE);

        try {
            if (!xmlFile.exists()) {
                System.out.println("[SERVER] accounts.xml not found, creating a new one.");
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                Element rootElement = doc.createElement("users");
                doc.appendChild(rootElement);
                return users; // Return empty list if file doesn't exist
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("user");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node nNode = nodeList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String username = eElement.getElementsByTagName("username").item(0).getTextContent();
                    String password = eElement.getElementsByTagName("password").item(0).getTextContent();
                    String accountType = eElement.getElementsByTagName("accountType").item(0).getTextContent();
                    users.add(new User(username, password, accountType));
                }
            }
            System.out.println("[SERVER] Loaded " + users.size() + " users from accounts.xml");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[DEBUG] Error loading users from accounts.xml: " + e.getMessage());
        }
        return users;
    }

    /** Save users list to XML file */
    public static void saveUsersToXML(List<User> users) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.getDocumentElement().normalize();

        Element rootElement = doc.createElement("users");
        doc.appendChild(rootElement);

        for (User user : users) {
            Element userElement = doc.createElement("user");

            appendChildElement(doc, userElement, "Username", user.getUsername());
            appendChildElement(doc, userElement, "Password", user.getPassword());
            appendChildElement(doc, userElement, "AccountType", user.getAccountType());

            rootElement.appendChild(userElement);
        }

        saveXmlDocument(doc, new File(ACCOUNTS_FILE));
    }

    /** Load books from XML file */
    public static List<Book> loadBooks(File xmlFile) {
        List<Book> books = new ArrayList<>();
        try {
            if (!xmlFile.exists()) {
                return books; // Return an empty list if the file doesn't exist
            }
            Document doc = parseXml(new File(BOOKS_FILE));
            doc.getDocumentElement().normalize();
            NodeList bookNodes = doc.getElementsByTagName("book");

            for (int i = 0; i < bookNodes.getLength(); i++) {
                Element bookElement = (Element) bookNodes.item(i);
                String title = bookElement.getElementsByTagName("Title").item(0).getTextContent();
                String author = bookElement.getElementsByTagName("Author").item(0).getTextContent();
                String genre = bookElement.getElementsByTagName("Genre").item(0).getTextContent();
                int stock = Integer.parseInt(bookElement.getElementsByTagName("Stock").item(0).getTextContent());
                String year = bookElement.getElementsByTagName("Year").item(0).getTextContent();
                double price = Double.parseDouble(bookElement.getElementsByTagName("Price").item(0).getTextContent());

                books.add(new Book(title, author, genre, stock, year, price));
            }

            return books;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }

    /** Updating the Books from books.xml */
    public static void saveBooks(List<Book> books, File bookXML) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.getDocumentElement().normalize();

        Element rootElement = doc.createElement("books");
        doc.appendChild(rootElement);

        for (Book book : books) {
            Element bookElement = doc.createElement("book");

            appendChildElement(doc, bookElement, "Title", book.getTitle());
            appendChildElement(doc, bookElement, "Author", book.getAuthor());
            appendChildElement(doc, bookElement, "Genre", book.getGenre());
            appendChildElement(doc, bookElement, "Stock", String.valueOf(book.getStock()));
            appendChildElement(doc, bookElement, "Year", String.valueOf(book.getYear()));
            appendChildElement(doc, bookElement, "Price", String.format("2.f",book.getPrice()));

            rootElement.appendChild(bookElement);
        }

        saveXmlDocument(doc, bookXML);
    }

    /** Method for saving the cart to cart.xml */
    public static synchronized void saveCart(String username, File cart) throws Exception {
        System.out.println("[SERVER] Received cart XML for user: " + username);

        File bookXML = new File(BOOKS_FILE);
        List<Book> books = loadBooks(bookXML);

        Document cartDoc = parseXml(cart);
        NodeList bookNodes = cartDoc.getElementsByTagName("book");

        double total = 0;
        List<Transaction> transactions = new ArrayList<>();

        // Process each book in the cart
        for (int i = 0; i < bookNodes.getLength(); i++) {
            Element bookElement = (Element) bookNodes.item(i);
            String title = bookElement.getElementsByTagName("Title").item(0).getTextContent();
            int quantity = Integer.parseInt(bookElement.getElementsByTagName("Quantity").item(0).getTextContent());

            // Find the matching book in bookUtilities list
            Book matchingBook = findBookByTitle(books, title);

            if (matchingBook != null) {
                if (matchingBook.getStock() >= quantity) {
                    System.out.println("[Server] " + quantity + " book/s of '" + title + "' have been successfully bought by " + username);
                    matchingBook.setStock(matchingBook.getStock() - quantity); // Reduce stock

                    // Calculate total for this book
                    double bookTotal = matchingBook.getPrice() * quantity;
                    total += bookTotal;

                    // Create transaction object
                    String transactionId = generateUniqueTransactionId();
                    String currentDate = new SimpleDateFormat("MM-dd-yyyy").format(new Date());

                    transactions.add(new Transaction(username, currentDate, transactionId, title, quantity, matchingBook.getPrice(), bookTotal));
                } else {
                    System.out.println("[Server] Not enough stock for the book: " + title);
                }
            } else {
                System.out.println("[Server] Book not found in catalog: " + title);
            }
        }
        saveBooks(books, bookXML);
        saveTransactions(transactions);
    }

    /** Generate unique transactionId for every purchased */
    private static String generateUniqueTransactionId() {

        long timestamp = System.currentTimeMillis(); // Get the current timestamp (milliseconds since epoch)
        int transactionNumber = transactionCounter.getAndIncrement(); // Generate a unique transaction ID by combining timestamp and atomic counter and increment the counter
        return String.format("%d-%06d", timestamp, transactionNumber); // Format the transaction ID to ensure uniqueness
    }

    /**
     * method to find a book by title in the list of BookUtilities
     */
    public static Book findBookByTitle(List<Book> books, String title) {
        for (Book book : books) {
            if (book.getTitle().equalsIgnoreCase(title)) {
                return book;
            }
        }
        return null;  // Return null if book is not found
    }


    /** Method for saving the transactions to transactions.xml */
    public static void saveTransactions(List<Transaction> newTransactions) throws Exception {
        File file = new File(TRANSACTIONS_FILE);
        Document doc;

        if (file.exists() && file.length() > 0) {
            doc = parseXml(file);
        } else {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
            doc.getDocumentElement().normalize();
            Element root = doc.createElement("transactions");
            doc.appendChild(root);
        }

        Element root = doc.getDocumentElement();

        for (Transaction transaction : newTransactions) {
            Element userElement = doc.createElement("user");

            appendChildElement(doc, userElement, "username", transaction.getUsername());
            appendChildElement(doc, userElement, "date", transaction.getDate());
            appendChildElement(doc, userElement, "transactionId", transaction.getTransactionId());
            appendChildElement(doc, userElement, "bookTitle", transaction.getBookTitle());
            appendChildElement(doc, userElement, "quantity", String.valueOf(transaction.getQuantity()));
            appendChildElement(doc, userElement, "price", String.format("%.2f", transaction.getPrice()));
            appendChildElement(doc, userElement, "totalAmount", String.format("%.2f", transaction.getTotalAmount()));

            root.appendChild(userElement);
        }

        saveXmlDocument(doc, file);
    }

    /** Method for saving sales to sales.xml */
    public static void saveSales() {
        try {
            File transactionsFile = new File(TRANSACTIONS_FILE);
            if (!transactionsFile.exists()) {
                System.out.println("[DEBUG] Transactions.xml file not found");
                return;
            }

            // Parse transactions.xml using helper method
            Document transactionsDoc = parseXml(transactionsFile);
            transactionsDoc.getDocumentElement().normalize();

            // Data structure: Year -> Month -> Day -> List of Transactions
            Map<String, Map<String, Map<String, List<Element>>>> salesData = new TreeMap<>();
            Map<String, Double> monthlyRevenue = new HashMap<>();
            Map<String, Double> yearlyRevenue = new HashMap<>();

            NodeList nodeList = transactionsDoc.getElementsByTagName("user");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element transaction = (Element) nodeList.item(i);
                String date = transaction.getElementsByTagName("date").item(0).getTextContent();
                String year = date.substring(6, 10);
                String month = date.substring(0, 2);
                String day = date.substring(3, 5);
                String totalAmount = transaction.getElementsByTagName("totalAmount").item(0).getTextContent();

                // Organize transactions per year, month, and day
                salesData.computeIfAbsent(year, y -> new TreeMap<>())
                        .computeIfAbsent(month, m -> new TreeMap<>())
                        .computeIfAbsent(day, d -> new ArrayList<>())
                        .add(transaction);

                // Compute revenue per month and year
                String monthKey = year + "-" + month;
                String yearKey = year;
                double amount = Double.parseDouble(totalAmount);
                monthlyRevenue.put(monthKey, monthlyRevenue.getOrDefault(monthKey, 0.0) + amount);
                yearlyRevenue.put(yearKey, yearlyRevenue.getOrDefault(yearKey, 0.0) + amount);
            }

            // Generate sales.xml
            Document salesDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = salesDoc.createElement("sales");
            salesDoc.appendChild(root);

            for (String year : salesData.keySet()) {
                Element yearElement = salesDoc.createElement("year");
                yearElement.setAttribute("value", year);
                yearElement.setAttribute("totalRevenue", String.format("%.2f", yearlyRevenue.get(year)));
                root.appendChild(yearElement);

                for (String month : salesData.get(year).keySet()) {
                    Element monthElement = salesDoc.createElement("month");
                    monthElement.setAttribute("value", month);
                    monthElement.setAttribute("totalRevenue", String.format("%.2f", monthlyRevenue.get(year + "-" + month)));
                    yearElement.appendChild(monthElement);

                    for (String day : salesData.get(year).get(month).keySet()) {
                        Element dayElement = salesDoc.createElement("day");
                        dayElement.setAttribute("value", day);
                        monthElement.appendChild(dayElement);

                        for (Element transaction : salesData.get(year).get(month).get(day)) {
                            Element transactionElement = salesDoc.createElement("transaction");

                            // Copy relevant transaction details
                            String[] fields = {"transactionId", "bookTitle", "quantity", "price", "totalAmount"};
                            for (String field : fields) {
                                Element fieldElement = salesDoc.createElement(field);
                                fieldElement.setTextContent(transaction.getElementsByTagName(field).item(0).getTextContent());
                                transactionElement.appendChild(fieldElement);
                            }

                            dayElement.appendChild(transactionElement);
                        }
                    }
                }
            }

            saveXmlDocument(salesDoc, new File(SALES_FILE));
            System.out.println("[SERVER] sales.xml generated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
