package com.ecdkey.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ListViewActivity extends AppCompatActivity {
    // Array of strings for ListView Title
    private Map<Integer, ArrayList<String>> weatherDesc = new LinkedHashMap<>();
    private Map<Integer, ArrayList<Double>> weatherTemp = new LinkedHashMap<>();
    private Map<Integer, ArrayList<String>> weatherCity = new LinkedHashMap<>();
    private Map<Integer, ArrayList<String>> weatherIcon = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_layout);

        Bundle bundle = this.getIntent().getExtras();

        if(bundle != null) {
            weatherDesc = (Map<Integer, ArrayList<String>>) bundle.getSerializable("weatherDesc");
            weatherTemp = (Map<Integer, ArrayList<Double>>) bundle.getSerializable("weatherTemp");
            weatherCity = (Map<Integer, ArrayList<String>>) bundle.getSerializable("weatherCity");
            weatherIcon = (Map<Integer, ArrayList<String>>) bundle.getSerializable("weatherIcon");
        }

        List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();

        for (int i = 0; i < 1; i++) {
            for(int z = 0; z < weatherIcon.get(i).size(); z++)
            {
                HashMap<String, String> hm = new HashMap<String, String>();
                hm.put("listview_title", weatherTemp.get(i).get(z) + "°F - " + weatherCity.get(i).get(z));
                hm.put("listview_discription", weatherDesc.get(i).get(z));
                if(weatherIcon.get(i).get(z).equals("01d") || weatherIcon.get(i).get(z).equals("01n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.clear_sky));
                }
                else if(weatherIcon.get(i).get(z).equals("02d") || weatherIcon.get(i).get(z).equals("02n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.few_clouds));
                }
                else if(weatherIcon.get(i).get(z).equals("03d") || weatherIcon.get(i).get(z).equals("03n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.scattered_clouds));
                }
                else if(weatherIcon.get(i).get(z).equals("04d") || weatherIcon.get(i).get(z).equals("04n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.broken_clouds));
                }
                else if(weatherIcon.get(i).get(z).equals("09d") || weatherIcon.get(i).get(z).equals("09n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.shower_rain));
                }
                else if(weatherIcon.get(i).get(z).equals("10d") || weatherIcon.get(i).get(z).equals("10n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.shower_rain));
                }
                else if(weatherIcon.get(i).get(z).equals("11d") || weatherIcon.get(i).get(z).equals("11n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.thunderstorm));
                }
                else if(weatherIcon.get(i).get(z).equals("13d") || weatherIcon.get(i).get(z).equals("13n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.snow));
                }
                else if(weatherIcon.get(i).get(z).equals("50d") || weatherIcon.get(i).get(z).equals("50n"))
                {
                    hm.put("listview_image", Integer.toString(R.drawable.windy));
                }
                aList.add(hm);
            }
        }

        String[] from = {"listview_image", "listview_title", "listview_discription"};
        int[] to = {R.id.listview_image, R.id.listview_item_title, R.id.listview_item_short_description};

        SimpleAdapter simpleAdapter = new SimpleAdapter(getBaseContext(), aList, R.layout.list_item, from, to);
        ListView androidListView = findViewById(R.id.list_view);
        androidListView.setAdapter(simpleAdapter);
    }
}
