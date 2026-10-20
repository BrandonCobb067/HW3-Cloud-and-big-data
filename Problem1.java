import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

public class Problem1 {

  public static void main(String[] args) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    int invalidCount = 0;
    long total = 0;

    try (Connection connection = ConnectionFactory.createConnection(conf);
         Table table = connection.getTable(TableName.valueOf("movielens_ratings"))) {

      Scan scan = new Scan();
      scan.addColumn(Bytes.toBytes("Rating"), Bytes.toBytes("score"));
      scan.setCaching(500);
      scan.setCacheBlocks(false);

      System.out.println("RowKey\tRating");

      try (ResultScanner scanner = table.getScanner(scan)) {
        for (Result result : scanner) {
          total++;
          String score = Bytes.toString(result.getValue(
            Bytes.toBytes("Rating"), Bytes.toBytes("score")));

          try {
            int rating = Integer.parseInt(score);
            if (rating < 1 || rating > 5) {
              System.out.println(Bytes.toString(result.getRow()) + "\t" + rating);
              invalidCount++;
            }
          } catch (NumberFormatException e) {
            System.out.println(Bytes.toString(result.getRow()) + "\t" + score);
            invalidCount++;
          }
        }
      }
    }

    System.out.println("Total ratings checked: " + total);
    System.out.println("Invalid ratings: " + invalidCount);
  }
}
