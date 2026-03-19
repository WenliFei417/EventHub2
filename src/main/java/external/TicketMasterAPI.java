package external;

import entity.Item;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TicketMasterAPI {
    private static final String BASE_URL = "https://app.ticketmaster.com/discovery/v2/events.json";
    private static final String DEFAULT_KEYWORD = ""; // no restriction
    //private static final String API_KEY = System.getenv("TICKETMASTER_API_KEY"); // ← 用你自己的
    private static final String API_KEY =
            System.getenv("TICKETMASTER_API_KEY") != null ? System.getenv("TICKETMASTER_API_KEY")
                    : System.getProperty("TICKETMASTER_API_KEY");

    //  {
    //    "name": "Sample Event",
    //    "id": "event123",
    //    "url": "www.example.com",
    //    ...
    //    "_embedded": {
    //	    "venues": [
    //	        {
    //		        "address": {
    //		           "line1": "101 First St,",
    //		           "line2": "Suite 101",
    //		           "line3": "...",
    //		        },
    //		        "city": {
    //		        	"name": "San Francisco"
    //		        }
    //		        ...
    //	        },
    //	        ...
    //	    ]
    //    }
    //    ...
    //  }

    /**
     * Helper methods
     */
    private String getAddress(JSONObject event) throws JSONException {
        if (!event.isNull("_embedded")) {
            JSONObject embedded = event.getJSONObject("_embedded");

            if (!embedded.isNull("venues")) {
                JSONArray venues = embedded.getJSONArray("venues");

                 for (int i = 0; i < venues.length(); ++i) {
                    JSONObject venue = venues.getJSONObject(i);

                    StringBuilder sb = new StringBuilder();

                    if (!venue.isNull("address")) {
                        JSONObject address = venue.getJSONObject("address");

                        if (!address.isNull("line1")) {
                            sb.append(address.getString("line1"));
                        }
                        if (!address.isNull("line2")) {
                            if (sb.length() > 0) sb.append(" ");  // ← 改进
                            sb.append(address.getString("line2"));
                        }
                        if (!address.isNull("line3")) {
                            if (sb.length() > 0) sb.append(" ");  // ← 改进
                            sb.append(address.getString("line3"));
                        }
                    }

                    if (!venue.isNull("city")) {
                        JSONObject city = venue.getJSONObject("city");

                        if (!city.isNull("name")) {
                            if (sb.length() > 0) sb.append(", ");  // ← 城市前加逗号
                            sb.append(city.getString("name"));
                        }
                    }

                    String result = sb.toString().trim();
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        }

        return "";
    }


    // {"images": [{"url": "www.example.com/my_image.jpg"}, ...]}
    private String getImageUrl(JSONObject event) throws JSONException {
        if (!event.isNull("images")) {
            JSONArray images = event.getJSONArray("images");

            for (int i = 0; i < images.length(); ++i) {
                JSONObject image = images.getJSONObject(i);

                if (!image.isNull("url")) {
                    return image.getString("url");
                }
            }
        }
        return "";
    }

    // {"classifications" : [{"segment": {"name": "music"}}, ...]}
    private Set<String> getCategories(JSONObject event) throws JSONException {
        Set<String> categories = new HashSet<>();
        if (!event.isNull("classifications")) {
            JSONArray classifications = event.getJSONArray("classifications");
            for (int i = 0; i < classifications.length(); i++) {
                JSONObject classification = classifications.getJSONObject(i);
                if (!classification.isNull("segment")) {
                    JSONObject segment = classification.getJSONObject("segment");
                    if (!segment.isNull("name")) {
                        String name = segment.getString("name");
                        categories.add(name);
                    }
                }
            }
        }
        return categories;
    }

    // Convert JSONArray to a list of item objects.
    private List<Item> getItemList(JSONArray events) throws JSONException {
        List<Item> itemList = new ArrayList<>();

        for (int i = 0; i < events.length(); ++i) {
            JSONObject event = events.getJSONObject(i);

            Item.ItemBuilder builder = new Item.ItemBuilder();

            if (!event.isNull("name")) {
                builder.setName(event.getString("name"));
            }

            if (!event.isNull("id")) {
                builder.setItemId(event.getString("id"));
            }

            if (!event.isNull("url")) {
                builder.setUrl(event.getString("url"));
            }

            if (!event.isNull("rating")) {
                builder.setRating(event.getDouble("rating"));
            }

            if (!event.isNull("distance")) {
                builder.setDistance(event.getDouble("distance"));
            }

            builder.setCategories(getCategories(event));
            builder.setAddress(getAddress(event));
            builder.setImageUrl(getImageUrl(event));

            itemList.add(builder.build());
        }

        return itemList;
    }



    public List<Item> search(double lat, double lon, String keyword) {
        if (keyword == null) {
            keyword = DEFAULT_KEYWORD;
        }

        try {
            keyword = java.net.URLEncoder.encode(keyword, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Convert lat/lon to geo hash
        String geoHash = GeoHash.encodeGeohash(lat, lon, 8);

        // Make your url query part like: "apikey=12345&geoPoint=abcd&keyword=music&radius=50"
        String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s",
                API_KEY, geoHash, keyword, 50);

        HttpURLConnection connection = null;
        try {
            // Open a HTTP connection between your Java application and TicketMaster based on url
            connection = (HttpURLConnection) new URL(BASE_URL + "?" + query).openConnection();
            // Set request method to GET
            connection.setRequestMethod("GET");
            // Send a request to TicketMaster and get response, response code could be returned directly
            // response body is saved in InputStream of connection.
            int responseCode = connection.getResponseCode();

            String safeQuery = query.replaceFirst("(?i)(apikey=)[^&]+", "$1***");
            System.out.println("Sending 'GET' request to URL : " + BASE_URL + "?" + safeQuery);
           //System.out.println("\nSending 'GET' request to URL : " + BASE_URL + "?" + query);
            System.out.println("Response Code : " + responseCode);

            if (responseCode != 200) {  // ← 添加检查
                return new ArrayList();
            }

            // Now read response body to get events data
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            JSONObject obj = new JSONObject(response.toString());
            if (obj.isNull("_embedded")) {
                return new ArrayList();
            }
            JSONObject embedded = obj.getJSONObject("_embedded");
            JSONArray events = embedded.getJSONArray("events");

            return getItemList(events);

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList();
        }finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void queryAPI(double lat, double lon) {
        List<Item> itemList = search(lat, lon, null);
        try {
            for (Item item : itemList) {
                JSONObject jsonObject = item.toJSONObject();
                System.out.println(jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        System.out.println("TicketMaster API initialized successfully.");
        TicketMasterAPI tmApi = new TicketMasterAPI();
        // Mountain View, CA
        tmApi.queryAPI(37.38, -122.08);
        // London, UK
        // tmApi.queryAPI(51.503364, -0.12);
        // Houston, TX
        //tmApi.queryAPI(29.682684, -95.295410);

    }
}