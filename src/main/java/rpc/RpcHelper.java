package rpc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class RpcHelper {

    /** Write a JSONObject to HTTP response as application/json. */
    public static void writeJsonObject(HttpServletResponse resp, JSONObject obj) {
        writeJson(resp, obj == null ? new JSONObject() : obj);
    }

    /** Write a JSONArray to HTTP response as application/json. */
    public static void writeJsonArray(HttpServletResponse resp, JSONArray array) {
        if (array == null) array = new JSONArray();
        writeJson(resp, array);
    }

    /** Internal common writer (Object can be JSONObject/JSONArray). */
    private static void writeJson(HttpServletResponse resp, Object json) {
        resp.setContentType("application/json; charset=UTF-8");
        // 加上 CORS 便于你本地前端页面跨域调用（需要就留，不需要可删）
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.addHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");

        try {
            PrintWriter out = resp.getWriter();
            // org.json 的 JSONObject/JSONArray 都有合适的 toString()
            out.print(json.toString());
            // 不必显式 close()，容器会管理输出流；开发期更利于热更新
            out.flush();
        } catch (IOException e) {
            // 简单记录，真实项目建议统一日志
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Parses a JSONObject from http request.
    public static JSONObject readJsonObject(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = request.getReader();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

}