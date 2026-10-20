import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Problem2 {

    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Table table = connection.getTable(TableName.valueOf("movielens_ratings"));

        // ----- Output File for 2.1 and 2.2 -----
        PrintWriter out2_1_2 = new PrintWriter(new FileWriter("part_2.1_and_2.2_output.txt"));

        out2_1_2.println("Part 2.1: Version Control");
        byte[] rowKey = Bytes.toBytes("1_1"); // Example RowKey (MovieID_UserID)
        
        // Update record twice
        Put p1 = new Put(rowKey);
        p1.addColumn(Bytes.toBytes("Rating"), Bytes.toBytes("score"), Bytes.toBytes("4"));
        table.put(p1);

        Put p2 = new Put(rowKey);
        p2.addColumn(Bytes.toBytes("Rating"), Bytes.toBytes("score"), Bytes.toBytes("5"));
        table.put(p2);

        // Retrieve versions
        Get get = new Get(rowKey);
        get.readVersions(2);
        Result res = table.get(get);
        for (Cell cell : res.getColumnCells(Bytes.toBytes("Rating"), Bytes.toBytes("score"))) {
            out2_1_2.println("Version Timestamp: " + cell.getTimestamp() + " | Value: " + Bytes.toString(CellUtil.cloneValue(cell)));
        }

        out2_1_2.println("\nPart 2.2: User with most ratings");
        Scan scanCount = new Scan();
        ResultScanner scannerCount = table.getScanner(scanCount);
        Map<String, Integer> userCounts = new HashMap<>();
        
        for (Result r : scannerCount) {
            String rk = Bytes.toString(r.getRow());
            String userId = rk.split("_")[1]; // RowKey is MovieID_UserID
            userCounts.put(userId, userCounts.getOrDefault(userId, 0) + 1);
        }
        
        String topUser = "";
        int maxRatings = 0;
        for (Map.Entry<String, Integer> entry : userCounts.entrySet()) {
            if (entry.getValue() > maxRatings) {
                maxRatings = entry.getValue();
                topUser = entry.getKey();
            }
        }
        out2_1_2.println("UserID\tNumber_of_Ratings");
        out2_1_2.println(topUser + "\t" + maxRatings);
        out2_1_2.close(); // Close first output file


        // ----- Part 2.3 Filtering -----

        // 1. Movie title contains Star or Toy
        PrintWriter out2_3_1 = new PrintWriter(new FileWriter("part_2.3.1_output.txt"));
        out2_3_1.println("Movies with Star or Toy:");
        Scan scan1 = new Scan();
        scan1.addColumn(Bytes.toBytes("Movie"), Bytes.toBytes("title"));
        scan1.setFilter(new SingleColumnValueFilter(Bytes.toBytes("Movie"), Bytes.toBytes("title"), CompareOperator.EQUAL, new RegexStringComparator("Star|Toy")));
        for (Result r : table.getScanner(scan1)) {
            out2_3_1.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("title"))));
        }
        out2_3_1.close();

        // 2. Movie belongs to both Comedy and Drama
        PrintWriter out2_3_2 = new PrintWriter(new FileWriter("part_2.3.2_output.txt"));
        out2_3_2.println("Movies with Comedy and Drama:");
        FilterList genreFilter = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        genreFilter.addFilter(new SingleColumnValueFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new SubstringComparator("Comedy")));
        genreFilter.addFilter(new SingleColumnValueFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new SubstringComparator("Drama")));
        Scan scan2 = new Scan();
        scan2.addColumn(Bytes.toBytes("Movie"), Bytes.toBytes("genres"));
        scan2.setFilter(genreFilter);
        for (Result r : table.getScanner(scan2)) {
            out2_3_2.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("genres"))));
        }
        out2_3_2.close();

        // 3. Movie belongs to more than two genres
        PrintWriter out2_3_3 = new PrintWriter(new FileWriter("part_2.3.3_output.txt"));
        out2_3_3.println("Movies with > 2 genres:");
        Scan scan3 = new Scan();
        scan3.addColumn(Bytes.toBytes("Movie"), Bytes.toBytes("genres"));
        scan3.setFilter(new SingleColumnValueFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new RegexStringComparator(".*\\|.*\\|.*")));
        for (Result r : table.getScanner(scan3)) {
             out2_3_3.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("genres"))));
        }
        out2_3_3.close();

        // 4. User ZIP code begins with 9
        PrintWriter out2_3_4 = new PrintWriter(new FileWriter("part_2.3.4_output.txt"));
        out2_3_4.println("ZIP starts with 9:");
        Scan scan4 = new Scan();
        scan4.addColumn(Bytes.toBytes("User"), Bytes.toBytes("zipCode"));
        scan4.setFilter(new SingleColumnValueFilter(Bytes.toBytes("User"), Bytes.toBytes("zipCode"), CompareOperator.EQUAL, new BinaryPrefixComparator(Bytes.toBytes("9"))));
        for (Result r : table.getScanner(scan4)) {
             out2_3_4.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("User"), Bytes.toBytes("zipCode"))));
        }
        out2_3_4.close();

        table.close();
        connection.close();
        System.out.println("Processing complete. Files have been generated in the current directory.");
    }
}