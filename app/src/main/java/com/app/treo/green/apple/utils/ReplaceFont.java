package com.app.treo.green.apple.utils;

import android.content.Context;
import android.graphics.Typeface;

import java.lang.reflect.Field;

public class ReplaceFont {
    public static void replaceDefaultFont(Context context, String nameOfFontBeingReplaced, String nameOfFontInAsset) {
        Typeface customFontTypeface = Typeface.createFromAsset(context.getAssets(), nameOfFontInAsset);
        replaceFont(nameOfFontBeingReplaced, customFontTypeface);
    }

    private static void replaceFont(String nameOfFontBeingReplaced, Typeface customFontTypeface) {
        try {
            Field myField = Typeface.class.getDeclaredField(nameOfFontBeingReplaced);
            myField.setAccessible(true);
            myField.set(null, customFontTypeface);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
