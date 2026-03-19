package rpc;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@WebServlet("/history")
public class ItemHistory extends HttpServlet {

    private void writeJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        try (PrintWriter out = resp.getWriter()) {
            out.write(json);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String userId = request.getParameter("user_id");
        JSONArray array = new JSONArray();

        DBConnection conn = DBConnectionFactory.getConnection();
        Set<Item> items = conn.getFavoriteItems(userId);
        for (Item item : items) {
            JSONObject obj = item.toJSONObject();
            try {
                obj.append("favorite", true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.put(obj);
        }
        RpcHelper.writeJsonArray(response, array);

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            JSONObject input = RpcHelper.readJsonObject(request);
            String userId = input.getString("user_id");

            JSONArray array = input.getJSONArray("favorite");
            List<String> itemIds = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                itemIds.add(array.get(i).toString());
            }

            DBConnection conn = DBConnectionFactory.getConnection();
            conn.setFavoriteItems(userId, itemIds);
            conn.close();

            RpcHelper.writeJsonObject(response,
                    new JSONObject().put("result", "SUCCESS"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            JSONObject input = RpcHelper.readJsonObject(request);
            String userId = input.getString("user_id");

            JSONArray array = input.getJSONArray("favorite");
            List<String> itemIds = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                itemIds.add(array.get(i).toString());
            }

            DBConnection conn = DBConnectionFactory.getConnection();
            conn.unsetFavoriteItems(userId, itemIds);
            conn.close();

            RpcHelper.writeJsonObject(response,
                    new JSONObject().put("result", "SUCCESS"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
