import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class Problem3 {

    static class MovieStats {
        String title;
        int count = 0;
        double sumRating = 0;
        MovieStats(String title) { this.title = title; }
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Table table = connection.getTable(TableName.valueOf("movielens_ratings"));

        Scan scan = new Scan();
        scan.addColumn(Bytes.toBytes("Rating"), Bytes.toBytes("score"));
        scan.addColumn(Bytes.toBytes("User"), Bytes.toBytes("age"));
        scan.addColumn(Bytes.toBytes("Movie"), Bytes.toBytes("movieId"));
        scan.addColumn(Bytes.toBytes("Movie"), Bytes.toBytes("title"));

        ResultScanner scanner = table.getScanner(scan);

        long totalRatings = 0;
        Map<Integer, Long> scoreCounts = new HashMap<>();
        long positiveRatings = 0;

        double sumUnder25 = 0, countUnder25 = 0;
        double sumOver45 = 0, countOver45 = 0;

        Map<String, MovieStats> movieMap = new HashMap<>();

        for (Result r : scanner) {
            String scoreStr = Bytes.toString(r.getValue(Bytes.toBytes("Rating"), Bytes.toBytes("score")));
            String ageStr = Bytes.toString(r.getValue(Bytes.toBytes("User"), Bytes.toBytes("age")));
            String movieId = Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("movieId")));
            String title = Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("title")));

            if (scoreStr == null) continue;
            int score = Integer.parseInt(scoreStr);
            totalRatings++;

            // 1. Most common rating score count
            scoreCounts.put(score, scoreCounts.getOrDefault(score, 0L) + 1);

            // 2. Positive ratings (Rating >= 4)
            if (score >= 4) {
                positiveRatings++;
            }

            // 3. User Group averages by age
            if (ageStr != null) {
                int age = Integer.parseInt(ageStr);
                if (age < 25) {
                    sumUnder25 += score;
                    countUnder25++;
                } else if (age >= 45) {
                    sumOver45 += score;
                    countOver45++;
                }
            }

            // 4. Movie aggregates
            if (movieId != null) {
                MovieStats ms = movieMap.computeIfAbsent(movieId, k -> new MovieStats(title));
                ms.count++;
                ms.sumRating += score;
            }
        }

        scanner.close();
        if (totalRatings == 0) throw new IllegalStateException("No ratings loaded");

        // Initialize PrintWriter to write to file
        PrintWriter out = new PrintWriter(new FileWriter("part_3_output.txt"));

        // Output Part 3.1: Most common score
        int mostCommonScore = -1;
        long maxScoreCount = 0;
        for (Map.Entry<Integer, Long> entry : scoreCounts.entrySet()) {
            if (entry.getValue() > maxScoreCount) {
                maxScoreCount = entry.getValue();
                mostCommonScore = entry.getKey();
            }
        }
        out.println("Part 3.1: Most Common Rating Score");
        out.println("Rating\tCount\tPercentage");
        double mostCommonPct = ((double) maxScoreCount / totalRatings) * 100;
        out.printf("%d\t%d\t%.2f%%\n\n", mostCommonScore, maxScoreCount, mostCommonPct);

        // Output Part 3.2: Positive ratings percentage
        out.println("Part 3.2: Positive Ratings (Rating >= 4)");
        double positivePct = ((double) positiveRatings / totalRatings) * 100;
        out.printf("Percentage of Positive Ratings: %.2f%%\n\n", positivePct);

        // Output Part 3.3: Average rating by age groups
        out.println("Part 3.3: Average Rating by Age Groups");
        out.println("Group\tAverage_Rating");
        out.printf("Age < 25\t%.2f\n", countUnder25 > 0 ? (sumUnder25 / countUnder25) : 0);
        out.printf("Age >= 45\t%.2f\n\n", countOver45 > 0 ? (sumOver45 / countOver45) : 0);

        // Output Part 3.4: Movies with ratings >= 200 and Avg >= 4.0
        out.println("Part 3.4: Top Movies (Ratings >= 200, Avg >= 4.0)");
        out.println("MovieID\tTitle\tNumber_of_Ratings\tAverage_Rating");

        List<Map.Entry<String, MovieStats>> topMovies = new ArrayList<>();
        for (Map.Entry<String, MovieStats> entry : movieMap.entrySet()) {
            MovieStats ms = entry.getValue();
            double avg = ms.sumRating / ms.count;
            if (ms.count >= 200 && avg >= 4.0) {
                topMovies.add(entry);
            }
        }

        // Sort descending by average rating
        topMovies.sort((a, b) -> {
            int order = Double.compare(b.getValue().sumRating / b.getValue().count,
                                       a.getValue().sumRating / a.getValue().count);
            if (order == 0) order = Integer.compare(b.getValue().count, a.getValue().count);
            if (order == 0) order = Integer.compare(Integer.parseInt(a.getKey()), Integer.parseInt(b.getKey()));
            return order;
        });

        for (Map.Entry<String, MovieStats> entry : topMovies) {
            MovieStats ms = entry.getValue();
            double avg = ms.sumRating / ms.count;
            out.printf("%s\t%s\t%d\t%.2f\n", entry.getKey(), ms.title, ms.count, avg);
        }

        out.close();
        table.close();
        connection.close();
        
        System.out.println("Processing complete. Check 'part_3_output.txt' for the results.");
    }
}