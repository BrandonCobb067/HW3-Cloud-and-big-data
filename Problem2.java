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

    private static SingleColumnValueFilter requiredFilter(byte[] family, byte[] column,
            CompareOperator op, ByteArrayComparable comparator) {
        SingleColumnValueFilter filter = new SingleColumnValueFilter(family, column, op, comparator);
        filter.setFilterIfMissing(true);
        return filter;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Table table = connection.getTable(TableName.valueOf("movielens_ratings"));

        // ----- Output File for 2.1 and 2.2 -----
        PrintWriter out2_1_2 = new PrintWriter(new FileWriter("part_2.1_and_2.2_output.txt"));

        out2_1_2.println("Part 2.1: Version Control");
        byte[] rowKey = Bytes.toBytes("1193_1");
        byte[] family = Bytes.toBytes("Rating");
        byte[] qualifier = Bytes.toBytes("score");
        try (Admin admin = connection.getAdmin()) {
            admin.modifyColumnFamily(table.getName(), ColumnFamilyDescriptorBuilder
                .newBuilder(admin.getDescriptor(table.getName()).getColumnFamily(family))
                .setMaxVersions(2).build());
        }
        Result original = table.get(new Get(rowKey).addColumn(family, qualifier));
        byte[] originalScore = original.getValue(family, qualifier);
        if (originalScore == null) throw new IllegalStateException("Load data before version test");
        long timestamp = Math.max(System.currentTimeMillis(), original.rawCells()[0].getTimestamp() + 1);
        String changedScore = Bytes.toString(originalScore).equals("4") ? "5" : "4";
        table.put(new Put(rowKey).addColumn(family, qualifier, timestamp, Bytes.toBytes(changedScore)));
        table.put(new Put(rowKey).addColumn(family, qualifier, timestamp + 1, originalScore));

        // Retrieve versions
        Get get = new Get(rowKey);
        get.readVersions(2);
        Result res = table.get(get);
        for (Cell cell : res.getColumnCells(Bytes.toBytes("Rating"), Bytes.toBytes("score"))) {
            out2_1_2.println("Version Timestamp: " + cell.getTimestamp() + " | Value: " + Bytes.toString(CellUtil.cloneValue(cell)));
        }

        out2_1_2.println("\nPart 2.2: User with most ratings");
        Scan scanCount = new Scan();
        scanCount.addColumn(Bytes.toBytes("User"), Bytes.toBytes("userId"));
        ResultScanner scannerCount = table.getScanner(scanCount);
        Map<String, Integer> userCounts = new HashMap<>();
        
        for (Result r : scannerCount) {
            String rk = Bytes.toString(r.getRow());
            String userId = Bytes.toString(r.getValue(Bytes.toBytes("User"), Bytes.toBytes("userId"))); // RowKey is MovieID_UserID
            userCounts.put(userId, userCounts.getOrDefault(userId, 0) + 1);
        }
        
        scannerCount.close();
        String topUser = "";
        int maxRatings = 0;
        for (Map.Entry<String, Integer> entry : userCounts.entrySet()) {
            if (entry.getValue() > maxRatings || (entry.getValue() == maxRatings &&
                (topUser.isEmpty() || Integer.parseInt(entry.getKey()) < Integer.parseInt(topUser)))) {
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
        scan1.setFilter(requiredFilter(Bytes.toBytes("Movie"), Bytes.toBytes("title"), CompareOperator.EQUAL, new RegexStringComparator("Star|Toy")));
        try (ResultScanner results = table.getScanner(scan1)) {
        for (Result r : results) {
            out2_3_1.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("title"))));
        }
        }
        out2_3_1.close();

        // 2. Movie belongs to both Comedy and Drama
        PrintWriter out2_3_2 = new PrintWriter(new FileWriter("part_2.3.2_output.txt"));
        out2_3_2.println("Movies with Comedy and Drama:");
        FilterList genreFilter = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        genreFilter.addFilter(requiredFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new SubstringComparator("Comedy")));
        genreFilter.addFilter(requiredFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new SubstringComparator("Drama")));
        Scan scan2 = new Scan();
        scan2.addColumn(Bytes.toBytes("Movie"), Bytes.toBytes("genres"));
        scan2.setFilter(genreFilter);
        try (ResultScanner results = table.getScanner(scan2)) {
        for (Result r : results) {
            out2_3_2.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("genres"))));
        }
        }
        out2_3_2.close();

        // 3. Movie belongs to more than two genres
        PrintWriter out2_3_3 = new PrintWriter(new FileWriter("part_2.3.3_output.txt"));
        out2_3_3.println("Movies with > 2 genres:");
        Scan scan3 = new Scan();
        scan3.addColumn(Bytes.toBytes("Movie"), Bytes.toBytes("genres"));
        scan3.setFilter(requiredFilter(Bytes.toBytes("Movie"), Bytes.toBytes("genres"), CompareOperator.EQUAL, new RegexStringComparator(".*\\|.*\\|.*")));
        try (ResultScanner results = table.getScanner(scan3)) {
        for (Result r : results) {
             out2_3_3.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("Movie"), Bytes.toBytes("genres"))));
        }
        }
        out2_3_3.close();

        // 4. User ZIP code begins with 9
        PrintWriter out2_3_4 = new PrintWriter(new FileWriter("part_2.3.4_output.txt"));
        out2_3_4.println("ZIP starts with 9:");
        Scan scan4 = new Scan();
        scan4.addColumn(Bytes.toBytes("User"), Bytes.toBytes("zipCode"));
        scan4.setFilter(requiredFilter(Bytes.toBytes("User"), Bytes.toBytes("zipCode"), CompareOperator.EQUAL, new BinaryPrefixComparator(Bytes.toBytes("9"))));
        try (ResultScanner results = table.getScanner(scan4)) {
        for (Result r : results) {
             out2_3_4.println(Bytes.toString(r.getRow()) + " : " + Bytes.toString(r.getValue(Bytes.toBytes("User"), Bytes.toBytes("zipCode"))));
        }
        }
        out2_3_4.close();

        table.close();
        connection.close();
        System.out.println("Processing complete. Files have been generated in the current directory.");
    }
}
