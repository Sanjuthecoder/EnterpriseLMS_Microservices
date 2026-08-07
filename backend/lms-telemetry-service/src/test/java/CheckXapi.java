import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.util.function.Consumer;

public class CheckXapi {
    public static void main(String[] args) {
        String uri = "mongodb+srv://sanjaysharmajr07_db_user:2qhwUbhjsWNykJK4@enterprisecluster.gtynvaw.mongodb.net/enterprise_db?appName=enterpriseCluster";
        
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("enterprise_db");
            MongoCollection<Document> collection = database.getCollection("svc_telemetry_xapi_statements");
            
            System.out.println("Documents count: " + collection.countDocuments());
            collection.find().forEach((Consumer<Document>) doc -> {
                System.out.println(doc.toJson());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
