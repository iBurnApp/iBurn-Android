package com.gaiagps.iburn.adapters;

import java.util.Arrays;
import java.util.List;

/**
 * Created by dbro on 8/18/15.
 */
public class AZSectionalizer {

    public static String[] sections = new String[] { "!", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    public static List<String> sectionsList = Arrays.asList(sections);

    public static int getSectionIndexForName(String title) {
        int result = Math.max(0, sectionsList.indexOf(title.toUpperCase().substring(0, 1)));
        //Timber.d("Got section index %d for %s", result, title.toLowerCase().substring(0, 1));
        return result;
    }
}
