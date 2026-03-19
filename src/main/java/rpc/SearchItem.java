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
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Collections;

@WebServlet(name = "SearchItem", urlPatterns = {"/search"})
public class SearchItem extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JSONArray array = new JSONArray();
        DBConnection connection = null;
        try {
            // 1) 读取请求参数
            final String userId = request.getParameter("user_id");
            final String latStr = request.getParameter("lat");
            final String lonStr = request.getParameter("lon");
            final String keyword = request.getParameter("term"); // 可能为空或空字符串

            // 基本必需参数
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            // 2) 使用同一个连接查询商品和用户收藏
            connection = DBConnectionFactory.getConnection();
            List<Item> items = connection.searchItems(lat, lon, keyword);

            // 如果 userId 缺失，使用空集合保持前端接口一致
            Set<String> favorite = (userId == null || userId.isEmpty())
                    ? Collections.emptySet()
                    : connection.getFavoriteItemIds(userId);

            // 3) 构建响应 JSON，添加前端需要的收藏标记
            for (Item item : items) {
                JSONObject obj = item.toJSONObject();
                obj.put("favorite", favorite.contains(item.getItemId()));
                array.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (Exception ignore) {} // 关闭连接，忽略异常
            }
        }

        // 4) 输出结果
        RpcHelper.writeJsonArray(response, array);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().println("Search POST invoked!");
    }
}
