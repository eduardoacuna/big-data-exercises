package nearsoft.academy.bigdata.recommendation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;


/**
 * Created by eduardo on 30/03/17.
 */
class MovieRecommender {

    private BiMap<String, Integer> products;
    private BiMap<String, Integer> users;

    private int totalProducts;
    private int totalUsers;
    private int totalReviews;
    private UserBasedRecommender recommender;

    MovieRecommender(String path) {

        totalProducts = 0;
        totalUsers = 0;
        totalReviews = 0;
        products = HashBiMap.create();
        users  = HashBiMap.create();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            PrintWriter pr = new PrintWriter("data.out", "UTF-8");
            for (String line1 = null, line2 = null, line3 = null;
                 (line1 = br.readLine()) != null && (line2 = br.readLine()) != null && (line3 = br.readLine()) != null;) {
                line1 = line1.split(": ")[1];
                line2 = line2.split(": ")[1];
                line3 = line3.split(": ")[1];
                if (!products.containsKey(line1)) {
                    totalProducts++;
                    products.put(line1, totalProducts);
                }
                if (!users.containsKey(line2)) {
                    totalUsers++;
                    users.put(line2, totalUsers);
                }
                totalReviews++;
                pr.printf("%s,%s,%s\n", users.get(line2), products.get(line1), line3);
            }
            pr.close();

            File outputFile = new File("data.out");
            assert outputFile.exists();
            DataModel model = new FileDataModel(outputFile);
            UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);

            recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

        } catch (IOException|TasteException e) {
            System.err.println(e);
            System.exit(-1);
        }
    }

    int getTotalReviews() {
        return totalReviews;
    }

    int getTotalProducts() {
        return totalProducts;
    }

    int getTotalUsers() {
        return totalUsers;
    }

    List<String> getRecommendationsForUser(String userId) {
        List<String> recommendationIds = new ArrayList<String>();
        try {
            Integer numericUserId = users.get(userId);
            assert numericUserId != null;
            List<RecommendedItem> recommendations = recommender.recommend(numericUserId.longValue(), 20);
            BiMap<Integer,String> inverseProducts = products.inverse();

            for (RecommendedItem r : recommendations) {
                assert r != null;
                String pid = inverseProducts.get((int) r.getItemID());
                assert pid != null;
                recommendationIds.add(pid);
            }
            return recommendationIds;
        } catch (TasteException e) {
            System.err.println(e);
            System.exit(-1);
        }
        return recommendationIds;
    }

}
