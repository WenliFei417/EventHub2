package rpc;

import algorithm.GeoRecommendation;
import entity.Item;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RecommendItem", urlPatterns = {"/recommendation"})
public class RecommendItem extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String userId = request.getParameter("user_id");
        double lat = Double.parseDouble(request.getParameter("lat"));
        double lon = Double.parseDouble(request.getParameter("lon"));

        GeoRecommendation recommendation = new GeoRecommendation();
        List<Item> items = recommendation.recommendItems(userId, lat, lon);

        JSONArray result = new JSONArray();
        try {
            for (Item item : items) {
                result.put(item.toJSONObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        RpcHelper.writeJsonArray(response, result);

    }
}