package com.example.creditcalculator;

import java.util.Calendar;

final class PaymentDateMath {
    private PaymentDateMath() {}
    static long startOfDay(long millis) {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }
    static boolean isSameDay(long a,long b){return startOfDay(a)==startOfDay(b);}
    static boolean isOnOrAfterDay(long value,long reference){return startOfDay(value)>=startOfDay(reference);}
    static boolean isBeforeDay(long value,long reference){return startOfDay(value)<startOfDay(reference);}
}
