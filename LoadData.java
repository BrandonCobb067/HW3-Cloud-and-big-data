import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

/** Joins MovieLens metadata and loads one complete HBase row per rating. */
public class LoadData {
    static Map<String, String[]> metadata(Path path) throws IOException {
        Map<String, String[]> result = new HashMap<>();
        for (String line : Files.readAllLines(path, StandardCharsets.ISO_8859_1)) {
            String[] fields = line.split("::", -1);
            result.put(fields[0], fields);
        }
        return result;
    }
    static void column(Put put, String family, String key, String value) {
        put.addColumn(Bytes.toBytes(family), Bytes.toBytes(key), Bytes.toBytes(value));
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: LoadData dataset-directory");
        Path dir = Paths.get(args[0]);
        Map<String, String[]> users = metadata(dir.resolve("users.dat"));
        Map<String, String[]> movies = metadata(dir.resolve("movies.dat"));
        try (Connection conn = ConnectionFactory.createConnection(HBaseConfiguration.create());
             BufferedMutator writer = conn.getBufferedMutator(TableName.valueOf("movielens_ratings"));
             BufferedReader input = Files.newBufferedReader(dir.resolve("ratings.dat"), StandardCharsets.ISO_8859_1)) {
            long count = 0;
            String line;
            while ((line = input.readLine()) != null) {
                String[] r = line.split("::", -1);
                String[] u = users.get(r[0]), m = movies.get(r[1]);
                if (u == null || m == null) throw new IllegalArgumentException("Missing metadata: " + line);
                Put put = new Put(Bytes.toBytes(r[1] + "_" + r[0]));
                String[] uk = {"userId", "gender", "age", "occupation", "zipCode"};
                String[] mk = {"movieId", "title", "genres"};
                for (int i = 0; i < uk.length; i++) column(put, "User", uk[i], u[i]);
                for (int i = 0; i < mk.length; i++) column(put, "Movie", mk[i], m[i]);
                column(put, "Rating", "score", r[2]);
                column(put, "Rating", "timestamp", r[3]);
                column(put, "RatingMetrics", "genreCount", Integer.toString(m[2].split("\\|").length));
                column(put, "RatingMetrics", "titleLength", Integer.toString(m[1].length()));
                writer.mutate(put);
                if (++count % 1000 == 0) writer.flush();
            }
            writer.flush();
            System.out.println("Loaded ratings: " + count);
        }
    }
}
