package db.mysql;

import entity.Item;
import external.TicketMasterAPI;

import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MySQLConnection implements db.DBConnection {

    private Connection conn;

    public MySQLConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(
                MySQLDBUtil.URL,
                MySQLDBUtil.USERNAME,
                MySQLDBUtil.PASSWORD
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                conn = null;
            }
        }
    }

    @Override
    public void setFavoriteItems(String userId, List<String> itemIds) {
        if (conn == null) {
            return;
        }
        try {
            String sql = "INSERT INTO history (user_id, item_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (String itemId : itemIds) {
                stmt.setString(1, userId);
                stmt.setString(2, itemId);
                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsetFavoriteItems(String userId, List<String> itemIds) {
        if (conn == null) {
            return;
        }
        try {
            String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (String itemId : itemIds) {
                stmt.setString(1, userId);
                stmt.setString(2, itemId);
                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getFavoriteItemIds(String userId) {
        if (conn == null) {
            return new HashSet<>();
        }
        Set<String> favoriteItemIds = new HashSet<>();
        try {
            String sql = "SELECT item_id FROM history WHERE user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String itemId = rs.getString("item_id");
                favoriteItemIds.add(itemId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return favoriteItemIds;
    }

    @Override
    public Set<Item> getFavoriteItems(String userId) {
        if (conn == null) {
            return new HashSet<>();
        }
        Set<Item> favoriteItems = new HashSet<>();
        Set<String> itemIds = getFavoriteItemIds(userId);
        try {
            String sql = "SELECT * FROM items WHERE item_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (String itemId : itemIds) {
                stmt.setString(1, itemId);
                ResultSet rs = stmt.executeQuery();
                Item.ItemBuilder builder = new Item.ItemBuilder();
                while (rs.next()) {
                    builder.setItemId(rs.getString("item_id"));
                    builder.setName(rs.getString("name"));
                    builder.setAddress(rs.getString("address"));
                    builder.setImageUrl(rs.getString("image_url"));
                    builder.setUrl(rs.getString("url"));
                    builder.setCategories(getCategories(itemId));
                    builder.setDistance(rs.getDouble("distance"));
                    builder.setRating(rs.getDouble("rating"));
                    favoriteItems.add(builder.build());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return favoriteItems;
    }

    @Override
    public Set<String> getCategories(String itemId) {
        if (conn == null) {
            return null;
        }
        Set<String> categories = new HashSet<>();
        try {
            String sql = "SELECT category FROM categories WHERE item_id = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, itemId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return categories;
    }

    @Override
    public List<Item> searchItems(double lat, double lon, String term) {
        TicketMasterAPI tmAPI = new TicketMasterAPI();
        List<Item> items = tmAPI.search(lat, lon, term);
        for (Item item : items) {
            saveItem(item);
        }
        return items;
    }

    @Override
    public void saveItem(Item item) {
        if (conn == null) {
            return;
        }
        try {
            String sql = "INSERT INTO items VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, item.getItemId());
            stmt.setString(2, item.getName());
            stmt.setDouble(3, item.getRating());
            stmt.setString(4, item.getAddress());
            stmt.setString(5, item.getImageUrl());
            stmt.setString(6, item.getUrl());
            stmt.setDouble(7, item.getDistance());
            stmt.execute();

            sql = "INSERT INTO categories VALUES (?, ?) ON CONFLICT DO NOTHING";
            stmt = conn.prepareStatement(sql);
            for (String category : item.getCategories()) {
                stmt.setString(1, item.getItemId());
                stmt.setString(2, category);
                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFullname(String userId) {
        if (conn == null) {
            return null;
        }
        String name = "";
        try {
            String sql = "SELECT first_name, last_name FROM users WHERE user_id = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                name = String.join(" ", rs.getString("first_name"), rs.getString("last_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    @Override
    public boolean verifyLogin(String userId, String password) {
        if (conn == null) {
            return false;
        }
        try {
            String sql = "SELECT user_id FROM users WHERE user_id = ? AND password = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, password);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}