package com.projects.asgrebennikov.repetitor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

/**
 * Created by as.grebennikov on 22.02.18.
 */

public class WordsArrayAdapter<T> extends ArrayAdapter<T> {

    public WordsArrayAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the current item from ListView
        View view = super.getView(position,convertView,parent);

        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setTextColor(Color.WHITE);
        textView.setGravity(Gravity.CENTER);

        if (textView.getBackground() == null)
        {
            Random rand = new Random();
            int randomIndex = rand.nextInt(((colors.length - 1) - 0) + 1) + 0;
            while ((randomIndex % 2) != (position % 2)) {
                randomIndex = rand.nextInt(((colors.length - 1) - 0) + 1) + 0;
            }

            int randomColor = colors[randomIndex];

            view.setBackgroundColor(randomColor);
        }

        return view;
    }

    private int[] colors = {
            Color.parseColor("#8CBF26"), // lime
            Color.parseColor("#A200FF"), // purple
            Color.parseColor("#FF0097"), // magenta
            Color.parseColor("#A05000"), // brown
            Color.parseColor("#E671B8"), // pink
            Color.parseColor("#F09609"), // orange
            Color.parseColor("#E51400"), // red
            Color.parseColor("#339933"), // green
    };
}
