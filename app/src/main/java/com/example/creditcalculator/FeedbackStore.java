package com.example.creditcalculator;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Stores only textual feedback history. Attachment URIs and file bytes are never persisted here. */
final class FeedbackStore {
    private static final String KEY = "feedback_history_json";
    private static final int MAX_ENTRIES = 100;

    static final class Entry {
        final String type;
        final String title;
        final String text;
        final long createdAt;
        Entry(String type,String title,String text,long createdAt){this.type=type;this.title=title;this.text=text;this.createdAt=createdAt;}
    }

    private FeedbackStore() {}

    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(AppPreferences.PREFS_NAME,Context.MODE_PRIVATE);
    }

    static synchronized void add(Context c,String type,String title,String text){
        try{
            JSONArray old=new JSONArray(prefs(c).getString(KEY,"[]"));
            JSONArray out=new JSONArray();
            JSONObject item=new JSONObject();
            item.put("type",type==null?"":type);
            item.put("title",title==null?"":title.trim());
            item.put("text",text==null?"":text.trim());
            item.put("createdAt",System.currentTimeMillis());
            out.put(item);
            for(int i=0;i<old.length()&&out.length()<MAX_ENTRIES;i++)out.put(old.optJSONObject(i));
            prefs(c).edit().putString(KEY,out.toString()).apply();
        }catch(Exception ignored){}
    }

    static List<Entry> load(Context c){
        List<Entry> result=new ArrayList<>();
        try{
            JSONArray a=new JSONArray(prefs(c).getString(KEY,"[]"));
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);if(o==null)continue;
                result.add(new Entry(o.optString("type",""),o.optString("title",""),o.optString("text",""),o.optLong("createdAt",0)));
            }
        }catch(Exception ignored){}
        return result;
    }
}
