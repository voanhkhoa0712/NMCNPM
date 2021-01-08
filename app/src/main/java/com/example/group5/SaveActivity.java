package com.example.group5;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SaveActivity extends AppCompatActivity {
    ImageView image;
    Bitmap bmp;
    String username, duration, pace, distance, caption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_activity);
        image = (ImageView)findViewById(R.id.display_img);

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        duration = intent.getStringExtra("duration");
        pace = intent.getStringExtra("pace");
        distance = intent.getStringExtra("distance");
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.image);
        image.setImageBitmap(bmp);
    }
    public void btn_add_onclick(View view) {
        RequestQueue queue = Volley.newRequestQueue(this);
        EditText caption_view = (EditText) findViewById(R.id.caption);
        caption = caption_view.getText().toString();
        String url ="http://10.0.2.2:8000/createactivity/";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            Log.d("tester", response);
                            String result = obj.getString("success");
                            JSONArray events_pass = obj.getJSONArray("milestone_pass");
                            if (events_pass.length() > 0){
                                ArrayList<String> passes = new ArrayList<>();
                                for(int i=0;i<events_pass.length();i++){
                                    passes.add(events_pass.getString(i));
                                }
                                Intent to_congratulation = new Intent(SaveActivity.this, congratulation.class);
                                Bundle extra = new Bundle();
                                extra.putSerializable("events_pass", passes);
                                to_congratulation.putExtra("bundle",extra);
                                startActivity(to_congratulation);
                            }
                            else {
                                //Intent to_home = new Intent(
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            protected Map<String, String> getParams()
                    throws AuthFailureError {
                String img_str = getStringImage(bmp);
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", username);
                params.put("duration", duration);
                params.put("pace", pace);
                params.put("distance", distance);
                params.put("caption", caption);
                params.put("image", img_str);
                return params;
            }
        };

        queue.add(stringRequest);
    }

    public String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }
}