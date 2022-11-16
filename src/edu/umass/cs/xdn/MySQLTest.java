package edu.umass.cs.xdn;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.util.List;
import java.util.stream.IntStream;

public class MySQLTest {
    public static void main(String[] args) {
        String database = args[0];
        String query = args[1];

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/test?useSSL=false", "root", "");

            if (connect != null) {
                Statement statement = connect.createStatement();
                ResultSet rs = statement.executeQuery("SELECT * FROM test;");
                System.out.println(rs);
                String response = getJsonFromResultSet(rs);
                System.out.println(response);
                connect.close();
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static String getJsonFromResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData md = resultSet.getMetaData();
        int numCols = md.getColumnCount();
        List<String> colNames = IntStream.range(0, numCols)
                .mapToObj(i -> {
                    try {
                        return md.getColumnName(i + 1);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return "?";
                    }
                }).toList();

        JSONArray result = new JSONArray();
        while (resultSet.next()) {
            JSONObject row = new JSONObject();
            colNames.forEach(cn -> {
                try {
                    row.put(cn, resultSet.getObject(cn));
                } catch (JSONException | SQLException e) {
                    e.printStackTrace();
                }
            });
            result.put(row);
        }
        return result.toString();
    }
}
