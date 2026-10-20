import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;

import java.util.HashMap;
import java.util.Map;

public class Problem2 {

    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Table table = connection.getTable(TableName.valueOf("movielens_ratings"));

        System.out.println("Part 2.1: Version Control");
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
            System.out.println("Version Timestamp: " + cell.getTimestamp() + " | Value: " + Bytes.toString(CellUtil.cloneValue(cell)));
        }

        System.out.println("\nPart 2.2: User with most ratings");
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
        System.out.println("UserID\tNumber_of_Ratings");
        System.out.println(topUser + "\t" + maxRatings);

        System.out.println("\nPart 2.3: Filtering");
        
        // 1. Movie title contains Star or Toy
        System.out.println("Movies with Star or Toy:");
        Scan scan1 = new Scan();
        scan1.setFilter(new SingleColumnValueFilter(Bytes.toBytes("Movie"), Bytes.toBytes("title"), CompareOperator.EQUAL, new RegexStringComparator("Star|Toy")));
        for (Result r : table.getScanner(scan1)) {
            System.out.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("title"))));
            break; 
        }

        // 2. Movie belongs to both Comedy and Drama
        System.out.println("Movies with Comedy and Drama:");
        FilterList genreFilter = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        genreFilter.addFilter(new SingleColumnValueFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new SubstringComparator("Comedy")));
        genreFilter.addFilter(new SingleColumnValueFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new SubstringComparator("Drama")));
        Scan scan2 = new Scan();
        scan2.setFilter(genreFilter);
        for (Result r : table.getScanner(scan2)) {
            System.out.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("genres"))));
            break;
        }

        // 3. Movie belongs to more than two genres (Uses Regex to find at least two '|' separators)
        System.out.println("Movies with > 2 genres:");
        Scan scan3 = new Scan();
        scan3.setFilter(new SingleColumnValueFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new RegexStringComparator(".*\\|.*\\|.*")));
        for (Result r : table.getScanner(scan3)) {
             System.out.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("genres"))));
             break;
        }

        // 4. User ZIP code begins with 9
        System.out.println("ZIP starts with 9:");
        Scan scan4 = new Scan();
        scan4.setFilter(new SingleColumnValueFilter(Bytes.toBytes("User"), Bytes.toBytes("zipCode"), CompareOperator.EQUAL, new BinaryPrefixComparator(Bytes.toBytes("9"))));
        for (Result r : table.getScanner(scan4)) {
             System.out.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("User"), Bytes.toBytes("zipCode"))));
             break;
        }

        table.close();
        connection.close();
    }
}