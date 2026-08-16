package com.example.creditcalculator;
import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;
public class PaymentDateMathTest {
    private static long at(int y,int m,int d,int h,int min){Calendar c=Calendar.getInstance();c.clear();c.set(y,m,d,h,min,0);return c.getTimeInMillis();}
    @Test public void sameCalendarDayIgnoresClockTime(){long due=at(2026,Calendar.AUGUST,16,9,0),action=at(2026,Calendar.AUGUST,16,15,33);assertTrue(PaymentDateMath.isSameDay(due,action));assertTrue(PaymentDateMath.isOnOrAfterDay(due,action));assertFalse(PaymentDateMath.isBeforeDay(due,action));}
    @Test public void adjacentDaysRemainOrdered(){long action=at(2026,Calendar.AUGUST,16,12,0);assertTrue(PaymentDateMath.isBeforeDay(at(2026,Calendar.AUGUST,15,23,59),action));assertTrue(PaymentDateMath.isOnOrAfterDay(at(2026,Calendar.AUGUST,17,0,1),action));}
}
