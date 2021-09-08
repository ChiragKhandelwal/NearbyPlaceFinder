package com.example.searchnearby;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JsonParser {
    private HashMap<String,String> parseJson(JSONObject object) throws JSONException {
        HashMap<String,String> map=new HashMap<>();

        //get name from object

        String name=object.getString("name");
        String latitude=object.getJSONObject("geometry").getJSONObject("location").getString("lat");
        String longitude=object.getJSONObject("geometry").getJSONObject("location").getString("lng");

        map.put("name",name);
        map.put("lat",latitude);
        map.put("lng",longitude);

        return map;
    }
    private  List<HashMap<String,String>> parseJsonArray(JSONArray array) throws JSONException{
        List<HashMap<String,String>> list=new ArrayList<>();
        for(int i=0;i<array.length();i++){
            try {
                HashMap<String, String> map = parseJson((JSONObject) JSONArray.get(i));
                list.add(map);
            }
        catch (Exception e){
                e.printStackTrace();
        }
        }
        return list;
    }
    public List<HashMap<String,String>> parseResult(JSONObject object) throws JSONException {
        JSONArray array=null;
        array=object.getJSONArray("result");
        return parseJsonArray(array);
    }
}
