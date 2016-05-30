package com.soomla.store.billing.amazon;

import com.soomla.Soomla;
import com.soomla.SoomlaUtils;

import java.util.Currency;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmazonIabUtils {

    /**
     * Gets currency code from market place code
     * For example, if marketPlace "US" return "USD"
     *
     * @param marketPlace The market place code.
     * @return Currency code corresponding to marketPlace.
     * If maketPlace is null, empty or not supported, return "NO_CODE"
     *
     */
    public static String getCurrencyCode(String marketPlace){
        if (marketPlace == null || marketPlace.isEmpty()){
            SoomlaUtils.LogDebug(TAG, "(getCurrencyCode) Market place string is null or empty.");
            return DEFAULT_CURRENCY_CODE;
        }

        try{
            Locale locale = new Locale("", marketPlace);
            return Currency.getInstance(locale).getCurrencyCode();
        }catch (IllegalArgumentException ex){
            SoomlaUtils.LogError(TAG, "locale's country: " + marketPlace + " is not a supported ISO 3166 country. ");
        }

        return DEFAULT_CURRENCY_CODE;
    }

    /**
     * Gets the price micros from price,
     * For example, if price is "€7.99", return is "7990000".
     *
     * @param price
     * @return Price micros amount for the given price string.
     * If price is null or empty, or price has an unsupported format, return 0
     */
    public static long getPriceAmountMicros(String price) {
        if (price == null || price.isEmpty()){
            SoomlaUtils.LogDebug(TAG, "(getPriceAmountMicros) Price string is null or empty.");
            return DEFAULT_PRICE_MICROS;
        }

        Pattern regex = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher regexMatcher = regex.matcher(price);

        if (!regexMatcher.find()){
            SoomlaUtils.LogDebug(TAG, "(getPriceAmountMicros) price: " + price + " has an unsupported format");
            return DEFAULT_PRICE_MICROS;
        }

        return (long)(Double.parseDouble(regexMatcher.group(0)) * MICROS_MULTIPLIER);
    }

    private static final long MICROS_MULTIPLIER = 1000000;
    private static final long DEFAULT_PRICE_MICROS = 0;
    private static final String DEFAULT_CURRENCY_CODE = "NO_CODE";
    private static final String TAG = "AmazonIabUtils";
}
