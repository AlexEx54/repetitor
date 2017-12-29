package com.projects.asgrebennikov.repetitor;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;


class MyRunnable implements Runnable {

    private static String yandexKey = "dict.1.1.20171219T092115Z.b4d251fe6793335a.ce9863aa4d1660707da362b3a1e2122fa7db659c";

    public void run() {
        try {
            URL url = new URL("https://dictionary.yandex.net/api/v1/dicservice.json/lookup?" +
                    "key=" + yandexKey + "&" +
                    "lang=ru-en" + "&" +
                    "flags=4" + "&" +
                    "text=перебежавший");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            in.read();

            Scanner s = new Scanner(in).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            result = "";

           /*
            1. Используется словарь если слово словарное (с флагом 4 - формы слова).
            2. Иначе, используется переводчик.
            3. family filter - отсекает матерные слова (не нужен)
             */

        } catch ( Exception e) {
            String what = e.getMessage();
            what = "";
        }
    }

}

public class SentenceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentence);

        ArrayList<String> listItems = new ArrayList<String>();

        ListView lv = (ListView) findViewById(R.id.listView2);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems) {

                // Color.BLUE
                // Color.RED
                // Color.MAGENTA
                // Color.GRAY
                // Color.BLACK
                // Color.GREEN

                int[] colors = {
                         Color.BLUE,
                         Color.RED,
                         Color.MAGENTA,
                         Color.GRAY,
                         Color.BLACK,
                         Color.GREEN
                };

                @Override
                public View getView(int position, View convertView, ViewGroup parent){
                    // Get the current item from ListView
                    View view = super.getView(position,convertView,parent);

                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    textView.setTextColor(Color.WHITE);

                    Random rand = new Random();
                    int randomIndex = rand.nextInt(((colors.length - 1) - 0) + 1) + 0;
                    while ((randomIndex % 2) != (position % 2)) {
                        randomIndex = rand.nextInt(((colors.length - 1) - 0) + 1) + 0;
                    }

                    int randomColor = colors[randomIndex];

                    view.setBackgroundColor(randomColor);

                    return view;
                }
            };


        lv.setAdapter(adapter);

        listItems.add("Clicked 1");
        listItems.add("Clicked 2");
        listItems.add("Clicked 3");
        listItems.add("Clicked 4");
        listItems.add("Clicked 5");
        listItems.add("Clicked 6");
        listItems.add("Clicked 7");
        listItems.add("Clicked 8");
        listItems.add("Clicked 9");
        listItems.add("Clicked 10");

        adapter.notifyDataSetChanged();

        MyRunnable myRunnable = new MyRunnable();
        Thread t = new Thread(myRunnable);
        t.start();
    }
}
